package com.vasik96.nonweb.stravacz

import androidx.compose.foundation.clickable
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import java.net.URLEncoder
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.vasik96.nonweb.stravacz.data.OrderStateStore
import com.vasik96.nonweb.stravacz.worker.OrderCheckWorker
import java.util.concurrent.TimeUnit
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.NoMeals
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.vasik96.nonweb.stravacz.data.CredentialStore
import com.vasik96.nonweb.stravacz.data.LoginCredentials
import com.vasik96.nonweb.stravacz.data.MenuDay
import com.vasik96.nonweb.stravacz.data.StravaRepository
import com.vasik96.nonweb.stravacz.ui.theme.StravaTheme
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as lazyRowItems
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.snapshotFlow
import androidx.core.app.NotificationCompat
import androidx.work.OneTimeWorkRequestBuilder
import kotlinx.coroutines.flow.collect
import androidx.compose.material3.LinearWavyProgressIndicator


// Formattery a locale se vytvoří jen jednou (dřív se vytvářely při každé rekompozici = lagy).
private val CS: Locale = Locale.forLanguageTag("cs-CZ")
private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE d. MMMM yyyy", CS)
private val DEADLINE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("d. M. HH:mm", CS)

private fun formatPrice(value: Double): String = "%.2f".format(CS, value)

private enum class Screen { Loading, Login, Menu }

/** Výběr jídel jednoho dne. Díky @Immutable + equals může DayCard přeskočit rekompozici. */
@Immutable
private data class DaySelection(val values: Map<String, String?>)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StravaTheme {
                StravaApp()
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StravaApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repository = remember { StravaRepository() }
    val credentialStore = remember {
        CredentialStore(context.applicationContext)
    }
    val orderStateStore = remember {
        OrderStateStore(context.applicationContext)
    }
    val scope = rememberCoroutineScope()

    var loggedIn by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var canteen by remember { mutableStateOf("") }

    // Jméno uživatele získané z API
    var userName by remember { mutableStateOf<String?>(null) }

    var rememberLogin by remember { mutableStateOf(true) }
    var passwordVisible by remember { mutableStateOf(false) }

    var days by remember { mutableStateOf<List<MenuDay>>(emptyList()) }
    var balance by remember { mutableStateOf<Double?>(null) }

    var selected by remember {
        mutableStateOf<Map<String, String?>>(emptyMap())
    }
    var original by remember {
        mutableStateOf<Map<String, String?>>(emptyMap())
    }

    var error by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val dirty = selected != original

    // Stabilní lambda, aby se karty dnů zbytečně nerekomponovaly.
    val onSelectMeal = remember {
        { key: String, id: String? -> selected = selected + (key to id) }
    }

    fun dayKey(day: MenuDay, type: String) = "${day.date}|$type"

    fun selectionState(menu: List<MenuDay>): Map<String, String?> =
        buildMap {
            menu.forEach { day ->
                day.meals
                    .filterNot { it.isSoup }
                    .groupBy { it.typeKey }
                    .forEach { (type, meals) ->
                        put(
                            dayKey(day, type),
                            meals.firstOrNull { it.ordered }?.id
                        )
                    }
            }
        }

    fun formatChangeNotificationDesc(key: String, currentMealId: String?): String {
        val dateStr = key.substringBefore('|')
        val formattedDate = try {
            LocalDate.parse(dateStr)
                .format(DateTimeFormatter.ofPattern("EEEE d. M.", CS))
                .replaceFirstChar { it.uppercase(CS) }
        } catch (_: Exception) {
            dateStr
        }

        return if (currentMealId == null) {
            "Zaznamenáno odhlášení oběda na $formattedDate (mimo aplikaci)"
        } else {
            "Zaznamenána změna oběda na $formattedDate (mimo aplikaci)"
        }
    }

    fun sendExternalNotification(ctx: Context, title: String, message: String) {
        val notificationManager =
            ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = "strava_order_changes"
        val channel = NotificationChannel(
            channelId,
            "Změny objednávek Strava.cz",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Upozornění při změně obědu jídelnou nebo z jiného zařízení."
            enableVibration(true)
        }
        notificationManager.createNotificationChannel(channel)

        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            ctx,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(ctx, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(1001, notification)
    }

    fun refreshMenu() {
        scope.launch {
            loading = true
            error = null

            try {
                val result = repository.fetchMenu()
                days = result

                val state = selectionState(result)
                selected = state
                original = state

                val previousState = orderStateStore.loadState()
                if (previousState.isNotEmpty()) {
                    val externalChanges = mutableListOf<String>()
                    val allKeys = previousState.keys.union(state.keys)

                    allKeys.forEach { key ->
                        val previousMealId = previousState[key]
                        val currentMealId = state[key]
                        if (previousMealId != currentMealId) {
                            externalChanges.add(formatChangeNotificationDesc(key, currentMealId))
                        }
                    }

                    if (externalChanges.isNotEmpty()) {
                        val notifText = externalChanges.joinToString("\n")
                        message = "Změna v jídelníčku:\n$notifText"
                        sendExternalNotification(context, "Změna v jídelníčku!", notifText)
                    }
                }

                orderStateStore.saveState(state)

                balance = repository.balance

                // Aktualizace jména z API
                userName = repository.userName
            } catch (e: Exception) {
                error = e.message
                    ?: "Jídelníček se nepodařilo načíst."
            } finally {
                loading = false
            }
        }
    }

    fun doLogin() {
        scope.launch {
            loading = true
            error = null

            try {
                repository.login(
                    LoginCredentials(
                        username.trim(),
                        password,
                        canteen.trim(),
                        rememberLogin
                    )
                )

                // Načtení jména uživatele po přihlášení
                userName = repository.userName

                loggedIn = true

                if (rememberLogin) {
                    credentialStore.save(
                        username.trim(),
                        password,
                        canteen.trim()
                    )
                } else {
                    credentialStore.clear()
                }

                refreshMenu()
            } catch (e: Exception) {
                error = e.message ?: "Přihlášení selhalo."
                loading = false
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<OrderCheckWorker>(30, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            "OrderCheckWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )

        val immediateWorkRequest = OneTimeWorkRequestBuilder<OrderCheckWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context.applicationContext).enqueue(immediateWorkRequest)

        val saved = credentialStore.load()

        if (saved != null) {
            username = saved.username
            password = saved.password
            canteen = saved.canteen

            try {
                repository.login(
                    LoginCredentials(
                        saved.username,
                        saved.password,
                        saved.canteen,
                        true
                    )
                )

                // Načtení jména i při automatickém přihlášení
                userName = repository.userName

                loggedIn = true
                refreshMenu()
            } catch (e: Exception) {
                credentialStore.clear()
                error = e.message
                    ?: "Automatické přihlášení selhalo."
                loading = false
            }
        } else {
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Strava.cz",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (loggedIn) {
                                "Jídelníček a objednávky"
                            } else {
                                "Přihlášení do jídelny"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    Surface(
                        Modifier
                            .padding(start = 12.dp, end = 8.dp)
                            .size(42.dp),
                        shape = RoundedCornerShape(15.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Restaurant,
                                null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                },
                actions = {
                    if (loggedIn) {
                        IconButton(
                            onClick = { refreshMenu() },
                            enabled = !loading
                        ) {
                            Icon(Icons.Default.Refresh, "Obnovit")
                        }

                        IconButton(
                            onClick = { showLogoutDialog = true },
                            enabled = !loading
                        ) {
                            Icon(Icons.Default.Logout, "Odhlásit")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (loggedIn && dirty) {
                Surface(
                    tonalElevation = 3.dp,
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { selected = original },
                            enabled = !loading,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Zahodit")
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    loading = true
                                    error = null

                                    try {
                                        repository.saveOrders(
                                            original,
                                            selected,
                                            days
                                        )

                                        message = "Změny uloženy"

                                        val refreshed =
                                            repository.fetchMenu()

                                        days = refreshed

                                        val state =
                                            selectionState(refreshed)

                                        selected = state
                                        original = state
                                        orderStateStore.saveState(state)
                                        balance = repository.balance
                                        userName = repository.userName
                                    } catch (e: Exception) {
                                        error = e.message
                                            ?: "Změny se nepodařilo uložit."
                                    } finally {
                                        loading = false
                                    }
                                }
                            },
                            enabled = !loading,
                            modifier = Modifier.weight(1.4f)
                        ) {
                            Icon(Icons.Default.CheckCircle, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Uložit změny")
                        }
                    }
                }
            }
        }
    ) { padding ->
        val screen = when {
            loggedIn -> Screen.Menu
            loading -> Screen.Loading
            else -> Screen.Login
        }

        // Plynulý přechod mezi přihlášením, načítáním a jídelníčkem.
        AnimatedContent(
            targetState = screen,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            transitionSpec = {
                fadeIn(tween(durationMillis = 320, delayMillis = 90)) togetherWith
                        fadeOut(tween(durationMillis = 180))
            },
            label = "screen"
        ) { target ->
            when (target) {
                Screen.Loading -> LoadingScreen()

                Screen.Menu -> MenuScreen(
                    days = days,
                    selected = selected,
                    loading = loading,
                    balance = balance,
                    error = error,
                    message = message,
                    userName = userName,
                    onSelect = onSelectMeal,
                    onDismissMessage = { message = null },
                    onRetry = { refreshMenu() }
                )

                Screen.Login -> LoginScreen(
                    username,
                    { username = it },
                    password,
                    { password = it },
                    canteen,
                    { canteen = it },
                    rememberLogin,
                    { rememberLogin = it },
                    passwordVisible,
                    { passwordVisible = it },
                    loading,
                    error,
                    { doLogin() }
                )
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = { Icon(Icons.Default.Logout, null) },
            title = { Text("Odhlásit se?") },
            text = {
                Text("Opravdu se chcete odhlásit ze Strava.cz?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false

                        scope.launch {
                            loading = true
                            runCatching { repository.logout() }

                            credentialStore.clear()
                            loggedIn = false
                            userName = null
                            days = emptyList()
                            selected = emptyMap()
                            original = emptyMap()
                            balance = null
                            password = ""
                            loading = false
                        }
                    }
                ) {
                    Text("Odhlásit")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutDialog = false }
                ) {
                    Text("Zůstat")
                }
            }
        )
    }
}

@Composable
private fun LoadingScreen() {
    Box(Modifier.fillMaxSize()) {
        // Logo a text nahoře (hodnotu top = 56.dp si klidně uprav).
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-144).dp)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                Modifier.size(92.dp),
                shape = RoundedCornerShape(30.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Restaurant,
                        null,
                        Modifier.size(46.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(Modifier.height(22.dp))

            Text(
                "Dobré jídlo\nzačíná tady.",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        // Indikátor přesně uprostřed obrazovky. a dolu
        WavyCircularIndicator(
            Modifier
                .align(Alignment.Center)
                .offset(y = 128.dp)
                .size(48.dp),
            thickness = 7.dp,
        )
    }
}

/** Nekonečně animovaný vlnitý indikátor načítání. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun WavyCircularIndicator(
    modifier: Modifier = Modifier,
    thickness: Dp = 4.dp,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val px = with(LocalDensity.current) { thickness.toPx() }
    val stroke = Stroke(width = px, cap = StrokeCap.Round)
    CircularWavyProgressIndicator(
        modifier = modifier,
        color = color,
        trackColor = color.copy(alpha = 0.25f),
        stroke = stroke,
        trackStroke = stroke,
    )
}


@Composable
private fun LoginScreen(
    username: String,
    onUsername: (String) -> Unit,
    password: String,
    onPassword: (String) -> Unit,
    canteen: String,
    onCanteen: (String) -> Unit,
    remember: Boolean,
    onRemember: (Boolean) -> Unit,
    passwordVisible: Boolean,
    onPasswordVisible: (Boolean) -> Unit,
    loading: Boolean,
    error: String?,
    onLogin: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(28.dp))

        Surface(
            Modifier.size(100.dp),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Restaurant,
                    null,
                    Modifier.size(50.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            "Dobré jídlo\nzačíná tady.",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        if (!loading) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Přihlaste se ke své školní jídelně.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(24.dp))

        val textFieldColors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        )

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
        ) {
            Column(
                Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextField(
                    username,
                    onUsername,
                    Modifier.fillMaxWidth(),
                    label = { Text("Uživatelské jméno") },
                    leadingIcon = { Icon(Icons.Default.Person, null) },
                    singleLine = true,
                    colors = textFieldColors
                )

                TextField(
                    password,
                    onPassword,
                    Modifier.fillMaxWidth(),
                    label = { Text("Heslo") },
                    leadingIcon = { Icon(Icons.Default.Lock, null) },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        TextButton(
                            onClick = {
                                onPasswordVisible(!passwordVisible)
                            }
                        ) {
                            Text(
                                if (passwordVisible) "Skrýt"
                                else "Zobrazit"
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password
                    ),
                    colors = textFieldColors
                )

                TextField(
                    canteen,
                    onCanteen,
                    Modifier.fillMaxWidth(),
                    label = { Text("Číslo jídelny") },
                    leadingIcon = {
                        Icon(Icons.Default.Restaurant, null)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    colors = textFieldColors
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(remember, onRemember)

                    Column {
                        Text(
                            "Zapamatovat přihlášení",
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Údaje jsou šifrované v zařízení",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (error != null) {
                    ErrorCard(error)
                }

                Button(
                    onClick = onLogin,
                    enabled = !loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    if (loading) {
                        WavyCircularIndicator(
                            Modifier.size(24.dp),
                            thickness = 3.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Default.CloudSync, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Přihlásit se")
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MenuScreen(
    days: List<MenuDay>,
    selected: Map<String, String?>,
    loading: Boolean,
    balance: Double?,
    error: String?,
    message: String?,
    userName: String?,
    onSelect: (String, String?) -> Unit,
    onDismissMessage: () -> Unit,
    onRetry: () -> Unit
) {
    val snackbar = remember { SnackbarHostState() }

    val visibleDays = remember(days) {
        days.filterIndexed { index, day ->
            if (hasRealMeals(day)) {
                true
            } else {
                val isHoliday = day.meals.any { meal ->
                    isHolidayMeal(meal.name)
                }

                val previousDayHasRealMeals =
                    index > 0 && hasRealMeals(days[index - 1])

                isHoliday && previousDayHasRealMeals
            }
        }
    }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val currentWeekStart = remember {
        LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }

    val nextWeekFirstDayIndex = remember(visibleDays, currentWeekStart) {
        visibleDays.indexOfFirst { day ->
            val dayWeekStart = day.date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            dayWeekStart > currentWeekStart
        }
    }

    val hasNextWeek = nextWeekFirstDayIndex >= 0
    val extraHeaderCount = 1 + if (error != null) 1 else 0
    val nextWeekItemIndex = if (hasNextWeek) nextWeekFirstDayIndex + extraHeaderCount else Int.MAX_VALUE

    // Tlačítko "Tento týden" je aktivní, pokud jsme odskrolovaní dolů z úplného vrcholu
    val isTentoTydenEnabled by remember(listState) {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0 }
    }

    // Tlačítko "Další týden" je aktivní, pokud existuje další týden a ještě k němu nejsme odskrolovaní
    val isDalsiTydenEnabled by remember(listState, hasNextWeek, nextWeekItemIndex) {
        derivedStateOf { hasNextWeek && listState.firstVisibleItemIndex < nextWeekItemIndex }
    }

    LaunchedEffect(message) {
        if (message != null) {
            snackbar.showSnackbar(message)
            onDismissMessage()
        }
    }

    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("EEE d. M.", CS)
    }

    Scaffold(
        // Vnější Scaffold již řeší systémové insets.
        // Zabráníme jejich druhému přičtení uvnitř MenuScreen.
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(
            0, 0, 0, 0
        ),
        snackbarHost = {
            SnackbarHost(snackbar)
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
        ) {
            // Přepínání jídelníčků po týdnech.
            if (visibleDays.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            scope.launch { listState.animateScrollToItem(0) }
                        },
                        enabled = isTentoTydenEnabled,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Tento týden")
                    }

                    Button(
                        onClick = {
                            if (hasNextWeek) {
                                scope.launch { listState.animateScrollToItem(nextWeekItemIndex) }
                            }
                        },
                        enabled = isDalsiTydenEnabled,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Další týden")
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 4.dp,
                        bottom = 16.dp
                    ),
                    verticalArrangement =
                        Arrangement.spacedBy(14.dp)
                ) {
                    item(
                        key = "header",
                        contentType = "header"
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(28.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        userName
                                            ?.takeIf { it.isNotBlank() }
                                            ?: "Vítej!",
                                        style = MaterialTheme
                                            .typography.titleMedium,
                                        color = MaterialTheme
                                            .colorScheme.onPrimaryContainer
                                    )

                                    Text(
                                        "Jídelníček",
                                        style = MaterialTheme
                                            .typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme
                                            .colorScheme.onPrimaryContainer
                                    )

                                    Spacer(
                                        Modifier.height(8.dp)
                                    )

                                    Row(
                                        verticalAlignment =
                                            Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Savings,
                                            contentDescription = null
                                        )

                                        Spacer(
                                            Modifier.width(8.dp)
                                        )

                                        Text(
                                            balance?.let {
                                                "Zůstatek ${formatPrice(it)} Kč"
                                            } ?: "Zůstatek není dostupný"
                                        )
                                    }
                                }

                                Icon(
                                    Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                        }
                    }

                    if (error != null) {
                        item(
                            key = "error",
                            contentType = "error"
                        ) {
                            ErrorCard(error, onRetry)
                        }
                    }

                    if (days.isEmpty() && !loading) {
                        item(
                            key = "empty",
                            contentType = "empty"
                        ) {
                            Text(
                                "Jídelníček je prázdný.",
                                style = MaterialTheme
                                    .typography.titleLarge
                            )
                        }
                    }

                    items(
                        items = visibleDays,
                        key = { day -> day.date.toString() },
                        contentType = { "day" }
                    ) { day ->
                        val daySelection = remember(selected, day.date) {
                            val prefix = "${day.date}|"

                            DaySelection(
                                selected.filterKeys { key ->
                                    key.startsWith(prefix)
                                }
                            )
                        }

                        DayCard(
                            day = day,
                            selection = daySelection,
                            onSelect = onSelect
                        )
                    }
                }

                if (loading) {
                    val thickness = with(LocalDensity.current) { 8.dp.toPx() }
                    val stroke = Stroke(width = thickness, cap = StrokeCap.Round)

                    LinearWavyProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                            .align(Alignment.TopCenter),
                        stroke = stroke,
                        trackStroke = stroke,
                        amplitude = 0.7f,
                        wavelength = 40.dp
                    )
                }
            }
        }
    }
}


private fun isPlaceholderMeal(name: String): Boolean {
    val normalized = name.trim().lowercase(CS)

    return normalized.matches(
        Regex("""^(oběd|obed|polévka|polevka)\s*\d*$""")
    ) || normalized.isBlank()
}

private fun isHolidayMeal(name: String): Boolean {
    val normalized = name.lowercase(CS)

    return normalized.contains("nevaří se") ||
            normalized.contains("nevarí se") ||
            normalized.contains("nevaříme") ||
            normalized.contains("nevarime") ||
            normalized.contains("zavřeno") ||
            normalized.contains("zavreno")
}

private fun hasRealMeals(day: MenuDay): Boolean {
    return day.meals.any { meal ->
        !meal.isSoup &&
                !isHolidayMeal(meal.name) &&
                !isPlaceholderMeal(meal.name)
    }
}

@Composable
private fun DayCard(
    day: MenuDay,
    selection: DaySelection,
    onSelect: (String, String?) -> Unit
) {
    var imageQuery by remember { mutableStateOf<String?>(null) }



    val dateLabel = remember(day.date) {
        day.date.format(DATE_FORMAT).replaceFirstChar { it.uppercase(CS) }
    }

    // Odstraní prázdné názvy jídel a sekce bez jídel.
    val groups = remember(day) {
        day.meals
            .filterNot { it.isSoup }
            .filterNot { isPlaceholderMeal(it.name) }
            .filterNot { isHolidayMeal(it.name) }
            .groupBy { it.typeKey }
            .filterValues { it.isNotEmpty() }
    }

    val soups = remember(day) {
        day.meals.filter {
            it.isSoup &&
                    !isPlaceholderMeal(it.name) &&
                    !isHolidayMeal(it.name)
        }
    }


    // Pokud po filtrování nezůstalo žádné jídlo, karta se nezobrazí.
    if (groups.isEmpty() && soups.isEmpty()) return

    val hasLunch = groups.keys.any { type ->
        selection.values["${day.date}|$type"] != null
    }

    val deadline = day.deadline
    val deadlineText = remember(deadline) {
        deadline?.format(DEADLINE_FORMAT)
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Hlavička dne
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = dateLabel,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    if (deadline != null && deadlineText != null) {
                        val passed = deadline.isBefore(LocalDateTime.now())

                        Text(
                            text = (
                                    if (passed) "Uzávěrka uplynula · "
                                    else "Objednávat do "
                                    ) + deadlineText,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (passed) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                val selectedDayMeals = groups.mapNotNull { (type, meals) ->
                    val key = "${day.date}|$type"
                    if (selection.values[key] == null) null
                    else meals.firstOrNull { it.ordered }
                }
                // Ikona je aktivní jen tehdy, když lze zrušit všechny vybrané obědy dne.
                val canCancelDay = selectedDayMeals.isNotEmpty() &&
                        selectedDayMeals.all { orderedMeal ->
                            orderedMeal == null || orderedMeal.canCancel
                        }
                IconButton(
                    onClick = {
                        groups.forEach { (type, meals) ->
                            val orderedMeal = meals.firstOrNull { it.ordered }
                            val key = "${day.date}|$type"
                            if (selection.values[key] != null &&
                                (orderedMeal == null || orderedMeal.canCancel)
                            ) {
                                onSelect(key, null)
                            }
                        }
                    },
                    enabled = canCancelDay
                ) {
                    Icon(
                        Icons.Default.NoMeals,
                        contentDescription = "Zrušit všechny objednávky tohoto dne",
                        tint = if (canCancelDay) MaterialTheme.colorScheme.error
                        else Color.LightGray.copy(alpha = 0.35f)
                    )
                }
            }

            // Polévky
            soups.forEach { soup ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (hasLunch) {
                            Icons.Default.CheckCircle
                        } else {
                            Icons.Default.Close
                        },
                        contentDescription = null,
                        tint = if (hasLunch) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        }
                    )

                    Spacer(Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(soup.name)

                        Text(
                            text = if (hasLunch) {
                                "Polévka · součást objednaného obědu"
                            } else {
                                "Polévka · žádný oběd není objednán"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = { imageQuery = "Polévka ${soup.name}" }) {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = "Zobrazit obrázky polévky ${soup.name}"
                        )
                    }
                }
            }

            // Jednotlivé typy obědů
            groups.forEach { (type, meals) ->
                HorizontalDivider()

                Text(
                    text = type,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                val key = "${day.date}|$type"
                val current = selection.values[key]
                val ordered = meals.firstOrNull { it.ordered }
                val canChange = ordered == null || ordered.canCancel

                meals.forEach { meal ->
                    val enabled = canChange &&
                            (meal.id == ordered?.id || meal.canOrder)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = current == meal.id,
                            onClick = {
                                if (enabled) {
                                    onSelect(key, if (current == meal.id) null else meal.id)
                                }
                            },
                            enabled = enabled
                        )

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(enabled = enabled) {
                                    onSelect(key, if (current == meal.id) null else meal.id)
                                }
                                .padding(vertical = 6.dp)
                        ) {
                            Text(
                                text = meal.name,
                                style = MaterialTheme.typography.bodyLarge
                            )

                            Text(
                                text = buildString {
                                    append(meal.variant)

                                    meal.price?.let {
                                        append(" · ${formatPrice(it)} Kč")
                                    }

                                    if (!enabled) {
                                        append(
                                            " · ${
                                                meal.restriction.ifBlank {
                                                    "nelze změnit"
                                                }
                                            }"
                                        )
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = { imageQuery = meal.name }
                        ) {
                            Icon(
                                Icons.Default.Image,
                                contentDescription = "Zobrazit obrázky jídla ${meal.name}"
                            )
                        }

                    }
                }
            }
        }
    }

    // Dialog s výsledky Google Obrázků
    imageQuery?.let { mealName ->
        val query = URLEncoder.encode(mealName, "UTF-8")
        val imageUrl = "https://www.google.com/search?tbm=isch&q=$query"

        AlertDialog(
            onDismissRequest = {
                imageQuery = null
            },
            title = {
                Text(
                    text = "Obrázky: $mealName",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(480.dp),
                    factory = { context ->
                        val cookieManager = CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)
                        cookieManager.setCookie("https://www.google.com", "SOCS=CAESHAgBEhJib3FfaWRlbnRpdHlmcm9udGVuZHVpc2VydmVyXzIwMjMwMzA3LjA2X3AxGgJjcyACGgYIgI_qnwY; Domain=.google.com; Path=/; Secure")
                        cookieManager.setCookie("https://google.com", "SOCS=CAESHAgBEhJib3FfaWRlbnRpdHlmcm9udGVuZHVpc2VydmVyXzIwMjMwMzA3LjA2X3AxGgJjcyACGgYIgI_qnwY; Domain=.google.com; Path=/; Secure")
                        cookieManager.setCookie("https://www.google.cz", "SOCS=CAESHAgBEhJib3FfaWRlbnRpdHlmcm9udGVuZHVpc2VydmVyXzIwMjMwMzA3LjA2X3AxGgJjcyACGgYIgI_qnwY; Domain=.google.cz; Path=/; Secure")
                        cookieManager.flush()

                        WebView(context).apply {
                            cookieManager.setAcceptThirdPartyCookies(this, true)
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    view?.evaluateJavascript(
                                        """
                                        (function() {
                                            var style = document.createElement('style');
                                            style.innerHTML = 'div[role="dialog"], c-wiz[role="dialog"], form[action*="consent"], #lb, div[aria-modal="true"], .fc-consent-root { display: none !important; } html, body { overflow: auto !important; position: static !important; }';
                                            (document.head || document.documentElement).appendChild(style);
                                            
                                            var btns = document.querySelectorAll('button, form button, input[type="submit"]');
                                            for (var i = 0; i < btns.length; i++) {
                                                var txt = (btns[i].innerText || btns[i].value || '').toLowerCase();
                                                if (txt.includes('přijmout') || txt.includes('accept') || txt.includes('souhlas') || txt.includes('agree')) {
                                                    btns[i].click();
                                                    break;
                                                }
                                            }
                                        })();
                                        """.trimIndent(),
                                        null
                                    )
                                }
                            }
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            loadUrl(imageUrl)
                        }
                    },
                    update = { webView ->
                        if (webView.url != imageUrl) {
                            webView.loadUrl(imageUrl)
                        }
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        imageQuery = null
                    }
                ) {
                    Text("Zavřít")
                }
            }
        )
    }
}


@Composable
private fun ErrorCard(
    text: String,
    onRetry: (() -> Unit)? = null
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            if (onRetry != null) {
                Spacer(Modifier.height(8.dp))

                TextButton(onClick = onRetry) {
                    Text("Zkusit znovu")
                }
            }
        }
    }
}
