package com.justai.dtdwrapper.serializers

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.justai.dtdwrapper.dto.DevtodevUserBody
import java.lang.reflect.Type

class DevtodevUserBodySerializer : JsonSerializer<DevtodevUserBody> {
    override fun serialize(obj: DevtodevUserBody, type: Type, context: JsonSerializationContext): JsonElement {
        val jsonObj = JsonObject()
        jsonObj.addProperty("prevId", obj.prevId)
        jsonObj.addProperty("userId", obj.userId)
        jsonObj.addProperty("prevUsedId", obj.prevId)
        obj.events.forEach { (k, v) ->
            jsonObj.add(k, context.serialize(v))
        }
        return jsonObj
    }
}