package com.jongmin.exam.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.RedisTemplate
import java.time.Duration

@SpringBootTest
class RedisTemplateTest {

    @Autowired
    private lateinit var sut: RedisTemplate<String, String>

    @Test
    fun testString() {
        val ops = sut.opsForValue()
        val key = "stringKey"
        val value = "hello"

        ops.set(key, value, Duration.ofSeconds(60L))

        val result = ops.get(key)
        assertThat(result).isEqualTo(value)
    }

    @Test
    fun testSet() {
        val ops = sut.opsForSet()
        val key = "setKey"

        ops.add(key, "h", "e", "l", "l", "o")

        val result = ops.members(key)
        val size = ops.size(key)
        assertThat(result).containsOnly("h", "e", "l", "o")
        assertThat(size).isEqualTo(4)
    }

    @Test
    fun testHash() {
        val ops = sut.opsForHash<Any, Any>()
        val key = "hashKey"
        val hKey = "hello"
        val hValue = "world"

        ops.put(key, hKey, hValue)

        val value = ops.get(key, hKey)
        assertThat(value).isEqualTo(hValue)

        val entries = ops.entries(key)
        assertThat(entries.keys).containsExactly(hKey)
        assertThat(entries.values).containsExactly(hValue)

        val size = ops.size(key)
        assertThat(size).isEqualTo(entries.size.toLong())
    }
}
