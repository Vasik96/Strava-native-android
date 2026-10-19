package com.vasik96.nonweb.stravacz.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vasik96.nonweb.stravacz.MainActivity
import com.vasik96.nonweb.stravacz.R
import com.vasik96.nonweb.stravacz.data.CredentialStore
import com.vasik96.nonweb.stravacz.data.LoginCredentials
import com.vasik96.nonweb.stravacz.data.MenuDay
import com.vasik96.nonweb.stravacz.data.OrderStateStore
import com.vasik96.nonweb.stravacz.data.StravaRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class OrderCheckWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val csLocale = Locale.forLanguageTag("cs-CZ")

    override suspend fun doWork(): Result {
        val credentialStore = CredentialStore(context)
        val savedCredentials = credentialStore.load() ?: return Result.success()

        val repository = StravaRepository()
        try {
            repository.login(
                LoginCredentials(
                    username = savedCredentials.username,
                    password = savedCredentials.password,
                    canteen = savedCredentials.canteen,
                    remember = true
                )
            )

            val freshMenu = repository.fetchMenu()
            val orderStateStore = OrderStateStore(context)
            val previousState = orderStateStore.loadState()
            val currentState = selectionState(freshMenu)

            if (previousState.isNotEmpty()) {
                val externalChanges = mutableListOf<String>()
                val allKeys = previousState.keys.union(currentState.keys)

                allKeys.forEach { key ->
                    val previousMealId = previousState[key]
                    val currentMealId = currentState[key]
                    if (previousMealId != currentMealId) {
                        externalChanges.add(formatChangeDesc(key, currentMealId))
                    }
                }

                if (externalChanges.isNotEmpty()) {
                    sendNotification(
                        title = "Změna v jídelníčku!",
                        message = externalChanges.joinToString("\n")
                    )
                }
            }

            orderStateStore.saveState(currentState)
            return Result.success()
        } catch (_: Exception) {
            return Result.retry()
        }
    }

    private fun formatChangeDesc(key: String, currentMealId: String?): String {
        val dateStr = key.substringBefore('|')
        val formattedDate = try {
            LocalDate.parse(dateStr)
                .format(DateTimeFormatter.ofPattern("EEEE d. M.", csLocale))
                .replaceFirstChar { it.uppercase(csLocale) }
        } catch (_: Exception) {
            dateStr
        }

        return if (currentMealId == null) {
            "Zaznamenáno odhlášení oběda na $formattedDate (mimo aplikaci)"
        } else {
            "Zaznamenána změna oběda na $formattedDate (mimo aplikaci)"
        }
    }

    private fun selectionState(menu: List<MenuDay>): Map<String, String?> =
        buildMap {
            menu.forEach { day ->
                day.meals
                    .filterNot { it.isSoup }
                    .groupBy { it.typeKey }
                    .forEach { (type, meals) ->
                        put(
                            "${day.date}|$type",
                            meals.firstOrNull { it.ordered }?.id
                        )
                    }
            }
        }

    private fun sendNotification(title: String, message: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

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

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
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
}
