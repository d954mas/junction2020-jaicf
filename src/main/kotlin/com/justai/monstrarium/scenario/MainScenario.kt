package com.justai.monstrarium.scenario

import com.google.api.services.actions_fulfillment.v2.model.HtmlResponse
import com.google.gson.Gson
import com.justai.jaicf.activators.connector.chatapi.chatApi
import com.justai.jaicf.channel.googleactions.actions
import com.justai.jaicf.context.ActionContext
import com.justai.jaicf.context.BotContext
import com.justai.jaicf.model.scenario.Scenario
import com.justai.monstrarium.analytics.MonstrariumAnalyticsFacade
import com.justai.monstrarium.iap.IapGoogle
import com.justai.monstrarium.utils.Config
import com.justai.monstrarium.utils.GameContext
import com.justai.monstrarium.utils.Utils
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.stringify
import org.slf4j.LoggerFactory
import shared.Shared
import shared.base.NativeApi
import shared.project.analytics.events.common.DtdAnalyticsEvent



class NativeApiJava(val context: BotContext, val action: ActionContext) : NativeApi {
    private val json = Json(JsonConfiguration.Stable.copy(strictMode = false, encodeDefaults = false))
    private var enabled: Boolean = true;
    override fun saveStorage(s: String?) {
        context.client["game_storage"] = s;
    }

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled;
    }

    override fun convExit() {
        if (enabled) {
            action.reactions.actions?.endConversation()
        }

    }

    override fun contextSet(s: String?, i: Int, o: Any?) {
        s!!
        var contexts = context.client["game_contexts"]
        if (contexts == null) {
            contexts = HashMap<String, GameContext>()
        }
        var contextsMap: HashMap<String, GameContext> = contexts as HashMap<String, GameContext>
        contextsMap.put(s, GameContext(s, o?.toString(), i))
        context.client["game_contexts"] = contextsMap

    }

    override fun convAsk(s: String?) {
        if (enabled) {
            if (s != null) {
                var str = s;
                action.context.temp["say"] = true
                str = str.replace("<color=1,0.4,0,1>", "")
                str = str.replace("</color>", "")
                action.reactions.say(str)
            }
        } else {
            System.out.println(s)
        }

    }

    override fun convAskHtmlResponse(s: String?) {
        if (enabled) {
            context.session.put("htmlResponse", s)
            //   }
        } else {
            System.out.println(s)
        }


    }

    override fun flushDone() {}

    //region haxe trash
    override fun __hx_getField(p0: String?, p1: Boolean, p2: Boolean, p3: Boolean): Any {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun __hx_invokeField(p0: String?, p1: Array<out Any>?): Any {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun __hx_setField(p0: String?, p1: Any?, p2: Boolean): Any {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun __hx_setField_f(p0: String?, p1: Double, p2: Boolean): Double {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun __hx_lookupSetField_f(p0: String?, p1: Double): Double {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun __hx_getField_f(p0: String?, p1: Boolean, p2: Boolean): Double {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun __hx_lookupSetField(p0: String?, p1: Any?): Any {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun __hx_lookupField_f(p0: String?, p1: Boolean): Double {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun __hx_lookupField(p0: String?, p1: Boolean, p2: Boolean): Any {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun __hx_getFields(p0: haxe.root.Array<String>?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
    //endregion
}

@ImplicitReflectionSerializer
object MainScenario : Scenario() {
    private val json = Json(JsonConfiguration.Stable.copy(strictMode = false, encodeDefaults = false))
    private val LOGGER = LoggerFactory.getLogger(MainScenario::class.java)
    private val IAP_GOOGLE: IapGoogle = IapGoogle();


    init {
        state("chatApiResult", noContext = true) {
            activators {
                intent("chatApiResult")
            }



            action {
                context.temp["say"] = false
                LOGGER.info("user_id:" + context.clientId)
                var input = Utils.fixedInputQuery(context, request)
                LOGGER.info("input:" + input)

                val nativeApi: NativeApiJava = NativeApiJava(context, this)
                val model = Utils.getShared(nativeApi, context, request);
                var conversationID: String? = request.actions?.request?.sessionId;
                if (conversationID == null) {
                    conversationID = "0001";
                }
                model.setConversationID(conversationID);
                model.updateServerTime(null);
                try {
                    var intent: String = activator.chatApi!!.name
                    var data: Any? = null;

                    var slots: MutableMap<String, String> = activator.chatApi!!.slots.toMutableMap()
                    LOGGER.info(String.format("intent:%s slots:%s", intent, slots.toString()));

                    if (slots.isNotEmpty()) {
                        data = Shared.jsonToDynamic(json.stringify(slots))
                    }

                    if (intent == "main.welcome") {
                        context.cleanSessionData()
                        model.setUUID(context.clientId)
                    }

                    if (intent == "actions.intent.DIGITAL_PURCHASE_CHECK") {
                        LOGGER.info("actions.intent.DIGITAL_PURCHASE_CHECK")
                        intent = IAP_GOOGLE.digitalPurchaseCheckToIntent(context, request, reactions)
                        LOGGER.info(intent)
                        if (intent == "actions.iap.can_purchase") {
                            if (context.client["iapKey"] != null) {
                                LOGGER.info("initiatePurchase")
                                IAP_GOOGLE.digitalPurchaseCBuildOrder(context, request, reactions)
                                IAP_GOOGLE.initiatePurchase(context, request, reactions, context.client["iapKey"] as String?)
                            }
                            LOGGER.info("build order success")
                        }
                    }

                    if (intent == "actions.intent.COMPLETE_PURCHASE") {
                        if (IAP_GOOGLE.handlePurchaseResponse(context, request, reactions)) {
                           // intent = "actions.iap.complete"
                            //тк навык закрывается. То нужно перезапустить его через main.welcome
                            intent = "main.welcome"
                            //  val payload = JSONObject()
                            // payload["iapKey"] = context.client["iapKey"]
                            // data = Shared.jsonToDynamic(payload.toJSONString())
                        } else {
                            //Не уверен что навык закрывается. Оствил вдруг возможна ситуация когда, failed и навык не закрыли
                            intent = "actions.iap.failed"
                        }
                    }

                   // nativeApi.setEnabled(false)
                    //IAP_GOOGLE.handlePurchaseMakeConsumable(context, request, reactions, model)
                   // nativeApi.setEnabled(true)

                    LOGGER.info(String.format("result intend final:%s data:%s", intent, data.toString()))

                    model.processIntent(intent, data);


                    println("MATCHED SHARED ENABLED:" + model.continuosMatchIsEnabled())
                    if (model.continuosMatchIsEnabled()) {

                    }


                    var htmlResponse = context.session["htmlResponse"] as String?
                    if (htmlResponse != null) {
                        var map = Utils.jsonToMap(json.parseJson(htmlResponse).jsonObject)
                        MonstrariumAnalyticsFacade.sendAnalytics(map)

                        var response = (map["response"] as MutableMap<String, Any>)
                        response["analyticEvents"] = emptyList<DtdAnalyticsEvent>()
                        map["response"] = response


                        var resp = HtmlResponse()
                        var intent = map.get("intent")
                        var matchShared = model.continuosMatchGetJson() as String
                        if (matchShared != "") {
                            println(map.get("continuousMatchConfig"))
                            map.set("continuousMatchConfig", Utils.jsonToMap(json.parseJson(matchShared).jsonObject))
                        }


                        //if (intent == "main.welcome") {
                            resp.setUrl(Config.HTML_URL)
                       // }
                        if (intent == "main.welcome") {
                            resp.setSuppressMic(true)
                        } else {
                            resp.setSuppressMic(false)
                        }
                        this.reactions.actions?.response?.builder?.add(resp.setUpdatedState(map))

                    }

                    if (context.temp["say"] == false) {
                        reactions.say(" ")
                    }

                } catch (e: Exception) {
                    model.processError(e.toString());
                    LOGGER.error(e.toString())
                    e.printStackTrace();
                }
            }

        }

        state("fallback", noContext = true) {
            activators {
                catchAll()
            }

            action {
                reactions.say("You say: ${request.input}\n")
                reactions.say("fallback")
            }
        }
    }
}