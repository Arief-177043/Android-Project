package com.growwx.util

import android.util.Log

/**
 * Lightweight analytics logger.
 * Replace the Log.d calls with Firebase Analytics / Mixpanel in production.
 *
 * Usage:
 *   Analytics.log("login_success")
 *   Analytics.log("trade_executed", mapOf("symbol" to "TCS", "type" to "BUY"))
 */
object Analytics {
    private const val TAG = "GrowwX_Analytics"

    fun log(event: String, params: Map<String, Any> = emptyMap()) {
        // In production, replace with:
        // FirebaseAnalytics.getInstance(context).logEvent(event, bundleOf(*params.entries.map { it.key to it.value }.toTypedArray()))
        Log.d(TAG, "Event: $event | Params: $params")
    }
}

// ─── Price Alert Notification Service ────────────────────────────────────────

package com.growwx.data.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.growwx.MainActivity
import com.growwx.R
import com.growwx.data.local.PriceAlertDao
import com.growwx.data.repository.StockRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class PriceAlertService : Service() {

    @Inject lateinit var alertDao: PriceAlertDao
    @Inject lateinit var stockRepo: StockRepository

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val CHANNEL_ID = "price_alerts"
    private var notifId = 1000

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        scope.launch {
            val allSymbols = (StockRepository.DUMMY_STOCKS + StockRepository.DUMMY_CRYPTO).map { it.symbol }
            stockRepo.observeQuotes(allSymbols, intervalMs = 10_000L).collect { prices ->
                val activeAlerts = alertDao.getActiveAlerts()
                activeAlerts.forEach { alert ->
                    val currentPrice = prices[alert.symbol]?.price ?: return@forEach
                    val triggered = when (alert.condition) {
                        "ABOVE" -> currentPrice >= alert.targetPrice
                        "BELOW" -> currentPrice <= alert.targetPrice
                        else -> false
                    }
                    if (triggered) {
                        alertDao.markTriggered(alert.id)
                        sendNotification(
                            title = "🔔 Price Alert: ${alert.symbol}",
                            body = "${alert.symbol} is now ₹${String.format("%,.2f", currentPrice)} (target: ${alert.condition.lowercase()} ₹${String.format("%,.2f", alert.targetPrice)})"
                        )
                    }
                }
            }
        }
        return START_STICKY
    }

    private fun sendNotification(title: String, body: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()

        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(notifId++, notification)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Price Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "GrowwX price alert notifications" }
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
