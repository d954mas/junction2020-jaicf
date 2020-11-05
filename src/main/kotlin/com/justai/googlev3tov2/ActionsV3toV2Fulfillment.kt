package com.justai.googlev3tov2


import com.google.actions.api.ActionsSdkApp
import com.google.actions.api.DefaultApp
import com.google.api.client.json.Json
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.justai.jaicf.api.BotApi
import com.justai.jaicf.channel.googleactions.*
import com.justai.jaicf.channel.http.HttpBotRequest
import com.justai.jaicf.channel.http.HttpBotResponse
import com.justai.jaicf.channel.http.asJsonHttpBotResponse
import com.justai.jaicf.channel.jaicp.JaicpCompatibleBotChannel
import com.justai.jaicf.channel.jaicp.JaicpCompatibleChannelFactory
import java.util.*

class ActionsFulfillmentV3toV2 private constructor(
    override val botApi: BotApi, private val app: DefaultApp
) : JaicpCompatibleBotChannel {

    override fun process(request: HttpBotRequest): HttpBotResponse? {
        println("**************************************************************************")
        var requestV3 = request.receiveText();
        println("*****requestV3*****")
        println(requestV3)
        val requestV3Json: JsonObject = JsonParser().parse(requestV3).asJsonObject
        var requestV3Scene = requestV3Json.getAsJsonObject("scene")

        var requestV2Json: JsonObject = JsonObject()
        var requestV2User: JsonObject = requestV3Json.getAsJsonObject("user")
        var requestV2Conversation: JsonObject = JsonObject()
        var requestV2Inputs: JsonArray = JsonArray()
        var requestV2Surface: JsonObject =
            JsonParser().parse("{      \"capabilities\":[         {\n" + "            \"name\":\"actions.capability.SCREEN_OUTPUT\"\n" + "         \n" + "},\n" + "         {\n" + "            \"name\":\"actions.capability.ACCOUNT_LINKING\"\n" + "         \n" + "},\n" + "         {\n" + "            \"name\":\"actions.capability.MEDIA_RESPONSE_AUDIO\"\n" + "         \n" + "},\n" + "         {\n" + "            \"name\":\"actions.capability.INTERACTIVE_CANVAS\"\n" + "         \n" + "},\n" + "         {\n" + "            \"name\":\"actions.capability.CUSTOM_STAGE\"\n" + "         \n" + "},\n" + "         {\n" + "            \"name\":\"actions.capability.AUDIO_OUTPUT\"\n" + "         \n" + "},\n" + "         {\n" + "            \"name\":\"actions.capability.CONTINUOUS_MATCH_MODE\"\n" + "         \n" + "}\n" + "      \n" + "]\n" + "   \n" + "}")
                .asJsonObject
        var requestV2AvailableSurface: JsonArray =
            JsonParser().parse("[      {         \"capabilities\":[            {\n" + "               \"name\":\"actions.capability.AUDIO_OUTPUT\"\n" + "            \n" + "},\n" + "            {\n" + "               \"name\":\"actions.capability.SCREEN_OUTPUT\"\n" + "            \n" + "},\n" + "            {\n" + "               \"name\":\"actions.capability.WEB_BROWSER\"\n" + "            \n" + "}\n" + "         \n" + "]\n" + "      \n" + "}\n" + "   \n" + "]")
                .asJsonArray

        requestV2Json.add("user", requestV2User)
        requestV2Json.add("conversation", requestV2Conversation)
        requestV2Json.add("inputs", requestV2Inputs)
        requestV2Json.add("surface", requestV2Surface)
        requestV2Json.add("availableSurfaces", requestV2AvailableSurface)
        requestV2Json.addProperty("isInSandbox", false)
        requestV2Json.addProperty("requestType", "SIMULATOR")

        //region user
        if (requestV2User.has("params")) {
            var params = requestV2User.getAsJsonObject("params")
            if (params.has("userStorage")) {
                var storage = params.get("userStorage").asString
                requestV2User.remove("params")
                requestV2User.addProperty("userStorage", storage)
            }
        }

        //endregion

        //region conversation
        var sesstionId = requestV3Json.getAsJsonObject("session").get("id").asString
        requestV2Conversation.addProperty("conversationId", sesstionId)
        requestV2Conversation.addProperty("type", "NEW")
        //endregion


        //region inputs
        var requestV2InputBase = JsonObject()
        var requestV2InputBaseRaw = JsonObject()
        var requestV2InputBaseRawInputs = JsonArray()
        var requestV2InputArguments: JsonArray? = null
        var intent = requestV3Json.getAsJsonObject("intent").getAsJsonPrimitive("name").asString
        if (intent == "user_utterance") {
            intent = "actions.intent.TEXT"
        }
        if (requestV3Json.has("handler")) {
            var handler = requestV3Json.getAsJsonObject("handler")
            var name = handler.getAsJsonPrimitive("name").asString
            if (name == "DigitalPurchaseCheckOk") {
                intent = "actions.intent.DIGITAL_PURCHASE_CHECK"
                requestV2InputArguments = JsonArray()

                var argumentJson = JsonObject()
                argumentJson.addProperty("name", "DIGITAL_PURCHASE_CHECK_RESULT")

                var argumentJsonExtension = JsonObject()
                argumentJsonExtension.addProperty("@type", "type.googleapis.com/google.actions.transactions.v3.DigitalPurchaseCheckResult")
                argumentJsonExtension.addProperty("resultType", "CAN_PURCHASE")

                argumentJson.add("extension", argumentJsonExtension)

                requestV2InputArguments.add(argumentJson)
            } else if (name == "DigitalPurchaseCheckError") {
                intent = "actions.intent.DIGITAL_PURCHASE_CHECK"
                requestV2InputArguments = JsonArray()

                var argumentJson = JsonObject()
                argumentJson.addProperty("name", "DIGITAL_PURCHASE_CHECK_RESULT")

                var argumentJsonExtension = JsonObject()
                argumentJsonExtension.addProperty("@type", "type.googleapis.com/google.actions.transactions.v3.DigitalPurchaseCheckResult")
                argumentJsonExtension.addProperty("resultType", "CANNOT_PURCHASE")

                argumentJson.add("extension", argumentJsonExtension)

                requestV2InputArguments.add(argumentJson)
            } else if (name == "CompletePurchase") {
                intent = "actions.intent.COMPLETE_PURCHASE"
                requestV2InputArguments = JsonArray()

                var argumentJson = JsonObject()
                argumentJson.addProperty("name", "COMPLETE_PURCHASE_VALUE")

                var argumentJsonExtension = JsonObject()
                argumentJsonExtension.addProperty("@type", "type.googleapis.com/google.actions.transactions.v3.CompletePurchaseValue")
                var status = requestV3Scene.getAsJsonObject("slots").getAsJsonObject("CompletePurchase").getAsJsonObject("value").getAsJsonPrimitive("purchaseStatus").asString
                argumentJsonExtension.addProperty("purchaseStatus", status)

                argumentJson.add("extension", argumentJsonExtension)

                requestV2InputArguments.add(argumentJson)
            }
        }

        requestV2InputBase.addProperty("intent", intent)
        requestV2InputBase.add("rawInputs", requestV2InputBaseRawInputs)
        if (requestV2InputArguments != null) {
            requestV2InputBase.add("arguments", requestV2InputArguments)
        }


        requestV2InputBaseRaw.addProperty("inputType", "VOICE")
        requestV2InputBaseRaw.addProperty("query", requestV3Json.getAsJsonObject("intent").getAsJsonPrimitive("query").asString)

        requestV2InputBaseRawInputs.add(requestV2InputBaseRaw)

        requestV2Inputs.add(requestV2InputBase)
        //endregion

        println("*****requestV2Json*****")
        var result = requestV2Json.toString()
        println(result)


        val actionRequest = app.createRequest(result, mapOf<String, String>()).apply {
            userStorage.putIfAbsent(ACTIONS_USER_ID, UUID.randomUUID().toString())
        }

        val responseBuilder = app.getResponseBuilder(actionRequest)
        val botRequest = when (actionRequest.intent) {
            TEXT_INTENT -> ActionsTextRequest(actionRequest)
            else -> ActionsIntentRequest(actionRequest)
        }

        val response = ActionsBotResponse(responseBuilder)
        val reactions = ActionsReactions(actionRequest, response)

        botApi.process(botRequest, reactions)
        var responseV2 = reactions.response.builder.build()
        println("*****responseV2*****")
        println(responseV2.toJson());

        val responseV2Json: JsonObject = JsonParser().parse(responseV2.toJson()).asJsonObject

        var responseV3Json: JsonObject = JsonObject()
        var responseV3Session: JsonObject = JsonObject()
        var responseV3Prompt: JsonObject = JsonObject()
        var responseV3User: JsonObject = JsonObject()


        responseV3Json.add("session", responseV3Session)
        responseV3Json.add("prompt", responseV3Prompt)
        responseV3Json.add("user", responseV3User)

        //region session
        responseV3Session.addProperty("id", sesstionId)
        responseV3Session.addProperty("languageCode", "")
        responseV3Session.add("params", JsonObject())
        //endregion

        //region prompt
        var responseV3PromptFirstSimple: JsonObject = JsonObject()
        var responseV3PromptCanvas: JsonObject = JsonObject()
        responseV3Prompt.addProperty("override", false)


        var inputsV2: JsonArray?
        if (responseV2.expectUserResponse == true) {
            inputsV2 = responseV2Json.get("expectedInputs").asJsonArray[0].asJsonObject.getAsJsonObject("inputPrompt").getAsJsonObject("richInitialPrompt").getAsJsonArray("items")
            var havePossibleIntents = responseV2Json.get("expectedInputs").asJsonArray[0].asJsonObject.has("possibleIntents")
            if (havePossibleIntents) {
                var possibleIntents: JsonArray = responseV2Json.get("expectedInputs").asJsonArray[0].asJsonObject.getAsJsonArray("possibleIntents")
                println("possibleIntents")
                var nextSceneJson: JsonObject? = null;
                for (v2Intent in possibleIntents) {
                    var name = v2Intent.asJsonObject.getAsJsonPrimitive("intent").asString
                    println(name)
                    if (name == "actions.intent.DIGITAL_PURCHASE_CHECK") {
                        nextSceneJson = JsonObject();
                        nextSceneJson.addProperty("name", "DigitalPurchaseCheck")
                    } else if (name == "actions.intent.COMPLETE_PURCHASE") {
                        nextSceneJson = JsonObject();
                        nextSceneJson.addProperty("name", "CompletePurchase")
                        var params = responseV3Session.get("params").asJsonObject
                        var purchaseData = JsonObject()
                        var purchaseDataSku = JsonObject()
                        purchaseData.addProperty("@type", "type.googleapis.com/google.actions.transactions.v3.CompletePurchaseValueSpec")
                        purchaseData.addProperty("developerPayload", "")


                        var data = v2Intent.asJsonObject.get("inputValueData").asJsonObject.get("skuId").asJsonObject
                        purchaseDataSku = data;

                        purchaseData.add("skuId", purchaseDataSku)
                        params.add("purchase", purchaseData);
                    }
                }
                if (nextSceneJson != null) {
                    requestV3Scene.add("next", nextSceneJson)
                }
            }
        } else {
            inputsV2 = responseV2Json.getAsJsonObject("finalResponse").getAsJsonObject("richResponse").getAsJsonArray("items")
            val nextSceneJson = JsonObject();
            nextSceneJson.addProperty("name", "actions.scene.END_CONVERSATION")
            requestV3Scene.add("next", nextSceneJson)
        }




        for (input in inputsV2) {
            var inputV2 = input.asJsonObject
            if (inputV2.has("simpleResponse")) {
                var responseInputV2 = inputV2.get("simpleResponse").asJsonObject
                if (responseInputV2.has("ssml")) {
                    responseV3PromptFirstSimple.addProperty("speech", responseInputV2.get("ssml").asString)
                    responseV3PromptFirstSimple.addProperty("text", responseInputV2.get("displayText").asString)
                    responseV3Prompt.add("firstSimple", responseV3PromptFirstSimple)
                }
            } else if (inputV2.has("htmlResponse")) {
                var responseInputV2 = inputV2.get("htmlResponse").asJsonObject
                var updatedStateJson: JsonObject = responseInputV2.get("updatedState").asJsonObject
                System.out.println(updatedStateJson.toString())
                if (updatedStateJson.has("continuousMatchConfig")) {
                    var continuousMatchConfig = updatedStateJson.get("continuousMatchConfig").asJsonObject;

                    updatedStateJson.remove("continuousMatchConfig")
                    var copyMatch = JsonObject()
                    copyMatch.addProperty("duration_seconds", continuousMatchConfig.get("duration_seconds").asInt)
                    copyMatch.add("expected_phrases", JsonArray())
                    updatedStateJson.add("continuousMatchConfig", copyMatch)

                    responseV3PromptCanvas.add("continuousMatchConfig", continuousMatchConfig);
                }
                responseV3PromptCanvas.add("data", responseInputV2.get("updatedState").asJsonObject)
                responseV3PromptCanvas.addProperty("suppressMic", responseInputV2.get("suppressMic").asBoolean)


                if (responseInputV2.has("url")) {
                    responseV3PromptCanvas.addProperty("url", responseInputV2.get("url").asString)
                }
                responseV3Prompt.add("canvas", responseV3PromptCanvas)
            }
        }

        //endregion

        //region user
        var responseV3UserParams: JsonObject = JsonObject()
        responseV3User.add("params", responseV3UserParams)

        responseV3UserParams.addProperty("userStorage", responseV2Json.get("userStorage").asString)

        responseV3Json.add("scene", requestV3Scene);

        //endregion
        println("*****responseV3Json*****")
        println(responseV3Json.toString())


        return responseV3Json.toString().asJsonHttpBotResponse()
    }

    companion object {
        private const val TEXT_INTENT = "actions.intent.TEXT"

        fun sdk(botApi: BotApi) = ActionsFulfillmentV3toV2(botApi, ActionsSdkApp())
    }


    object ActionsFulfillmentV3toV2SDK : JaicpCompatibleChannelFactory {
        override val channelType = "google"
        override fun create(botApi: BotApi) = sdk(botApi)
    }
}
