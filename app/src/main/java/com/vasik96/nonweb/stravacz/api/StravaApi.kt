package com.vasik96.nonweb.stravacz.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.SocketTimeoutException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

/** Native Kotlin client for the unofficial Strava.cz JSON API.
 * API behavior is undocumented and may change; do not log credentials, SID, or raw responses.
 */
class StravaApi(
    private val baseUrl: String = "https://app.strava.cz",
    private val language: String = "EN",
    private val timeoutMs: Int = 30_000,
) {
    // Strava.cz may set a session cookie during login; keep it for subsequent API calls.
    private val sessionCookies = linkedMapOf<String, String>()
    private var canteenNumber: String? = null
    private var sid: String? = null
    private var s5url: String? = null

    var user: StravaUser? = null
        private set

    val isLoggedIn: Boolean get() = !sid.isNullOrBlank() && !s5url.isNullOrBlank()

    suspend fun login(username: String, password: String, canteen: String): StravaUser =
        withContext(Dispatchers.IO) {
            require(username.isNotBlank()) { "Zadej uživatelské jméno." }
            require(password.isNotBlank()) { "Zadej heslo." }
            require(canteen.isNotBlank()) { "Zadej číslo jídelny." }

            // Match the Python client: establish the website session before calling the JSON API.
            primeWebSession()

            val payload = JSONObject()
                .put("cislo", canteen.trim())
                .put("jmeno", username.trim())
                .put("heslo", password)
                .put("zustatPrihlasen", true)
                .put("environment", "W")
                .put("lang", language)

            val response = post("/api/login", payload, allowUnauthenticated = true)
            checkApiError(response)

            val newSid = response.optString("sid").takeIf { it.isNotBlank() }
                ?: throw StravaApiException("Přihlášení nevrátilo platnou session (SID).")
            val newS5Url = response.optString("s5url").takeIf { it.isNotBlank() }
                ?: throw StravaApiException("Přihlášení nevrátilo adresu služby jídelny (s5url).")
            val account = response.optJSONObject("uzivatel") ?: JSONObject()

            canteenNumber = canteen.trim()
            sid = newSid
            s5url = newS5Url
            val parsedUser = StravaUser(
                username = username.trim(),
                fullName = account.optString("jmeno").takeIf { it.isNotBlank() },
                email = account.optString("email").takeIf { it.isNotBlank() },
                balance = account.optString("konto").toMoneyOrNull(),
                currency = account.optString("mena").ifBlank { "Kč" },
                canteenName = account.optString("nazevJidelny").takeIf { it.isNotBlank() },
                canteenNumber = canteen.trim(),
            )
            user = parsedUser
            parsedUser
        }

    suspend fun fetchMenu(): List<ApiMenuDay> = withContext(Dispatchers.IO) {
        ensureLoggedIn()
        val payload = authenticatedPayload()
            .put("s5url", requireNotNull(s5url))
            .put("konto", 0)
            .put("podminka", "")
        val response = post("/api/objednavky", payload)
        checkApiError(response)
        parseMenu(response)
    }

    /** Orders a meal and commits it as one transaction, then verifies the saved state. */
    suspend fun orderMeal(mealId: Int): OrderResult = setMealsOrdered(listOf(mealId), ordered = true)

    suspend fun orderMeals(mealIds: List<Int>): OrderResult = setMealsOrdered(mealIds, ordered = true)

    suspend fun cancelMeal(mealId: Int): OrderResult = setMealsOrdered(listOf(mealId), ordered = false)

    suspend fun cancelMeals(mealIds: List<Int>): OrderResult = setMealsOrdered(mealIds, ordered = false)

    private suspend fun setMealsOrdered(ids: List<Int>, ordered: Boolean): OrderResult = withContext(Dispatchers.IO) {
        ensureLoggedIn()
        val distinctIds = ids.distinct()
        if (distinctIds.isEmpty()) return@withContext OrderResult(emptyList(), emptyList())

        // Always resolve IDs against a freshly fetched menu: `veta` is not permanent.
        val currentMenu = fetchMenu()
        val allMeals = currentMenu.flatMap { it.meals }.associateBy { it.id }
        val missing = distinctIds.filterNot { allMeals.containsKey(it) }
        if (missing.isNotEmpty()) throw StravaApiException("Jídlo už není v aktuálním jídelníčku: ${missing.joinToString()}. Obnov jídelníček.")

        val targetMeals = distinctIds.map { allMeals.getValue(it) }
        val unchanged = targetMeals.filter { it.ordered == ordered }.map { it.id }
        val toChange = targetMeals.filter { it.ordered != ordered }
        if (toChange.isEmpty()) return@withContext OrderResult(emptyList(), unchanged)

        // Fail before sending anything if the current menu says the action is not allowed.
        val blocked = toChange.firstOrNull { meal ->
            if (ordered) !meal.canOrder else !meal.canCancel
        }
        if (blocked != null) {
            val reason = if (ordered) blocked.orderRestriction else blocked.cancelRestriction
            throw StravaApiException("Nelze ${if (ordered) "objednat" else "odhlásit"} '${blocked.name}': ${reason.ifBlank { "jídelna změnu nepovoluje" }}")
        }

        try {
            for (meal in toChange) {
                val stagePayload = authenticatedPayload()
                    .put("url", requireNotNull(s5url))
                    .put("veta", meal.id.toString())
                    .put("pocet", if (ordered) "1" else "0")
                val stageResponse = post("/api/pridejJidloS5", stagePayload)
                checkApiError(stageResponse)
                stageResponse.optString("konto").toMoneyOrNull()?.let { user = user?.copy(balance = it) }
            }

            val savePayload = authenticatedPayload()
                .put("url", requireNotNull(s5url))
                .put("xml", JSONObject.NULL)
            val saveResponse = post("/api/saveOrders", savePayload)
            checkApiError(saveResponse)
            saveResponse.optString("konto").toMoneyOrNull()?.let { user = user?.copy(balance = it) }

            val verified = fetchMenu().flatMap { it.meals }.associateBy { it.id }
            val notSaved = toChange.filter { verified[it.id]?.ordered != ordered }
            if (notSaved.isNotEmpty()) {
                throw StravaApiException("Server změnu nepotvrdil pro: ${notSaved.joinToString { it.name }}.")
            }
            OrderResult(toChange.map { it.id }, unchanged)
        } catch (e: Exception) {
            // Discard provisional changes on failure, as the Python library does.
            runCatching { reloadAndDiscardStagedChanges() }
            throw if (e is StravaApiException) e else StravaApiException(message = "Objednávku se nepodařilo dokončit: ${e.message ?: "neznámá chyba"}", cause = e)
        }
    }

    private suspend fun reloadAndDiscardStagedChanges() {
        val payload = authenticatedPayload()
            .put("url", requireNotNull(s5url))
            .put("getText", true)
            .put("checkVersion", true)
            .put("resetTables", true)
            .put("frontendFunction", "refreshInformations")
        val response = post("/api/nactiVlastnostiPA", payload)
        checkApiError(response)
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        if (isLoggedIn) {
            runCatching {
                val payload = authenticatedPayload().put("url", requireNotNull(s5url))
                post("/api/logOut", payload)
            }
        }
        sid = null
        s5url = null
        canteenNumber = null
        synchronized(sessionCookies) { sessionCookies.clear() }
        user = null
    }

    private fun authenticatedPayload(): JSONObject {
        ensureLoggedIn()
        return JSONObject()
            .put("cislo", requireNotNull(canteenNumber))
            .put("sid", requireNotNull(sid))
            .put("lang", language)
            .put("ignoreCert", "false")
    }

    private fun ensureLoggedIn() {
        if (!isLoggedIn || canteenNumber.isNullOrBlank()) {
            throw StravaApiException("Nejsi přihlášený. Nejdřív se přihlas.")
        }
    }

    private suspend fun primeWebSession() = withContext(Dispatchers.IO) {
        val connection = (URL(baseUrl.trimEnd('/') + "/en/prihlasit-se?jidelna").openConnection() as HttpURLConnection)
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = timeoutMs
            connection.readTimeout = timeoutMs
            connection.instanceFollowRedirects = true
            setBrowserHeaders(connection, isPost = false)
            val status = connection.responseCode
            // Drain the response so the connection can close cleanly; the page body isn't needed.
            val stream = if (status >= 400) connection.errorStream else connection.inputStream
            stream?.use { it.copyTo(java.io.OutputStream.nullOutputStream()) }
            captureCookies(connection)
        } catch (_: Exception) {
            // Same behavior as Python client: priming is best-effort; login gives the real error.
        } finally {
            connection.disconnect()
        }
    }

    private fun setBrowserHeaders(connection: HttpURLConnection, isPost: Boolean) {
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36")
        connection.setRequestProperty("Accept", "*/*")
        connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9,cs;q=0.8")
        connection.setRequestProperty("Origin", baseUrl.trimEnd('/'))
        connection.setRequestProperty("Referer", baseUrl.trimEnd('/') + "/en/prihlasit-se?jidelna")
        connection.setRequestProperty("sec-fetch-dest", "empty")
        connection.setRequestProperty("sec-fetch-mode", "cors")
        connection.setRequestProperty("sec-fetch-site", "same-origin")
        if (isPost) connection.setRequestProperty("Content-Type", "text/plain;charset=UTF-8")
    }

    private fun captureCookies(connection: HttpURLConnection) {
        synchronized(sessionCookies) {
            connection.headerFields.entries
                .firstOrNull { it.key.equals("Set-Cookie", ignoreCase = true) }
                ?.value.orEmpty()
                .forEach { header ->
                    val pair = header.substringBefore(';').trim()
                    val separator = pair.indexOf('=')
                    if (separator > 0) sessionCookies[pair.substring(0, separator).trim()] = pair.substring(separator + 1).trim()
                }
        }
    }

    private suspend fun post(path: String, body: JSONObject, allowUnauthenticated: Boolean = false): JSONObject =
        withContext(Dispatchers.IO) {
            if (!allowUnauthenticated) ensureLoggedIn()
            val connection = (URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                doOutput = true
                setBrowserHeaders(this, isPost = true)
                synchronized(sessionCookies) {
                    if (sessionCookies.isNotEmpty()) {
                        setRequestProperty("Cookie", sessionCookies.entries.joinToString("; ") { "${it.key}=${it.value}" })
                    }
                }
            }
            try {
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                // HttpURLConnection does not persist cookies between independent requests.
                captureCookies(connection)
                val status = connection.responseCode
                val stream = if (status >= 400) connection.errorStream else connection.inputStream
                val text = stream?.use { input -> BufferedReader(InputStreamReader(input, Charsets.UTF_8)).readText() }.orEmpty()
                if (text.isBlank()) {
                    if (status !in 200..299) throw StravaApiException("Strava.cz vrátila HTTP $status.")
                    // Some endpoints may return a bare JSON literal (e.g. logout).
                    return@withContext JSONObject().put("_httpStatus", status)
                }
                val parsed = try {
                    when (text.trim()) {
                        "true" -> JSONObject().put("_bareJson", true)
                        "false" -> JSONObject().put("_bareJson", false)
                        else -> JSONObject(text)
                    }
                } catch (e: Exception) {
                    throw StravaApiException("Neplatná JSON odpověď z $path (HTTP $status).", null, e)
                }
                if (status !in 200..299 && !parsed.has("state")) {
                    throw StravaApiException("Strava.cz vrátila HTTP $status.")
                }
                parsed.put("_httpStatus", status)
                parsed
            } catch (e: SocketTimeoutException) {
                throw StravaApiException("Strava.cz neodpověděla v limitu ${timeoutMs / 1000} s při požadavku $path. Zkontroluj připojení a zkus to znovu.", null, e)
            } finally {
                connection.disconnect()
            }
        }

    private fun checkApiError(json: JSONObject) {
        val state = json.optString("state")
        if (state.equals("error", ignoreCase = true)) {
            val code = json.optInt("number", -1)
            val message = json.optString("message").ifBlank { "Strava.cz požadavek odmítla." }
            throw StravaApiException(message, code)
        }
        val status = json.optInt("_httpStatus", 200)
        if (status !in 200..299) throw StravaApiException("Strava.cz vrátila HTTP $status.")
    }

    private fun parseMenu(root: JSONObject): List<ApiMenuDay> {
        val dayObjects = mutableMapOf<String, MutableList<JSONObject>>()
        val keys = root.keys().asSequence()
            .filter { it.startsWith("table") }
            .sortedBy { it.removePrefix("table").toIntOrNull() ?: Int.MAX_VALUE }
        keys.forEach { key ->
            val rows = root.optJSONArray(key) ?: return@forEach
            for (i in 0 until rows.length()) {
                val row = rows.optJSONObject(i) ?: continue
                val dateRaw = row.optString("datum")
                val date = parseDate(dateRaw) ?: continue
                dayObjects.getOrPut(date.toString()) { mutableListOf() }.add(row)
            }
        }
        return dayObjects.entries.sortedBy { it.key }.map { (dateString, rows) ->
            val meals = rows.mapNotNull(::parseMeal)
            val dayCode = rows.firstOrNull()?.optJSONObject("omezeniObj")?.optString("den").orEmpty()
            ApiMenuDay(
                date = LocalDate.parse(dateString),
                meals = meals,
                noSchool = dayCode == "VP" || meals.any { it.name.equals("Nevaří se", true) || it.name.equals("Státní svátek", true) },
                autoOrdered = dayCode != "T",
                statusCode = dayCode,
            )
        }
    }

    private fun parseMeal(row: JSONObject): StravaMeal? {
        val id = row.optString("veta").toIntOrNull() ?: return null
        val name = row.optString("nazev").ifBlank { row.optString("druh_popis") }
        if (name.isBlank()) return null
        // Ignore unpublished slots where the canteen has not published a meal name yet.
        if (name == row.optString("druh_popis") && name.matches(Regex("(?i)(oběd|obed|jídlo|jidlo)\\s*\\d+"))) return null
        val restrictions = row.optJSONObject("omezeniObj") ?: JSONObject()
        val orderRestriction = restrictions.optString("obj")
        val cancelRestriction = restrictions.optString("zm")
        val typeCode = row.optString("druh")
        val type = when {
            typeCode.equals("PO", true) || typeCode.startsWith("P", true) -> MealType.SOUP
            typeCode.startsWith("O", true) -> MealType.MAIN
            else -> MealType.UNKNOWN
        }
        val allergens = mutableListOf<Allergen>()
        row.optJSONArray("alergeny")?.let { array ->
            for (i in 0 until array.length()) {
                val item = array.optJSONArray(i) ?: continue
                allergens += Allergen(item.optString(0), item.optString(1))
            }
        }
        return StravaMeal(
            id = id,
            date = parseDate(row.optString("datum")) ?: return null,
            type = type,
            typeLabel = row.optString("druh_popis"),
            variant = row.optString("druh_popis"),
            name = name,
            description = row.optString("delsiPopis").takeIf { it.isNotBlank() },
            price = row.optString("cena").toMoneyOrNull(),
            ordered = row.optInt("pocet", 0) > 0 || row.optString("pocet") == "1",
            canOrder = orderRestriction.isBlank(),
            canCancel = cancelRestriction.isBlank(),
            orderRestriction = restrictionText(orderRestriction),
            cancelRestriction = restrictionText(cancelRestriction),
            deadline = parseDateTime(row.optString("casKonec")),
            allergens = allergens,
        )
    }

    private fun restrictionText(code: String): String = when (code) {
        "" -> ""
        "C" -> "Uzávěrka objednávek již proběhla."
        "I" -> "Toto jídlo nelze objednat samostatně."
        "V" -> "V tento den se nevaří."
        else -> "Změna není povolena (kód $code)."
    }

    private fun parseDate(value: String): LocalDate? {
        val candidates = listOf("dd.MM.uuuu", "dd-MM.uuuu", "d.M.uuuu", "d-M-uuuu")
        for (pattern in candidates) try { return LocalDate.parse(value.trim(), DateTimeFormatter.ofPattern(pattern, Locale.ROOT)) } catch (_: DateTimeParseException) { }
        return null
    }

    private fun parseDateTime(value: String): LocalDateTime? {
        if (value.isBlank()) return null
        try { return LocalDateTime.parse(value) } catch (_: DateTimeParseException) { }
        val formats = listOf("dd.MM.uuuu HH:mm", "d.M.uuuu H:mm", "dd.MM.uuuu'T'HH:mm:ss")
        for (pattern in formats) try { return LocalDateTime.parse(value, DateTimeFormatter.ofPattern(pattern, Locale.ROOT)) } catch (_: DateTimeParseException) { }
        return null
    }
}

private fun String.toMoneyOrNull(): Double? = trim().replace(',', '.').toDoubleOrNull()

data class StravaUser(
    val username: String,
    val fullName: String? = null,
    val email: String? = null,
    val balance: Double? = null,
    val currency: String = "Kč",
    val canteenName: String? = null,
    val canteenNumber: String,
)

enum class MealType { SOUP, MAIN, UNKNOWN }

data class Allergen(val code: String, val name: String)

data class StravaMeal(
    val id: Int,
    val date: LocalDate,
    val type: MealType,
    val typeLabel: String,
    val variant: String,
    val name: String,
    val description: String? = null,
    val price: Double? = null,
    val ordered: Boolean,
    val canOrder: Boolean,
    val canCancel: Boolean,
    val orderRestriction: String = "",
    val cancelRestriction: String = "",
    val deadline: LocalDateTime? = null,
    val allergens: List<Allergen> = emptyList(),
)

data class ApiMenuDay(
    val date: LocalDate,
    val meals: List<StravaMeal>,
    val noSchool: Boolean,
    val autoOrdered: Boolean,
    val statusCode: String = "",
) {
    val ordered: Boolean get() = meals.any { it.ordered }
}

data class OrderResult(val changedIds: List<Int>, val unchangedIds: List<Int>)

class StravaApiException(
    override val message: String,
    val apiCode: Int? = null,
    cause: Throwable? = null,
) : Exception(message, cause)
