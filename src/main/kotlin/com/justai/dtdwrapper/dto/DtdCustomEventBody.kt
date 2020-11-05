package com.justai.dtdwrapper.dto

import com.justai.dtdwrapper.util.toInt
import java.lang.IllegalArgumentException

class DtdCustomEventBody(val t1:Long, data:Map<*,*>) {
    val string:MutableMap<Any, String> = mutableMapOf()
    val double:MutableMap<Any, Number> = mutableMapOf()

    init {
        data.forEach { (k, v) ->
            when (v) {
                is Number -> double[k!!] = v
                is String -> string[k!!] = v
                is Boolean -> double[k!!] = v.toInt()
                else -> throw IllegalArgumentException("Field ${k.toString()} contains ${v.toString()}")
            }
        }
    }
}