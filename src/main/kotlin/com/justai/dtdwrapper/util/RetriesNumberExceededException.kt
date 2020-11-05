package com.justai.dtdwrapper.util

import java.lang.Exception

class RetriesNumberExceededException : Exception("Too many unsuccessful retries") {
}