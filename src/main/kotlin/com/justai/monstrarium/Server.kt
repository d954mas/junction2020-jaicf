package com.justai.monstrarium

import com.google.rpc.BadRequest
import com.justai.googlev3tov2.ActionsFulfillmentV3toV2
import com.justai.jaicf.channel.googleactions.ActionsFulfillment
import com.justai.jaicf.channel.http.httpBotRouting
import com.justai.monstrarium.scenario.MainScenario
import com.justai.monstrarium.utils.Utils
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.serialization.ImplicitReflectionSerializer
import org.slf4j.LoggerFactory
import shared.Configs
import shared.Shared
import shared.project.prototypes.Regions

@ImplicitReflectionSerializer
fun main() {
    Shared.load()


    embeddedServer(Netty, System.getenv("PORT")?.toInt() ?: 8080) {
        routing {
            httpBotRouting("/google_actionsv3" to ActionsFulfillmentV3toV2.sdk(templateBot))
            httpBotRouting("/rest_api" to ActionsFulfillment.sdk(templateBot))
        }
    }.start(wait = true)


}