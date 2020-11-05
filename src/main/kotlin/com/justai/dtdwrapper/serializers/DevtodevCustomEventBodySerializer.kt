package com.justai.dtdwrapper.serializers

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.justai.dtdwrapper.dto.DevtodevUserBody
import com.justai.dtdwrapper.dto.DtdCustomEventBody
import java.lang.reflect.Type

class DevtodevCustomEventBodySerializer : JsonSerializer<DtdCustomEventBody> {
    override fun serialize(obj: DtdCustomEventBody, type: Type, context: JsonSerializationContext): JsonElement {
        val root = JsonObject()
        val leaf = JsonObject()
        leaf.add("string", context.serialize(obj.string))
        leaf.add("double", context.serialize(obj.double))
        val params = JsonObject()
        params.add("t1", leaf)
        root.add("p", params)
        root.addProperty("t1", obj.t1)

        return root
    }
}