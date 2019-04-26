package com.candroid.bootlaces

import android.content.Context
import android.content.Intent
import android.os.Build
import android.preference.PreferenceManager
import kotlin.reflect.KClass

class Laces{
    companion object{
        fun tie(context: Context, serviceName: String, notificationTitle: String = "candroid", notificationContent: String = "boot laces", notificationIcon: Int = -1){
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val serviceClassName = preferences.getString(BootReceiver.SERVICE_CLASS_NAME_KEY, "null")
            if(serviceClassName.equals("null")){
                val editor = preferences.edit()
                editor.putString(BootReceiver.SERVICE_CLASS_NAME_KEY, serviceName)
                editor.putString(BootService.KEY_NOTIFICATION_TITLE, notificationTitle)
                editor.putString(BootService.KEY_NOTIFICATION_CONTENT, notificationContent)
                editor.putInt(BootService.KEY_NOTIFICATION_ICON, notificationIcon)
                editor.apply()
            }
            if(!BootService.isRunning){
                val intent = Intent(context, Class.forName(serviceName))
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    context.startForegroundService(intent)
                else
                    context.startService(intent)
            }
        }
    }
}