package com.justai.dtdwrapper.util

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets.UTF_8
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

fun compressGzip(data: String): ByteArray {
    val outStream = ByteArrayOutputStream()
    GZIPOutputStream(outStream).bufferedWriter(UTF_8).use { it.write(data) }
    return outStream.toByteArray()
}

fun decompressGzip(content: ByteArray) =
        GZIPInputStream(content.inputStream()).bufferedReader(UTF_8).use { it.readText() }