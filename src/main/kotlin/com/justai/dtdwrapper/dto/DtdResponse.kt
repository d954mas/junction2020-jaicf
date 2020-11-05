package com.justai.dtdwrapper.dto

import com.justai.dtdwrapper.interfaces.HttpResponseFacade
import org.apache.http.HttpResponse
import org.apache.http.util.EntityUtils

class DtdResponse(val response:HttpResponse):HttpResponseFacade {
    override val body: ByteArray = EntityUtils.toByteArray(response.entity)
    override val statusCode: Int
        get() = response.statusLine.statusCode
    override val statusReason: String
        get() = response.statusLine.reasonPhrase

}