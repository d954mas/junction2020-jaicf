package com.justai.dtdwrapper.interfaces

import java.util.*

interface AnalyticsWrapper {
    fun sendEvent(userId: String, eventName: String, timestamp: Long = Date().time, level: Int = 0, data: Any)
    fun flush(): HttpResponseFacade
}