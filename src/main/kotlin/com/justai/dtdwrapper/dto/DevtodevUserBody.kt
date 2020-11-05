package com.justai.dtdwrapper.dto

data class DevtodevUserBody(
        val prevId: String = "",
        val userId: String = "",
        val prevUserId: String = "",
        val events: MutableMap<String, MutableList<Any>> = mutableMapOf()
) {
    fun putEvent(eventName: String, event: Any) {
        if (events.containsKey(eventName)) {
            events[eventName]?.add(event)
        } else {
            events[eventName] = mutableListOf(event)
        }
    }
}