/*Copyright 2019 Chris Basinger

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.*/
package com.candroid.bootlaces

import android.app.AlarmManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleService
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.DefineComponent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped
import dagger.hilt.migration.AliasOf
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import javax.inject.Scope
import javax.inject.Singleton

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
/**
 * @author Chris Basinger
 * @email evilthreads669966@gmail.com
 * @date 10/31/20
 *
 **/
@Scope
@AliasOf(ServiceScoped::class)
annotation class ForegroundScope

@Module
@InstallIn(ApplicationComponent::class)
object BroadcastReceiverModule {
    @Singleton
    @Provides fun provideWorkDao(@ApplicationContext ctx: Context): WorkDao = Room.databaseBuilder(ctx, WorkDatabase::class.java, "worker_database").build().workerDao()
}

@FlowPreview
@InternalCoroutinesApi
@ForegroundScope
@EntryPoint
@InstallIn(ForegroundComponent::class)
interface ForegroundEntryPoint{
    @ForegroundScope
    fun getActivator(): ForegroundActivator
}

@InstallIn(ServiceComponent::class)
@Module
object ForegroundModule{
    @ForegroundScope
    @Provides fun notificationBuilder(@ApplicationContext ctx: Context) = NotificationCompat.Builder(ctx)
    @ForegroundScope
    @Provides fun provideScope() = CoroutineScope(Dispatchers.Default + SupervisorJob())
    @ForegroundScope
    @Provides fun provideNotificationManager(@ApplicationContext ctx: Context) = NotificationManagerCompat.from(ctx)
    @ForegroundScope
    @Provides fun provideChannel() = Channel<Work>()
    @ForegroundScope
    @Provides fun provideAlarmManager(@ApplicationContext ctx: Context) = ctx.getSystemService(LifecycleService.ALARM_SERVICE) as AlarmManager
}

@InstallIn(ServiceComponent::class)
@Module
interface ForegroundBindings{
    @ForegroundScope
    @Binds fun bindAdapter(workAdapter: WorkAdapter): Adapter<Work,Worker>
}
/*@InstallIn(ServiceComponent::class)
@Module
interface ForegroundBindingModule{
    @ForegroundScope
    @Binds fun bindDatabase(database: WorkDatabase): RoomDatabase
}*/

@DefineComponent(parent = ServiceComponent::class)
interface ForegroundComponent{
    @DefineComponent.Builder
    interface Builder {
        fun build(): ForegroundComponent
    }
}