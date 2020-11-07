package com.justai.jaicf.activators.connector.chatapi

import com.justai.jaicf.activator.Activator
import com.justai.jaicf.activator.ActivatorFactory
import com.justai.jaicf.activator.intent.BaseIntentActivator
import com.justai.jaicf.activator.intent.IntentActivatorContext
import com.justai.jaicf.api.BotRequest
import com.justai.jaicf.channel.googleactions.actions
import com.justai.jaicf.context.BotContext

import com.justai.jaicf.model.scenario.ScenarioModel
import com.justai.monstrarium.utils.Utils
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.stringify
import shared.Shared
import shared.base.SpeechCommands
import shared.project.enums.Intents


@ImplicitReflectionSerializer
class IntentCommandActivator(
    model: ScenarioModel
) : BaseIntentActivator(model) {
    private val json = Json(JsonConfiguration.Stable.copy(strictMode = false, encodeDefaults = false))

    override fun canHandle(request: BotRequest) = true

    override fun recogniseIntent(botContext: BotContext, request: BotRequest): IntentActivatorContext? {
        val model = Utils.getShared(null, botContext, request);
        var text = request.input

        if(text == "actions.intent.DIGITAL_PURCHASE_CHECK" || text == "actions.intent.COMPLETE_PURCHASE"){
            return ChatApiActivatorContext(text, "chatApiResult", emptyMap());
        }
       // println("sessionId")
       // println(botContext.session["sessionId"])
       // println(request.actions!!.request.sessionId)
       // println(text.toLowerCase())
       // println(request.actions != null && text.toLowerCase() == "talk to junk game".toLowerCase())
        // Плохо работает чтото на стороне гугла.Не начинает новую сессию
        if(request.actions != null && text.toLowerCase() == "talk to junk game".toLowerCase()
            //&&  botContext.session["sessionId"] != request.actions!!.request.sessionId
        ) { //fix not restart in google console. Todo add check session.id
           // botContext.session["sessionId"] = request.actions!!.request.sessionId
           // return ChatApiActivatorContext("main.welcome", "chatApiResult", emptyMap());
        }
        botContext.session["sessionId"] = request.actions!!.request.sessionId

        if(text == "actions.intent.CANCEL"){
            return ChatApiActivatorContext("main.game_exit", "chatApiResult", emptyMap());
        }

        var parts = text.split(" ").toMutableList();
        var slots: MutableMap<String, String> = mutableMapOf()
        var name:String = Utils.fixedIntentName(Utils.chatApiToIntentName(parts[0]))
        if (Intents.isIntent(name)) {
            if (parts.size > 1) {
                when (name) {
                    "level.spawn.unit" -> {
                        slots = mutableMapOf("unit" to parts[1])
                        if (parts.size > 2) slots["amount"] = parts[2]
                    }
                    "main.help" -> slots = mutableMapOf("question" to parts[1])
                }
            }

            if (model.world.canProcessIntent(name, Shared.jsonToDynamic(json.stringify(slots.toMap())), false)) {
                return ChatApiActivatorContext(name, "chatApiResult", slots);
            }
        }

        var intent = SpeechCommands.checkIntent(model.world, text);
        if (intent != null) {
            return ChatApiActivatorContext(intent, "chatApiResult", mutableMapOf());
        }
        return null
    }


    companion object : ActivatorFactory {
        override fun create(model: ScenarioModel): Activator {
            return IntentCommandActivator(model)
        }
    }
}
