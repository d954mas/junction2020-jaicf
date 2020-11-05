package com.justai.jaicf.mongo

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.justai.jaicf.context.BotContext
import com.justai.jaicf.context.manager.BotContextManager
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Filters
import com.mongodb.client.model.UpdateOptions
import org.bson.Document

class MongoBotContextManagerMy(
    private val collection: MongoCollection<Document>
) : BotContextManager {

    private val mapper = jacksonObjectMapper().enableDefaultTyping()

    override fun loadContext(clientId: String): BotContext {
        return collection
            .find(Filters.eq("_id", clientId))
            .iterator().tryNext()?.let { doc ->
                if (doc.containsKey("gameStorage")) {
                    doc.set("gameStorage", doc.get("gameStorage", Document::class.java).toJson())
                }
                val model = mapper.readValue(doc.toJson(), BotContextModelMy::class.java)
                BotContext(model._id, model.dialogContext).apply {
                    result = model.result
                    client.putAll(model.client)
                    session.putAll(model.session)
                    client["game_storage"] = model.gameStorage
                }


            } ?: BotContext(clientId)
    }

    override fun saveContext(botContext: BotContext) {
        if(botContext.client["game_storage"] != null){
            var gameStorage: String = botContext.client["game_storage"] as String;

            botContext.client["game_storage"] = "game_storage";
            BotContextModelMy(
                _id = botContext.clientId,
                result = botContext.result,
                client = botContext.client.toMap(),
                session = botContext.session.toMap(),
                dialogContext = botContext.dialogContext,
                gameStorage = "game_storage"
            ).apply {
                var resultJson = mapper.writeValueAsString(this);
                resultJson = resultJson.replaceFirst("\"gameStorage\":\"game_storage\"", "\"gameStorage\":" + gameStorage);
                val doc = Document.parse(resultJson)
                collection.replaceOne(Filters.eq("_id", _id), doc, UpdateOptions().upsert(true))
            }
        }

    }
}