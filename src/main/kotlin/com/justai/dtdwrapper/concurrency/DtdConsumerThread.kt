package com.justai.dtdwrapper.concurrency

import com.justai.dtdwrapper.DevtodevAnalyticsWrapper
import com.justai.dtdwrapper.interfaces.HttpResponseFacade
import com.justai.dtdwrapper.util.RetriesNumberExceededException
import java.util.*
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.locks.ReentrantLock

class DtdConsumerThread private constructor(private val wrapper: DevtodevAnalyticsWrapper, private val capacity: Int, private val retries: Int) : Thread() {
    private val lock = ReentrantLock()
    private val notEmpty = lock.newCondition()

    //blocking queue might be overkill here, considering having locks on sendEvent
    private val queue: BlockingQueue<DtdEntry> = LinkedBlockingQueue()
    override fun run() {
        var counter = 0
        while (true) {
            if (counter == capacity) {
                flush()
                counter = 0
            }
            send()
            counter++
        }
    }

    private fun send() {
        lock.lock()
        val entry: DtdEntry;
        if (queue.isEmpty()) {
            notEmpty.await()
        }

        entry = queue.take()
        wrapper.sendEvent(
                userId = entry.userId,
                eventName = entry.eventName,
                timestamp = entry.timestamp,
                level = entry.level,
                data = entry.data
        )
        lock.unlock()
    }

    private fun flush() {
        var tries = 0
        var response: HttpResponseFacade
        do {
            response = wrapper.flush()
            println("DTD_RESPONSE_CODE: ${response.statusCode}")
            println("DTD_RESPONSE_BODY: ${response.getBodyAsString()}")
            println("DTD_RESPONSE_REASON: ${response.statusReason}")
            if (tries > retries) throw RetriesNumberExceededException()
            tries++
        } while (response.statusCode != 200)
    }

    fun sendEvent(userId: String, eventName: String, timestamp: Long = Date().time, level: Int = 0, data: Any) {
        lock.lock()
        queue.put(DtdEntry(userId, eventName, timestamp, level, data))
        if (queue.size == 1) {
            notEmpty.signal()
        }

        lock.unlock()
    }

    companion object {
        private lateinit var instance: DtdConsumerThread
        fun initOrGet(apiKey: String = "", capacity: Int = 100, retries: Int = 15): DtdConsumerThread {
            if (!::instance.isInitialized) {
                instance = DtdConsumerThread(DevtodevAnalyticsWrapper(apiKey), capacity, retries)
                instance.start()
            }
            return instance
        }
    }
}