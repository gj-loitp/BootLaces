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
import android.app.Service
import android.content.*
import android.os.Build
import android.util.Log
import androidx.core.app.ServiceCompat
import dagger.hilt.EntryPoints
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Provider
import kotlin.coroutines.CoroutineContext
import kotlin.properties.Delegates

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
 * @date 10/09/20
 *
 * */
@ExperimentalCoroutinesApi
@FlowPreview
@AndroidEntryPoint
class WorkService: Service(), ComponentCallbacks2, CoroutineScope {
    @Inject internal lateinit var foregroundProvider: Provider<ForegroundComponent.Builder>
    @Inject internal lateinit var alarmMgr: AlarmManager
    @Inject internal lateinit var database: WorkDao
    @Inject internal lateinit var intentFactory: IntentFactory
    @Inject lateinit var workSchedulerFacade: WorkShedulerFacade
    @Inject lateinit var mutex: Mutex
    @Inject lateinit var workers: MutableCollection<Worker>
    private lateinit var foreground: ForegroundActivator

    override val coroutineContext: CoroutineContext = Dispatchers.Default + SupervisorJob()
    private var startId: Int? = null

    var workerCount: Int by Delegates.observable(0) { _, _, newValue ->
        if (newValue == 0)
            stopWorkService()
    }

    companion object{
        internal var state: ServiceState = ServiceState.STOPPED
        internal fun isStarted() = !state.equals(ServiceState.STOPPED)
    }

    internal fun stopWorkService(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            if(state == ServiceState.FOREGROUND)
                foreground.deactivate()
        if(startId != null)
            stopSelfResult(startId!!)
        else
            stopSelf()
    }

    override fun onCreate() {
        super.onCreate()
        state = ServiceState.BACKGROUND
        foreground = EntryPoints.get(foregroundProvider.get().build(),ForegroundEntryPoint::class.java).getForeground()
        foreground.activate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        this.launch { this.startAction(intent) }
        super.onStartCommand(intent, flags, startId)
        this.startId = startId
        return START_NOT_STICKY
    }

    private suspend fun CoroutineScope.startAction(intent: Intent?){
            val work: Work? = intent?.getParcelableExtra(Work.KEY_PARCEL)
            when (intent?.action ?: return) {
                Actions.ACTION_SCHEDULE_BEFORE_REBOOT.action -> workSchedulerFacade.scheduleBeforeReboot(database, work!!, this)
                Actions.ACTION_SCHEDULE_AFTER_REBOOT.action -> workSchedulerFacade.scheduleAfterReboot(database, this)
                Actions.ACTION_EXPIRED_WORK.action ->{
                    val worker = Worker.createFromWork(work!!)
                    processExpiredWork(worker, this)
                }
                else -> return
            }
        }

    override fun onDestroy() {
        Log.d("WorkService", "onDestroy()")
        this.coroutineContext.cancelChildren()
        this.cancel()
        startId = 0
        runBlocking {
            mutex.withLock {
                workers.clear()
                workerCount = 0
            }
        }
        workers.forEach { worker -> worker.unregisterReceiver(this) }
        if(state.equals(ServiceState.FOREGROUND))
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        state = ServiceState.STOPPED
        super.onDestroy()
    }

    /*this particullar fuunction is up next for my attention*/
    internal suspend fun processExpiredWork(worker: Worker, scope: CoroutineScope){
        if(workers.contains(worker)) return
        mutex.withLock {
            workers.add(worker)
            workerCount++
        }
        val intent = intentFactory.createWorkNotificationIntent(worker)
        if(worker.withNotification == true)
            NotificatonService.enqueue(this, intent)
        worker.registerReceiver(this)
        scope.launch { worker.doWork(this@WorkService) }.join()
        worker.unregisterReceiver(this)
        if(worker.withNotification == true)
            NotificatonService.enqueue(this, intent.apply { setAction(Actions.ACTION_FINISH.action) })
        mutex.withLock { workerCount-- }
    }

    override fun onBind(intent: Intent?) = null

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if(level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW)
            System.gc()
    }
}