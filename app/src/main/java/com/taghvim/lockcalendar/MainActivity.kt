package com.taghvim.lockcalendar

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.time.LocalDate

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var txtStatus: TextView

    private val notifPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            startServiceIfPossible()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        findViewById<TextView>(R.id.txt_today_jalali).text =
            DateLines.persianLine(LocalDate.now())
        findViewById<TextView>(R.id.txt_today_gregorian).text =
            DateLines.gregorianLine(LocalDate.now())
        txtStatus = findViewById(R.id.txt_status)

        findViewById<Button>(R.id.btn_activate).setOnClickListener {
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                startServiceIfPossible()
            }
        }

        findViewById<Button>(R.id.btn_battery).setOnClickListener {
            requestIgnoreBatteryOptimizations()
        }

        findViewById<Button>(R.id.btn_stop).setOnClickListener {
            prefs.edit().putBoolean(KEY_ENABLED, false).apply()
            stopService(Intent(this, CalendarService::class.java))
            txtStatus.postDelayed({ updateStatus() }, 400)
        }
    }

    override fun onResume() {
        super.onResume()
        // اگر کاربر قبلاً فعال کرده ولی پروسه کشته شده، دوباره بالا می‌آوریم
        if (prefs.getBoolean(KEY_ENABLED, false) && !isNotificationShowing()) {
            startServiceIfPossible()
        }
        updateStatus()
    }

    private fun startServiceIfPossible() {
        prefs.edit().putBoolean(KEY_ENABLED, true).apply()
        CalendarService.start(this)
        txtStatus.postDelayed({ updateStatus() }, 500)
    }

    private fun isNotificationShowing(): Boolean {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return nm.activeNotifications.any { it.id == CalendarService.NOTIFICATION_ID }
    }

    private fun updateStatus() {
        txtStatus.setText(
            if (isNotificationShowing()) R.string.status_active else R.string.status_inactive
        )
    }

    private fun requestIgnoreBatteryOptimizations() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (pm.isIgnoringBatteryOptimizations(packageName)) return
        try {
            startActivity(
                Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:$packageName")
                )
            )
        } catch (e: Exception) {
            runCatching {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
        }
    }

    companion object {
        const val PREFS = "taghvim_prefs"
        const val KEY_ENABLED = "service_enabled"
    }
}
