package com.taghvim.lockcalendar

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * بعد از ری‌استارت گوشی یا آپدیت برنامه، اگر کاربر سرویس را فعال گذاشته باشد
 * دوباره اجرایش می‌کند.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(MainActivity.KEY_ENABLED, false)) {
            CalendarService.start(context)
        }
    }
}
