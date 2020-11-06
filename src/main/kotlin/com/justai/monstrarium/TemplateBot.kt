package com.justai.monstrarium

import com.justai.jaicf.BotEngine
import com.justai.jaicf.activator.catchall.CatchAllActivator
import com.justai.jaicf.activators.connector.chatapi.ChatApiActivator
import com.justai.jaicf.activators.connector.chatapi.ChatApiSettings
import com.justai.jaicf.activators.connector.chatapi.IntentCommandActivator
import com.justai.jaicf.context.manager.InMemoryBotContextManager
import com.justai.jaicf.context.manager.mongo.MongoBotContextManager
import com.justai.jaicf.mongo.MongoBotContextManagerMy
import com.justai.monstrarium.scenario.MainScenario
import com.justai.monstrarium.utils.Config
import com.mongodb.MongoClient
import com.mongodb.MongoClientURI
import kotlinx.serialization.ImplicitReflectionSerializer


private fun getDbUri(): String {
    return System.getenv("MONGODB_URI")!!
}

private val contextManager = "".let { url ->
    val uri = MongoClientURI(getDbUri())
    val client = MongoClient(uri)
    MongoBotContextManagerMy(client.getDatabase(uri.database!!).getCollection("contexts"))
}



@ImplicitReflectionSerializer
val templateBot = BotEngine(
    model = MainScenario.model,
    contextManager = contextManager,
    activators = arrayOf(
        IntentCommandActivator,
        ChatApiActivator.Factory(
            ChatApiSettings(
                Config.JAICP_EN,
                Config.JAICP_RU
            )
        ),
        CatchAllActivator
    )
)