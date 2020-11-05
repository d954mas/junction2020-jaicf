package com.justai.monstrarium.utils

import com.google.api.services.actions_fulfillment.v2.model.HtmlResponse
import com.justai.jaicf.api.BotRequest
import com.justai.jaicf.channel.googleactions.actions
import com.justai.jaicf.context.ActionContext
import com.justai.jaicf.context.BotContext
import com.justai.monstrarium.scenario.MainScenario
import com.justai.monstrarium.scenario.NativeApiJava
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import kotlinx.serialization.stringify
import org.json.JSONArray
import org.json.JSONObject
import shared.Shared
import shared.base.NativeApi
import java.util.*
import java.util.stream.Collectors.toList
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


@Serializable
data class GameUser(
    val conv: String, val locale: String, val permissions: String, val verification: String, val lastSeen: String
)

@Serializable
data class GameContext(
    val name: String, val parameters: String?, val lifespan: Int
)

@Serializable
data class GameIntentWordSayData(
    val word: String, val word_exist: Boolean
)

class NativeApiEmpty() : NativeApi {
    override fun saveStorage(s: String?) {}
    override fun convExit() {}
    override fun contextSet(s: String?, i: Int, o: Any?) {}
    override fun convAsk(s: String?) {}
    override fun convAskHtmlResponse(s: String?) {}
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


class Utils {
    companion object {
        private val json = Json(JsonConfiguration.Stable.copy(strictMode = false, encodeDefaults = false))
        private val RU_QUERY_FIX = mapOf(
            "." to "точка", "0" to "нуль", "1" to "один", "2" to "два", "3" to "три",
            "4" to "четыре", "5" to "пять", "6" to "шесть", "7" to "семь", "8" to "восемь",
            "9" to "девять", "10" to "десять", "11" to "одиннадцать", "12" to "двенадцать",
            "13" to "тринадцать", "14" to "четырнадцать", "15"
                    to "пятнадцать", "16" to "шестнадцать", "17" to "семнадцать",
            "18" to "восемнадцать", "19" to "деватнадцать", "20" to "двадцать",
            "30" to "тридцать", "40" to "сорок", "50" to "пятьдесят", "60" to "шестьдесят",
            "70" to "семьдесят", "80" to "восемдесят", "90" to "девяносто", "100" to "сто",
            "200" to "двести", "300" to "триста", "400" to "четыреста", "500" to "пятьсот",
            "600" to "шестьсот", "700" to "семьсот", "800" to "восемьсот", "900" to "девятьсот",
            "1000" to "тысяча"
        )
        private val EN_QUERY_FIX = mapOf(
            "." to "dot", "0" to "zero", "1" to "one", "2" to "two", "3" to "three",
            "4" to "four", "5" to "five", "6" to "siz", "7" to "seven", "8" to "eight",
            "9" to "nine", "10" to "ten", "11" to "eleven", "12" to "twelve",
            "13" to "thirteen", "14" to "fourteen", "15"
                    to "fifteen", "16" to "sixteen", "17" to "seventeen",
            "18" to "eighteen", "19" to "nineteen", "20" to "twenty",
            "30" to "thirty", "40" to "forty", "50" to "fifty",
            "60" to "sixty", "70" to "seventy","80" to "eighty", "90" to "ninety"
        )

        @UseExperimental(ImplicitReflectionSerializer::class)
        fun getGameContextsString(context: BotContext): String {
            var contexts = context.client["game_contexts"]
            if (contexts == null || contexts == "null") {
                contexts = "{}";
            } else {
                var contextsMap = contexts as HashMap<String, GameContext>
                var contextsArray: MutableList<GameContext> = ArrayList()
                for (pair in contextsMap) {
                    contextsArray.add(pair.value)
                }
                contexts = json.stringify(contextsArray)
            }
            return contexts;
        }

        fun isContextExist(context: BotContext, name: String): Boolean {
            var contexts = context.client["game_contexts"]
            if (contexts != null) {
                var contextsMap = contexts as HashMap<String, GameContext>
                var ctx = contextsMap.get(name);
                return ctx != null && ctx.lifespan > 0
            }
            return false
        }

        fun getGameContextsHaxeMap(context: BotContext): haxe.IMap<String, shared.base.struct.ContextStruct> {
            var contexts = getGameContextsString(context);
            return Shared.ContextJsonToMap(contexts);
        }

        fun getLocale(context: BotContext, request: BotRequest): String {
            var localeStr = request.actions?.request?.user?.locale
            if (localeStr == null) {
                localeStr = "en-US"
            }
            if (localeStr == "ru-RU") {
                context.temp["locale"] = "ru"
            } else {
                context.temp["locale"] = "en"
            }

            val locale: String? = context.temp["locale"] as String?
            return locale ?: "en"
        }

        fun getGameStorageString(context: BotContext, request: BotRequest): String {
            return getGameStorageString(context, getLocale(context, request));
        }

        fun getGameStorageString(context: BotContext, locale: String): String {

            val user = GameUser("1111", locale, "", "", "")
            var storage: String? = context.client["game_storage"]?.toString()
            if (storage == null || storage == "null") {
                storage = "{}";
            }
            val version = Utils::class.java.classLoader.getResource("version.json").readText()
            storage = Shared.prepareStorage(storage, json.toJson(GameUser.serializer(), user).toString(), version)
            return storage;

        }

        fun fixedIntentName(name: String): String {
            if (name == "actions.intent.MAIN") {
                return "main.welcome";
            }
            return name;
        }

        fun chatApiToIntentName(name: String): String {
            var result = name.removeSuffix("/intent")
            result = result.removeSuffix("/pattern")
            result = result.replace("/", ".")
            if (result.startsWith(".")) {
                result = result.substring(1);
            }
            return result.trim();
        }

        fun jsonToMap(json: JsonObject): MutableMap<String, Any> {
            val map: MutableMap<String, Any> = HashMap()

            val mapJson: Map<String, JsonElement> = json.toMap();

            for (pair in mapJson) {
                val key = pair.key
                var value: JsonElement = pair.value
                if (value.isNull) {

                } else if (value is JsonLiteral) {
                    if (value.isString) {
                        map.put(key, value.primitive.content)
                    } else if (value.booleanOrNull != null) {
                        map.put(key, value.boolean)
                    } else {
                        map.put(key, value.double)
                    }

                } else if (value is JsonObject) {
                    map.put(key, jsonToMap(value))
                } else if (value is JsonArray) {
                    map.put(key, jsonToArray(value.jsonArray))
                } else {
                    throw Exception("unknow json")
                }
            }
            return map
        }

        fun jsonToArray(json: JsonArray): List<Any> {
            var list: MutableList<Any> = ArrayList();
            for (value in json.content) {
                if (value is JsonPrimitive) {
                    list.add(value.primitive.content)
                } else if (value is JsonObject) {
                    list.add(jsonToMap(value))
                } else if (value is JsonArray) {
                    list.add((jsonToArray(value)))
                }
            }
            return list;
        }

        fun getShared(native: NativeApi?, context: BotContext, request: BotRequest): Shared {
            var nativeApiUsed = native;
            if (nativeApiUsed == null) {
                nativeApiUsed = NativeApiEmpty()
            }
            return Shared(nativeApiUsed, getGameStorageString(context, request), getGameContextsString(context), false, Config.isDev);
        }


        fun fixedInputQuery(context: BotContext, request: BotRequest): String {
            var locale = getLocale(context, request);
            var input = request.input
            var words: Map<String, String> = EN_QUERY_FIX
            if (locale == "ru") {
                words = RU_QUERY_FIX
            }
            return words.getOrDefault(input, input)
        }

    }
}
