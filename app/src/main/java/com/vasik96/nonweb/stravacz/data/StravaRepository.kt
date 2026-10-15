package com.vasik96.nonweb.stravacz.data

import com.vasik96.nonweb.stravacz.api.StravaApi
import com.vasik96.nonweb.stravacz.api.StravaApiException
import com.vasik96.nonweb.stravacz.api.MealType

import java.time.LocalDate
import java.time.LocalDateTime

/** UI-friendly models; API models stay in StravaApi.kt. */
data class LoginCredentials(val username: String, val password: String, val canteen: String, val remember: Boolean)
data class MealOption(
    val id: String,
    val name: String,
    val variant: String,
    val typeKey: String,
    val price: Double? = null,
    val ordered: Boolean = false,
    val canOrder: Boolean = true,
    val canCancel: Boolean = true,
    val isSoup: Boolean = false,
    val restriction: String = ""
)
data class MenuDay(val date: LocalDate, val deadline: LocalDateTime?, val meals: List<MealOption>, val noSchool: Boolean = false)

class StravaRepository(private val api: StravaApi = StravaApi()) {
    val isLoggedIn: Boolean get() = api.isLoggedIn
    val balance: Double? get() = api.user?.balance
    val userName: String?
        get() = api.user?.fullName?.takeIf { it.isNotBlank() }
            ?: api.user?.username?.takeIf { it.isNotBlank() }

    suspend fun login(credentials: LoginCredentials) {
        api.login(credentials.username, credentials.password, credentials.canteen)
    }

    suspend fun fetchMenu(): List<MenuDay> = api.fetchMenu().map { day ->
        val meals = day.meals.map { meal ->
            val soup = meal.type == MealType.SOUP || meal.typeLabel.contains("polév", true) || meal.typeLabel.contains("polev", true)
            val typeKey = when (meal.type) {
                MealType.MAIN -> "Jídlo"
                MealType.SOUP -> "Polévka"
                MealType.UNKNOWN -> meal.typeLabel.ifBlank { "Jídlo" }
            }
            MealOption(
                id = meal.id.toString(), name = meal.name,
                variant = meal.variant.ifBlank { meal.typeLabel.ifBlank { "Jídlo" } },
                typeKey = typeKey, price = meal.price,
                ordered = meal.ordered, canOrder = meal.canOrder, canCancel = meal.canCancel,
                isSoup = soup,
                restriction = if (meal.ordered) meal.cancelRestriction else meal.orderRestriction
            )
        }
        MenuDay(day.date, day.meals.filterNot { it.type == MealType.SOUP }.mapNotNull { it.deadline }.minOrNull()
            ?: day.meals.mapNotNull { it.deadline }.minOrNull(), meals, day.noSchool)
    }

    /** Save the diff in the same order as the desktop client: cancel old selection, then order new one. */
    suspend fun saveOrders(original: Map<String, String?>, selected: Map<String, String?>, menu: List<MenuDay>) {
        val changes = selected.keys.union(original.keys).filter { selected[it] != original[it] }
        for (key in changes) {
            val oldId = original[key]
            val newId = selected[key]
            val dayKey = key.substringBefore('|')
            val typeKey = key.substringAfter('|', "")
            val currentDay = fetchMenu().firstOrNull { it.date.toString() == dayKey }
                ?: throw StravaApiException("Den $dayKey už není v jídelníčku.")
            val groupMeals = currentDay.meals.filter { !it.isSoup && it.typeKey == typeKey }
            if (oldId != null) {
                val old = groupMeals.firstOrNull { it.id == oldId }
                    ?: throw StravaApiException("Původní jídlo už není v nabídce. Obnov jídelníček.")
                if (old.ordered) api.cancelMeal(old.id.toInt())
            }
            if (newId != null) {
                val freshDay = fetchMenu().firstOrNull { it.date.toString() == dayKey }
                    ?: throw StravaApiException("Den $dayKey už není v jídelníčku.")
                val fresh = freshDay.meals.firstOrNull { !it.isSoup && it.typeKey == typeKey && it.id == newId }
                    ?: throw StravaApiException("Vybrané jídlo už není v nabídce. Obnov jídelníček.")
                if (!fresh.ordered) api.orderMeal(fresh.id.toInt())
            }
        }
    }

    suspend fun logout() = api.logout()
}
