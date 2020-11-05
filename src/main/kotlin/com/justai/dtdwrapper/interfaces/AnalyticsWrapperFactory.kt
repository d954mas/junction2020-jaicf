package com.justai.dtdwrapper.interfaces

interface AnalyticsWrapperFactory {
    fun init(apiKey: String): AnalyticsWrapper
}