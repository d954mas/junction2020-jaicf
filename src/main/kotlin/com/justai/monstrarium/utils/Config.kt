package com.justai.monstrarium.utils

import com.justai.jaicf.activators.connector.chatapi.chatApi
import com.justai.jaicf.context.BotContext
import com.justai.jaicf.model.scenario.Scenario
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import shared.Shared
import shared.base.NativeApi


class Config {
    companion object {
        val HTML_URL = "https://junction2020-prod.firebaseapp.com"
        val IAP_CREDENTIAL_FILE = "/noiaps.json"
        val INAPP_PACKAGE_NAME = "com.no.iaps"
        val JAICP_RU = "https://zb04.just-ai.com/chatadapter/chatapi/EnwlZoBg:010df2d13db46e164ea1e1f7e0aad7a1d1d4782e"
        val JAICP_EN = "https://zb04.just-ai.com/chatadapter/chatapi/EnwlZoBg:010df2d13db46e164ea1e1f7e0aad7a1d1d4782e"
        val DTD_API_KEY = System.getenv().get("DTD_API_KEY")
        val DTD_CAPACITY = System.getenv().get("DTD_CAPACITY")?.toInt()
        val SERVER_TAG = System.getenv().get("SERVER_TAG") ?: "DEV"
        val DTD_RETRIES_NUMBER = 15
        val isDev = System.getenv().get("isDev")?.toBoolean()
    }
}
