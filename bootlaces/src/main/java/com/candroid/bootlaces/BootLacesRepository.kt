package com.candroid.bootlaces

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.preference.PreferenceManager
/*
            (   (                ) (             (     (
            )\ ))\ )    *   ) ( /( )\ )     (    )\ )  )\ )
 (   (   ( (()/(()/(  ` )  /( )\()|()/((    )\  (()/( (()/(
 )\  )\  )\ /(_))(_))  ( )(_)|(_)\ /(_))\((((_)( /(_)) /(_))
((_)((_)((_|_))(_))   (_(_()) _((_|_))((_))\ _ )(_))_ (_))
| __\ \ / /|_ _| |    |_   _|| || | _ \ __(_)_\(_)   \/ __|
| _| \ V /  | || |__    | |  | __ |   / _| / _ \ | |) \__ \
|___| \_/  |___|____|   |_|  |_||_|_|_\___/_/ \_\|___/|___/
....................../´¯/)
....................,/¯../
.................../..../
............./´¯/'...'/´¯¯`·¸
........../'/.../..../......./¨¯\
........('(...´...´.... ¯~/'...')
.........\.................'...../
..........''...\.......... _.·´
............\..............(
..............\.............\...
*/
class BootLacesRepositoryImpl(ctx: Context): BootLacesRepository(ctx){
    override fun getPreferences(): SharedPreferences {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            val dpCtx = ctx.createDeviceProtectedStorageContext()
            dpCtx.moveSharedPreferencesFrom(ctx, PreferenceManager.getDefaultSharedPreferencesName(ctx))
            return PreferenceManager.getDefaultSharedPreferences(dpCtx)
        }
        return mPrefs
    }

    override fun putBootService(serviceName: String) = mPrefs.edit().putString(KEY_SERVICE_CLASS_NAME, serviceName).apply()

    override fun fetchBootService() = mPrefs.getString(KEY_SERVICE_CLASS_NAME, null)

    override fun putContent(content: String) = mPrefs.edit().putString(KEY_CONTENT, content).apply()

    override fun fetchContent() = mPrefs.getString(KEY_CONTENT, null)

    override fun putTitle(title: String) = mPrefs.edit().putString(KEY_TITLE, title).apply()

    override fun fetchTitle() = mPrefs.getString(KEY_TITLE, null)

    override fun putIcon(icon: Int) = mPrefs.edit().putInt(KEY_SMALL_ICON, icon).apply()

    override fun fetchIcon() = mPrefs.getInt(KEY_SMALL_ICON, -1)

    override fun putActivity(activityName: String) = mPrefs.edit().putString(KEY_ACTIVITY_NAME, activityName).apply()

    override fun fetchActivity() = mPrefs.getString(KEY_ACTIVITY_NAME, null)
}

sealed class BootLacesRepository(val ctx: Context){
    companion object Keys{
        val KEY_TITLE = "KEY_TITLE"
        val KEY_CONTENT = "KEY_CONTENT"
        val KEY_SMALL_ICON = "KEY_SMALL_ICON"
        val KEY_ACTIVITY_NAME = "KEY_ACTIVITY_NAME"
    }
    internal val mPrefs by lazy { PreferenceManager.getDefaultSharedPreferences(ctx) }

    internal abstract fun getPreferences(): SharedPreferences

    internal abstract fun putBootService(serviceName: String)

    internal abstract fun fetchBootService(): String?

    internal abstract fun putContent(content: String)

    internal abstract fun fetchContent(): String?

    internal abstract fun putTitle(title: String)

    internal abstract fun fetchTitle(): String?

    internal abstract fun putIcon(icon: Int)

    internal abstract fun fetchIcon(): Int?

    internal abstract fun putActivity(activityName: String)

    internal abstract fun fetchActivity(): String?
}