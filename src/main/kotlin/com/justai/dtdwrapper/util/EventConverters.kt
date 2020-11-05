package com.justai.dtdwrapper.util

import kotlin.reflect.full.memberProperties

fun eventPojoToMap(event:Any):Map<*, *> {
    val map = mutableMapOf<Any,Any?>()
    event::class.memberProperties.forEach { map[it.name] = it.getter.call(event) }
    return map
}