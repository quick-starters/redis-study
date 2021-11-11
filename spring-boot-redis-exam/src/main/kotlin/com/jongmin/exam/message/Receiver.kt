package com.jongmin.exam.message

import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicInteger

class Receiver {
    private val log = LoggerFactory.getLogger(Receiver::class.java)

    private val counter = AtomicInteger()

    fun receiveMessage(message: String) {
        log.info("Received <$message>")
        counter.incrementAndGet()
    }
}
