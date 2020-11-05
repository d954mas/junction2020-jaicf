package com.justai.jaicf.activators.connector.chatapi

import com.justai.jaicf.activator.Activator
import com.justai.jaicf.activator.ActivatorFactory
import com.justai.jaicf.activator.caila.CailaNLUSettings
import com.justai.jaicf.activator.intent.BaseIntentActivator
import com.justai.jaicf.activator.intent.IntentActivatorContext
import com.justai.jaicf.api.BotRequest
import com.justai.jaicf.context.BotContext

import com.justai.jaicf.model.scenario.ScenarioModel
import org.slf4j.LoggerFactory;


class ChatApiSettings(
    val url: String,
    val urlRU: String
)
