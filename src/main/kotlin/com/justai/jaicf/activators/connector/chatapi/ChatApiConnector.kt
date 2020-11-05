package com.justai.jaicf.activators.connector.chatapi

import com.justai.jaicf.activator.caila.dto.CailaInferenceResults
import com.justai.jaicf.activator.caila.dto.CailaIntent
import com.justai.jaicf.activator.caila.dto.CailaSlot
import com.justai.jaicf.helpers.http.toUrl
import com.justai.jaicf.helpers.logging.WithLogger
import com.justai.monstrarium.utils.Utils
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.header

import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readText
import io.ktor.util.Hash
import kotlinx.coroutines.runBlocking

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.json.JsonObject

@Serializable
data class ChatApiContext(val name: String, val lifespan: Int, val parameter: JsonObject?)

@Serializable
data class ChatApiBody(
    val clientId: String,
    val query: String,
    val contexts: Map<String, ChatApiContext>
)

@Serializable
data class ChatApiBodyResponse(
    val token: String,
    val clientId: String,
    val questionId: String,
    val data: ChatApiBodyResponseData
)

@Serializable
data class ChatApiBodyResponseData(
    val answer: String,
    val replies: List<ChatApiBodyResponseDataReplies>
)

@Serializable
data class ChatApiBodyResponseDataReplies(
    val type: String,
    val body: JsonObject?
)

internal class ChatApiConnector(
    url: String, urlRu: String
) : WithLogger {


    private val simpleInferenceUrl = "$url".toUrl()
    private val simpleInferenceRUUrl = "$urlRu".toUrl()
    private val client = HttpClient(CIO) { expectSuccess = true }
    private val json = Json(JsonConfiguration.Stable.copy(strictMode = false, encodeDefaults = false))

    fun simpleInference(clientId: String, query: String, locale: String): ChatApiBodyResponse? {
        try {
            logger.warn("Try connect");
            return runBlocking { simpleInferenceAsync(clientId, query, locale) }
        } catch (ex: Exception) {
            logger.warn("Failed on cailapub activator processing", ex)
        }
        return null
    }

    private suspend fun simpleInferenceAsync(clientId: String, query: String, locale: String): ChatApiBodyResponse {
        val contexts: MutableMap<String, ChatApiContext> = HashMap();
        contexts.put("battle", ChatApiContext("battle", 1, null))
        var url = simpleInferenceUrl
        if(locale == "ru"){
            url = simpleInferenceRUUrl
        }
        val response = client.post<HttpResponse>(url) {
            header("Content-Type", "application/json")
            header("User-Agent", "PostmanRuntime/7.23.0")
            header("Cache-Control", "no cache")
            header("Accept-Encoding", "gzip, deflate, br")
            header("Connection", "keep-alive")
            body = json.toJson(
                ChatApiBody.serializer(),
                ChatApiBody(clientId = clientId, query = query, contexts = contexts)
            ).toString()
        }
        logger.info(response.toString())
        val responseStr = response.readText()
        logger.info(responseStr)
        val result = json.parse(ChatApiBodyResponse.serializer(), responseStr)
        return result
    }
}