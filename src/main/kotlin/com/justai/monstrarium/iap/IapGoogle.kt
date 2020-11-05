package com.justai.monstrarium.iap

import com.google.actions.api.ActionContext
import com.google.actions.api.ActionResponse
import com.google.actions.api.response.ResponseBuilder
import com.google.actions.api.response.helperintent.CompletePurchase
import com.google.actions.api.response.helperintent.DigitalPurchaseCheck
import com.google.api.services.actions_fulfillment.v2.model.*
import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import com.google.gson.reflect.TypeToken
import com.justai.jaicf.api.BotRequest
import com.justai.jaicf.channel.googleactions.actions
import com.justai.jaicf.context.BotContext
import com.justai.jaicf.reactions.Reactions
import com.justai.monstrarium.analytics.PurchaseEvent
import com.justai.monstrarium.analytics.PurchaseEventEntry
import com.justai.monstrarium.iap.billing.DigitalGoodsService
import com.justai.monstrarium.iap.billing.DigitalGoodsService.PurchaseResult.*
import com.justai.monstrarium.iap.billing.SkuDetails
import com.justai.monstrarium.scenario.MainScenario
import org.json.simple.JSONObject
import org.slf4j.LoggerFactory
import shared.Shared
import java.util.*
import java.util.Date
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class IapGoogle {

    companion object {
        private const val SKU_DETAILS_LIST_KEY = "skuDetailsList"
        private val CONSUMABLE_IDS = Arrays.asList("monstrarium.gold.pack.1", "monstrarium.gold.pack.2", "monstrarium.gold.pack.3")
        private const val BUILD_ORDER_CONTEXT = "build-the-order"
        private const val BUILD_ORDER_LIFETIME = 5
        private val LOGGER = LoggerFactory.getLogger(IapGoogle::class.java)

    }

    fun getSkuList(context: BotContext, request: BotRequest, reactions: Reactions): List<SkuDetails> {
        val skuDetailsList: List<SkuDetails> = DigitalGoodsService.getSkuDetails(request.actions!!.request.appRequest!!.conversation.conversationId)
        return skuDetailsList
    }

    fun addDigitalPurchaseCheck(context: BotContext, request: BotRequest, reactions: Reactions) {
        LOGGER.info("iap check")
        reactions.actions?.response?.builder?.add(DigitalPurchaseCheck())
    }

    fun digitalPurchaseCheckToIntent(context: BotContext, request: BotRequest, reactions: Reactions): String {
        try {
            val checkResult = Objects.requireNonNull(request.actions?.request?.getArgument("DIGITAL_PURCHASE_CHECK_RESULT"))!!.extension["resultType"].toString()
            if (checkResult.equals("CAN_PURCHASE", ignoreCase = true)) {
                return "actions.iap.can_purchase"
            } else if (checkResult.equals("CANNOT_PURCHASE", ignoreCase = true) || checkResult.equals("RESULT_TYPE_UNSPECIFIED",
                    ignoreCase = true)) { // User does not meet necessary conditions for completing a digital
// purchase. This may be due to location, device or other factors.
                return "actions.iap.can_not_purchase"
            }
        } catch (e: Exception) {
            return "actions.iap.internal_error"
        }
        return "actions.iap.internal_error"
    }


    fun digitalPurchaseCBuildOrder(context: BotContext, request: BotRequest, reactions: Reactions): ActionResponse {
        LOGGER.info("Build the Order Intent")
        val responseBuilder: ResponseBuilder = reactions.actions?.response?.builder!!
        val response: ActionResponse
        val requestGoogle = request.actions!!.request;
        try {
            val skuDetailsList: List<SkuDetails> = DigitalGoodsService.getSkuDetails(request.actions!!.request.appRequest!!.conversation.conversationId)
            if (skuDetailsList == null || skuDetailsList.size == 0) {
                responseBuilder.add("Oops, looks like there is nothing available. " + "Please try again later");
            } else {
                responseBuilder.conversationData!![SKU_DETAILS_LIST_KEY] = createSkuIdToSkuDetailsMap(skuDetailsList)
                /*responseBuilder.add("Great! I found the following items: " + buildSimpleResponse(skuDetailsList))
                val screenAvailable = requestGoogle.hasCapability(Capability.SCREEN_OUTPUT.value)
                if (screenAvailable) {
                    val skuDetailsListShow: MutableList<SkuDetails> = ArrayList();
                    for(sku in skuDetailsList){
                        if(sku.skuId == context.client["iapKey"]){
                            skuDetailsListShow.add(sku);
                        }
                    }
                    if (skuDetailsListShow.size == 0) {
                        responseBuilder.add("Oops, looks like there is nothing available, with key:" + context.client["iapKey"] + "Please try again later");
                    }
                    val carousel: SelectionCarousel = SelectionCarousel().setItems(buildCarouselItems(skuDetailsListShow))
                    responseBuilder.add(carousel)
                }*/
            }
        } catch (e: Exception) {
            LOGGER.info("Exception in welcome $e")
            e.printStackTrace()
            responseBuilder.add("Oops, something went wrong. Try again later").endConversation()
        } finally {
            response = responseBuilder.build()
        }
        return response
    }

    private fun buildSimpleResponse(skus: List<SkuDetails>): String {
        val s: MutableList<String> = ArrayList()
        for (sku in skus) {
            s.add(sku.getTitle())
        }
        return java.lang.String.join(",", s)
    }

    private fun createSkuIdToSkuDetailsMap(skus: List<SkuDetails>): String {
        val map: HashMap<String, SkuDetails> = HashMap()
        for (sku in skus) {
            map[sku.getSkuId().getId()] = sku
        }
        // needed to ensure proper serialization
        return Gson().toJson(map)
    }

    private fun buildCarouselItems(skuDetailsList: List<SkuDetails>): List<CarouselSelectCarouselItem> {
        val items: MutableList<CarouselSelectCarouselItem> = ArrayList()
        var item: CarouselSelectCarouselItem
        for (sku in skuDetailsList) {
            item = CarouselSelectCarouselItem()
            item.setTitle(sku.getTitle())
            item.setDescription(sku.getDescription())
            val optionInfo = OptionInfo()
            optionInfo.setKey(sku.getSkuId().getId())
            item.setOptionInfo(optionInfo)
            items.add(item)
        }
        return items
    }

    // @ForIntent("Initiate the Purchase")
    fun initiatePurchase(context: BotContext, requestJaicf: BotRequest, reactions: Reactions, skuId: String?): ActionResponse {
        LOGGER.info("Initiate Purchase")
        val responseBuilder: ResponseBuilder = reactions.actions?.response?.builder!!
        val response: ActionResponse
        val request = requestJaicf.actions!!.request;
        var selectedSkuId: String? = skuId
        if (selectedSkuId == null) {
            if (request.getArgument("OPTION") != null) {
                selectedSkuId = request.getArgument("OPTION")!!.textValue
                LOGGER.info("selectedSkuId = $selectedSkuId")
            } else {
                selectedSkuId = if (request.getParameter("SKU") == null) null else request.getParameter("SKU").toString()
            }
            if (selectedSkuId == null) {
                responseBuilder.add("Oops, something went wrong, sorry.")
                return responseBuilder.endConversation().build()
            }
        }

        LOGGER.info(selectedSkuId)
        val skuDetailsSerMap = request.conversationData[SKU_DETAILS_LIST_KEY] as String?
        LOGGER.info("skuDetailsMap = " + skuDetailsSerMap.toString())
        // See what this line does in
// https://google.github.io/gson/apidocs/com/google/gson/Gson.html
// We need Gson to do serialization to convert back to the map.
        val skuDetailsMap: Map<String, SkuDetails> = Gson().fromJson(skuDetailsSerMap, object : TypeToken<HashMap<String?, SkuDetails?>?>() {}.getType())
        val selectedSku: SkuDetails = skuDetailsMap[selectedSkuId]!!
        if (selectedSku == null) {
            responseBuilder.add("selectedSku is null")
            return responseBuilder.build()
        }
        LOGGER.info("Found selected Sku: " + selectedSku.getDescription())
        responseBuilder.conversationData!!["purchasedItemSku"] = selectedSku
        val purchaseHelper: CompletePurchase = CompletePurchase().setSkuId(selectedSku.getSkuId())
        responseBuilder.add("Great! Here you go.")
        responseBuilder.add(purchaseHelper)
        return responseBuilder.build()
    }

    private fun findSelectedEntitlement(
        entitlements: List<PackageEntitlement>, selectedSku: SkuDetails?
    ): Entitlement? {
        for (entitlementGroup in entitlements) {
            for (entitlement in entitlementGroup.getEntitlements()) {
                if (entitlement.getSku().equals(selectedSku!!.getSkuId().getId())) {
                    return entitlement
                }
            }
        }
        return null
    }

    fun handlePurchaseMakeConsumable(context: BotContext, requestJaicf: BotRequest, reactions: Reactions, model: Shared) {
        if (requestJaicf.actions != null && requestJaicf.actions!!.request.user != null) {
            var user = requestJaicf.actions!!.request.user
            var packages = user?.packageEntitlements
            val request = requestJaicf.actions!!.request;
            if (packages != null) {
                for (pack in packages) {
                    LOGGER.info(pack.toString());
                    for (en in pack.entitlements) {
                        LOGGER.info(en.toString())
                        if (en.skuType == "IN_APP") {
                            LOGGER.info("TRY Consumed!")
                            val purchaseToken = en.getInAppDetails().getInAppPurchaseData().get("purchaseToken") as String
                            try {
                                var result = DigitalGoodsService.consumePurchase(request.appRequest!!.conversation.conversationId, purchaseToken)
                                if(result!= null){
                                    LOGGER.info("----- Consumed!")
                                    LOGGER.info(result.toString())

                                    var intent = "actions.iap.buy"
                                    val payload = JSONObject()
                                    payload["iapKey"] = context.client["iapKey"]
                                    var data = Shared.jsonToDynamic(payload.toJSONString())

                                    context.client["iapKey"] = null

                                    LOGGER.info(String.format("shop consumed final:%s data:%s", intent, data.toString()))
                                    model.processPurchase(data);

                                    var entries: MutableList<PurchaseEventEntry> = ArrayList()
                                    /*entries.add(
                                        PurchaseEventEntry(
                                            orderId = status.invoiceId.toString(),
                                            price = 0f, //надо получить цену
                                            currencyCode = "USD", timestamp = Date().time
                                        )
                                    )
                                    thread.sendEvent(
                                        userId = context.clientId,
                                        eventName = "rp",
                                        level = model.world.userLevel,
                                        data = PurchaseEvent(
                                            name = status.invoice.order.orderBundle[0].name,
                                            entries = entries
                                        )
                                    )*/

                                }

                            } catch (e: Exception) {
                                LOGGER.info("----- Consumed! failed")
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }

        }


    }

    // @ForIntent("Describe the Purchase Status")
    fun handlePurchaseResponse(context: BotContext, requestJaicf: BotRequest, reactions: Reactions): Boolean {
        LOGGER.info("Describe the Purchase Status intent start.")

        val rb: ResponseBuilder = reactions.actions?.response?.builder!!
        val response: ActionResponse
        val request = requestJaicf.actions!!.request;


        if (request.getArgument("COMPLETE_PURCHASE_VALUE") == null) {
            rb.add("Purchased failed. Check the logs.")
            return false
        }
        val status = request.getArgument("COMPLETE_PURCHASE_VALUE")!!.extension["purchaseStatus"] as String?
        LOGGER.info(status);
        if (status.equals(java.lang.String.valueOf(PURCHASE_STATUS_OK), ignoreCase = true)) {
           // rb.add(ActionContext(BUILD_ORDER_CONTEXT, BUILD_ORDER_LIFETIME))
          // rb.add("You've successfully purchased the item!. Would you like to " + "do anything else?")
          //  LOGGER.info(request.conversationData["purchasedItemSku"].toString())
           // var data = request.conversationData["purchasedItemSku"] as LinkedTreeMap<String, Any>
           // var skuIdData = data.get("skuId") as LinkedTreeMap<String, Any>
          //  var skuId = SkuId()
         //   skuId.setId(skuIdData.get("id") as String?)
           // skuId.setPackageName(skuIdData.get("packageName") as String?)
         //   skuId.setSkuType(skuIdData.get("skuType") as String?)
         //   val selectedSku: SkuDetails? = SkuDetails(data.get("title") as String?, data.get("description") as String?, skuId, data.get("formattedPrice") as String?)
            /*if (CONSUMABLE_IDS.contains(selectedSku!!.getSkuId().getId())) {
                val entitlementForSelectedSku: Entitlement? = findSelectedEntitlement(request.user!!.packageEntitlements, selectedSku)
                val purchaseToken = entitlementForSelectedSku!!.getInAppDetails().getInAppPurchaseData().get("purchaseToken") as String
                try {
                    DigitalGoodsService.consumePurchase(request.appRequest!!.conversation.conversationId, purchaseToken)
                    LOGGER.info("----- Consumed!")
                    return  true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }*/
            return true
        } else if (status.equals(java.lang.String.valueOf(PURCHASE_STATUS_ALREADY_OWNED), ignoreCase = true)) {
            rb.add("Purchase failed. You already own the item.");
            return false;
        } else if (status.equals(java.lang.String.valueOf(PURCHASE_STATUS_ITEM_UNAVAILABLE), ignoreCase = true)) {
            rb.add("Purchase failed. Item is not available.");
            return false
        } else if (status.equals(java.lang.String.valueOf(PURCHASE_STATUS_ITEM_CHANGE_REQUESTED), ignoreCase = true)) {
            rb.add(ActionContext(BUILD_ORDER_CONTEXT, BUILD_ORDER_LIFETIME))
            rb.add("Looks like you've changed your mind. Would you like " + "to try again?")
            return false
        } else if (status.equals(java.lang.String.valueOf(PURCHASE_STATUS_USER_CANCELLED), ignoreCase = true)) {
            rb.add(ActionContext(BUILD_ORDER_CONTEXT, BUILD_ORDER_LIFETIME))
            rb.add("Looks like you've cancelled the purchase. Do you still want " + "to try to do a purchase?")
            return false
        } else if (status.equals(java.lang.String.valueOf(PURCHASE_STATUS_ERROR), ignoreCase = true) || status.equals(java.lang.String.valueOf(PURCHASE_STATUS_UNSPECIFIED), ignoreCase = true)) {
            rb.add("Purchase failed. try again later.")
            return false
        } else {
            rb.add("Oops, there was an internal error. Please try again later")
            return false
        }
        return false
    }

}