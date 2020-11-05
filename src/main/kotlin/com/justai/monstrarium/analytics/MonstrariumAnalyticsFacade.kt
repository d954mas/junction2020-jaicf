package com.justai.monstrarium.analytics

import com.justai.dtdwrapper.concurrency.DtdConsumerThread
import com.justai.monstrarium.utils.Config.Companion.DTD_API_KEY
import com.justai.monstrarium.utils.Config.Companion.DTD_CAPACITY
import com.justai.monstrarium.utils.Config.Companion.DTD_RETRIES_NUMBER
import java.util.*

class MonstrariumAnalyticsFacade {
    companion object {
        fun sendAnalytics(map: Map<String, Any>) {
            val events: List<Map<*, *>>
            val storage: Map<*, *>
            val profile: Map<*, *>
            val dtdId: String
            val response: Map<*, *> = map["response"] as Map<*, *>

            events = response["analyticEvents"] as List<Map<*, *>>
            //println("AnalyticEvents: $events")
            if (map["storage"] is Map<*, *>) {
                storage = map["storage"] as Map<*, *>
                if (storage["profile"] is Map<*, *>) {
                    profile = storage["profile"] as Map<*, *>
                    dtdId = profile["dtdId"] as String
                } else throw ProfileNotFoundException()
            } else throw ProfileNotFoundException()
            val thread = DtdConsumerThread.initOrGet(DTD_API_KEY!!, DTD_CAPACITY!!, DTD_RETRIES_NUMBER)
            events.map { it -> it.toMutableMap() }.forEach { it ->
                val tmp = it["eventName"] as String
                it.remove("eventName")
                if (!Consts.eventSet.contains(tmp)) {
                    val userLevel = (it["userLevel"] as Double).toInt()
                    it.remove("userLevel")
                    assert(it.size <= 20) { "Event must contain no more than 20 fields. Current number of fields=${it.size}" }
                    thread.sendEvent(userId = dtdId, eventName = tmp, timestamp = Date().time, level = userLevel, data = it)
                }
                else thread.sendEvent(userId = dtdId, eventName = tmp, data = it)
            }
        }
    }
}