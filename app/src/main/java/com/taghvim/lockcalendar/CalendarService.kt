package com.taghvim.lockcalendar

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.time.LocalDate

/**
 * سرویس پیش‌زمینه که نوتیفیکیشن دائمی تاریخ را نگه می‌دارد.
 * چون visibility نوتیفیکیشن PUBLIC است، متن آن روی صفحهٔ قفل هم دیده می‌شود.
 */
class CalendarService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var lastDate: LocalDate? = null

    private val tickRunnable = object : Runnable {
        override fun run() {
            val today = LocalDate.now()
            if (today != lastDate) {
                lastDate = today
                notifyToday()
            }
            handler.postDelayed(this, CHECK_INTERVAL_MS)
        }
    }

    private val systemReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            handler.post {
                handler.removeCallbacks(tickRunnable)
                lastDate = null
                tickRunnable.run()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
        ContextCompat.registerReceiver(
            this,
            systemReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_DATE_CHANGED)
                addAction(Intent.ACTION_TIME_CHANGED)
                addAction(Intent.ACTION_TIMEZONE_CHANGED)
                addAction(Intent.ACTION_LOCALE_CHANGED)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        startForeground(NOTIFICATION_ID, buildNotification())
        lastDate = LocalDate.now()
        handler.postDelayed(tickRunnable, CHECK_INTERVAL_MS)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lastDate = null
        handler.removeCallbacks(tickRunnable)
        tickRunnable.run()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        runCatching { unregisterReceiver(systemReceiver) }
        super.onDestroy()
    }

    private fun notifyToday() {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val today = LocalDate.now()
        val collapsed = RemoteViews(packageName, R.layout.notification_date).apply {
            setTextViewText(R.id.txt_jalali, DateLines.persianLine(today))
            setTextViewText(R.id.txt_gregorian, DateLines.gregorianLine(today))
        }
        val expanded = RemoteViews(packageName, R.layout.notification_date_big).apply {
            setTextViewText(R.id.txt_jalali, DateLines.persianLine(today))
            setTextViewText(R.id.txt_gregorian, DateLines.gregorianLine(today))
        }
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setCustomContentView(collapsed)
            .setCustomBigContentView(expanded)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.channel_description)
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "persistent_date"
        const val NOTIFICATION_ID = 1001
        private const val CHECK_INTERVAL_MS = 30_000L

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, CalendarService::class.java)
            )
        }
    }
}
