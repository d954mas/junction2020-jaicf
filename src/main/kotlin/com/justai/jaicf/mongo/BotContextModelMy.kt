package com.justai.jaicf.mongo

import com.justai.jaicf.context.DialogContext

data class BotContextModelMy(
    val _id: String,

    val result: Any?,
    val gameStorage: String?,

    val client: Map<String, Any?>,
    val session: Map<String, Any?>,
    val dialogContext: DialogContext
)