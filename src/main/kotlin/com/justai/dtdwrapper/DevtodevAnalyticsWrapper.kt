package com.justai.dtdwrapper

import com.google.gson.GsonBuilder
import com.justai.dtdwrapper.dto.DevtodevUserBody
import com.justai.dtdwrapper.dto.DtdCustomEvent
import com.justai.dtdwrapper.dto.DtdCustomEventBody
import com.justai.dtdwrapper.dto.DtdResponse
import com.justai.dtdwrapper.interfaces.AnalyticsWrapper
import com.justai.dtdwrapper.interfaces.AnalyticsWrapperFactory
import com.justai.dtdwrapper.interfaces.HttpResponseFacade
import com.justai.dtdwrapper.serializers.DevtodevCustomEventBodySerializer
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ByteArrayEntity
import org.apache.http.impl.client.HttpClients
import com.justai.dtdwrapper.serializers.DevtodevUserBodySerializer
import com.justai.dtdwrapper.util.Consts
import com.justai.dtdwrapper.util.compressGzip
import com.justai.dtdwrapper.util.eventPojoToMap

class DevtodevAnalyticsWrapper(private val apiKey: String) : AnalyticsWrapper {
    private val url = "https://api.devtodev.com/stat/v1/?api="
    private val eventsMap = mutableMapOf<String, DevtodevUserBody>()
    private val gson = GsonBuilder().registerTypeAdapter(DtdCustomEventBody::class.java,DevtodevCustomEventBodySerializer()).registerTypeAdapter(DevtodevUserBody::class.java, DevtodevUserBodySerializer()).setPrettyPrinting().disableHtmlEscaping().create()
    private val fullUrl: String
        get() = url + apiKey

    override fun sendEvent(userId: String, eventName: String, timestamp: Long, level:Int, data: Any) {
        if (!eventsMap.containsKey(userId)) {
            eventsMap[userId] = DevtodevUserBody()
        }
        if (Consts.eventSet.contains(eventName))
            eventsMap[userId]?.putEvent(eventName, data)
        else sendCustomEvent(userId, eventName, timestamp, level, data)
    }

    private fun sendCustomEvent(userId: String, eventName: String, timestamp: Long, level:Int, data: Any) {
        var event:Map<*, *> = if (data is Map<*,*>) data
        else eventPojoToMap(data)
        if (!eventsMap[userId]!!.events.containsKey("ce")) {
            eventsMap[userId]!!.events["ce"] = mutableListOf()
        }
        val eventList = eventsMap[userId]!!.events["ce"] as MutableList<DtdCustomEvent>
        val contains = eventList.filter { it.name == eventName }
        val dtdEvent: DtdCustomEvent
        if (contains.isEmpty()) {
            dtdEvent = DtdCustomEvent(eventName, level)
            eventList.add(dtdEvent)
        } else dtdEvent = contains[0]
        dtdEvent.addEvent(timestamp, event)
    }

    override fun flush(): HttpResponseFacade {
        //Blocking HTTP logic
        val client = HttpClients.createDefault()
        val httpPost = HttpPost(fullUrl)
        val json = gson.toJson(eventsMap)
        println("JSONNED: $json")
        val zippedEvents = compressGzip(json)
        httpPost.entity = ByteArrayEntity(zippedEvents)
        val response = client.execute(httpPost)
        val responseFacade = DtdResponse(response)
        response.use {
            val statusCode = it.statusLine.statusCode
            if (statusCode == 200) eventsMap.clear()
        }
        return responseFacade
    }


    companion object : AnalyticsWrapperFactory {
        @JvmStatic
        override fun init(apiKey: String): AnalyticsWrapper {
            return DevtodevAnalyticsWrapper(apiKey)
        }

    }
}