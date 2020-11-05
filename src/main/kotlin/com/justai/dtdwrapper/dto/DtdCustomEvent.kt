package com.justai.dtdwrapper.dto

class DtdCustomEvent(val name:String, val level:Int = 0) {
    val entries: MutableSet<DtdCustomEventBody> = mutableSetOf()
    fun addEvent(timestamp:Long, data:Map<*,*>) {
        val body = DtdCustomEventBody(timestamp, data)
        entries.add(body)
    }
}