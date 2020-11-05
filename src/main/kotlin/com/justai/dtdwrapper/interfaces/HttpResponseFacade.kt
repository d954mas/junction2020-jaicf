package com.justai.dtdwrapper.interfaces

interface HttpResponseFacade {
    val statusCode:Int
    val statusReason: String
    val body: ByteArray
    fun getBodyAsString():String = String(body, Charsets.UTF_8)
}