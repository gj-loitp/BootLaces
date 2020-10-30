package com.candroid.bootlaces

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.ServiceLifecycleDispatcher
import androidx.lifecycle.lifecycleScope
import dagger.hilt.EntryPoints
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.flow.collect
import java.util.*
import javax.inject.Inject
import javax.inject.Provider

/**
 * @author Chris Basinger
 * @email evilthreads669966@gmail.com
 * @date 10/09/20
 *
 * activates [Worker]
 * */
@ExperimentalCoroutinesApi
@InternalCoroutinesApi
@FlowPreview
@ForegroundScope
abstract class BackgroundWorkService: LifecycleService() {
    private val mDispatcher = ServiceLifecycleDispatcher(this)
    private var startId: Int = 0
    @Inject lateinit var provider: Provider<ForegroundComponent.Builder>
    @Inject lateinit var channel: Channel<Work>
    internal lateinit var foreground: ForegroundActivator
    val workers = Collections.synchronizedSet(mutableSetOf<Worker>())
    val receivers = mutableListOf<BroadcastReceiver>()

    init {
        lifecycle.addObserver(BootServiceState)
        lifecycleScope.launchWhenCreated { handleWorkers() }
    }

    override fun getLifecycle() = mDispatcher.lifecycle

    override fun onCreate() {
        mDispatcher.onServicePreSuperOnCreate()
        super.onCreate()
        foreground = EntryPoints.get(provider.get().build(),ForegroundEntryPoint::class.java).getActivator()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        mDispatcher.onServicePreSuperOnStart()
        super.onStartCommand(intent, flags, startId)
        this.startId = startId
        return START_STICKY
    }

    override fun onDestroy() {
        mDispatcher.onServicePreSuperOnDestroy()
        if(BootServiceState.isForeground())
            ServiceCompat.stopForeground(this,ServiceCompat.STOP_FOREGROUND_REMOVE)
        foreground.scope.also { it.coroutineContext.cancelChildren() }.cancel()
        receivers.forEach { unregisterReceiver(it) }
        lifecycle.removeObserver(BootServiceState)
        stopSelfResult(startId)
        super.onDestroy()
    }
    suspend fun handleWorkers(){

        foreground.scope.launch {
            ticker(5000,3000).consumeEach {
                if(isActive){
                    if(foreground.workerCount == 0 && foreground.lastCompletionTime != null){
                        val time = System.currentTimeMillis() - foreground.lastCompletionTime!!
                        if(time > 10000){
                            foreground.deactivate()
                        }
                    }
                }
            }
        }
        foreground.scope.launch {
            foreground.database.getAll().collect { work ->
                handleWork(this, work)
            }
        }
        foreground.scope.launch {
            channel.consumeEach {
                handleWork(this, it)
            }
        }
    }

    @InternalCoroutinesApi
    suspend fun handleWork(coroutineScope: CoroutineScope, work: Work){
        val worker = Class.forName(work.job).newInstance() as Worker
        if(workers.contains(worker))
            return
        workers.add(worker)
        if( worker.hasReceiver && worker.action != null){
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    worker.onReceive(context!!, intent!!)
                }
            }
            val filter = IntentFilter(worker.action)
            registerReceiver(receiver,filter)
        }
        if(!BootServiceState.isForeground())
            foreground.activate()
        coroutineScope.launch(Dispatchers.Default){
            foreground.notifyBackground(ForegroundActivator.ForegroundTypes.BACKGROUND_STARTED, work.id, worker.description)
            worker.doWork(this@BackgroundWorkService)
            foreground.notifyBackground(ForegroundActivator.ForegroundTypes.BACKGROUND_FINISHED, work.id, worker.description)
            workers.remove(worker)
        }
    }
}