package com.candroid.bootlaces

import javax.inject.Inject

class WorkAdapter @Inject constructor(): Adapter<Work,Worker> {
    override fun request(adaptee: Work): Worker = Class.forName(adaptee.job).newInstance() as Worker
}

interface Adapter<T,R>{
    fun request(adaptee: T): R
}