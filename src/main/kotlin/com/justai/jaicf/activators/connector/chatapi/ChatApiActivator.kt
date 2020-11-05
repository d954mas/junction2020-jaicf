package com.justai.jaicf.activators.connector.chatapi

import com.justai.jaicf.activator.Activator
import com.justai.jaicf.activator.ActivatorFactory
import com.justai.jaicf.activator.caila.CailaIntentActivatorContext
import com.justai.jaicf.activator.caila.dto.CailaInferenceResults
import com.justai.jaicf.activator.intent.BaseIntentActivator
import com.justai.jaicf.activator.intent.IntentActivatorContext
import com.justai.jaicf.api.BotRequest
import com.justai.jaicf.api.hasIntent
import com.justai.jaicf.context.ActivatorContext
import com.justai.jaicf.context.BotContext

import com.justai.jaicf.model.scenario.ScenarioModel
import com.justai.monstrarium.scenario.MainScenario
import com.justai.monstrarium.utils.Utils
import com.justai.monstrarium.utils.Utils.Companion.chatApiToIntentName
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.content
import kotlinx.serialization.stringify
import org.slf4j.LoggerFactory;
import shared.Shared


@Suppress("ArrayInDataClass")
@Serializable
data class ChatApiNlu(
    val patterns: Array<ChatApiPattern>,
    val examples: Array<ChatApiExample>,
    val intents: Array<ChatApiIntent>
)

@Serializable
data class ChatApiPattern(
    val clazz: String,
    val score: Float
)

@Serializable
data class ChatApiExample(
    val clazz: String,
    val score: Float
)

@Serializable
data class ChatApiIntent(
    val debugInfo: ChatApiIntentDebugInfo,
    val score: Float
)

@Serializable
data class ChatApiIntentDebugInfo(
    val intent: ChatApiIntentDebugInfoIntent,
    val slots: Array<ChatApiIntentDebugInfoSlot>?
)


@Serializable
data class ChatApiIntentDebugInfoSlot(
    val name: String,
    val value: String
)

@Serializable
data class ChatApiIntentDebugInfoIntent(
    val path: String
)

data class ChatApiActivatorContext(val name: String, override val intent: String, val slots: Map<String, String>) :
    IntentActivatorContext(1f, intent)

val ActivatorContext.chatApi
    get() = this as? ChatApiActivatorContext


@ImplicitReflectionSerializer
class ChatApiActivator(
    model: ScenarioModel,
    settings: ChatApiSettings
) : BaseIntentActivator(model) {

    companion object {
        val LOGGER = LoggerFactory.getLogger(ChatApiActivator::class.java)
    }

    private val connector = ChatApiConnector(settings.url,settings.urlRU)
    private val json = Json(JsonConfiguration.Stable.copy(strictMode = false, encodeDefaults = false))


    override fun canHandle(request: BotRequest) = true

    override fun recogniseIntent(botContext: BotContext, request: BotRequest): IntentActivatorContext? {
        val results = connector.simpleInference(request.clientId, request.input,Utils.getLocale(botContext,request) ) ?: return null
        val reply = results.data.replies[0] ?: return null
        var nlu: ChatApiNlu = json.parse(ChatApiNlu.serializer(), reply.body?.getArray("nlu")?.get(0).toString())
        var resultName: String? = null;
        var resultSlots: MutableMap<String, String>? = null;

        val model = Utils.getShared(null, botContext,request);

        if (nlu.intents.isNotEmpty()) {
            for (intent in nlu.intents) {
                var intentName = chatApiToIntentName(intent.debugInfo.intent.path);
                if(!intentName.equals("main.fallback")){
                    var slots:MutableMap<String, String> = HashMap();
                    if (intent.debugInfo.slots != null) {
                        for (slot in intent.debugInfo.slots!!) {
                            slots.put(slot.name, slot.value)
                        }
                    }

                    if (model.world.canProcessIntent(intentName,Shared.jsonToDynamic(json.stringify(slots)),false)) {
                        resultName = intentName
                        resultSlots = slots;
                        break;
                    }
                }

            }

        }
        if (resultName == null && nlu.patterns.isNotEmpty()) {
            for (pattern in nlu.patterns) {
                var name = chatApiToIntentName(pattern.clazz)
                println("check pattern:" + name)
                if(!name.equals("main.fallback")){
                    println("can process:" + model.world.canProcessIntent(name,null, false))
                    if (model.world.canProcessIntent(name,null, false)) {
                        resultName = name
                        break;
                    }
                }

            }
        }
        if (resultName == null && nlu.examples.isNotEmpty()) {
            for (example in nlu.examples) {
                var name = chatApiToIntentName(example.clazz)
                if(!name.equals("main.fallback")){
                    if (model.world.canProcessIntent(name,null, false)) {
                        resultName = name
                        break;
                    }
                }
            }
        }

        println("find intent:" + resultName)

        if (resultName != null) {
            return ChatApiActivatorContext(resultName, "chatApiResult", resultSlots ?: emptyMap());
        } else {
            return ChatApiActivatorContext("main.fallback", "chatApiResult", emptyMap())
        }
    }


    class Factory(private val settings: ChatApiSettings) : ActivatorFactory {
        override fun create(model: ScenarioModel): Activator {
            return ChatApiActivator(model, settings)
        }
    }
}
