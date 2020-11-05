package com.justai.dtdwrapper.concurrency

data class DtdEntry(
        val userId: String,
        val eventName: String,
        val timestamp: Long,
        val level:Int,
        val data: Any
)