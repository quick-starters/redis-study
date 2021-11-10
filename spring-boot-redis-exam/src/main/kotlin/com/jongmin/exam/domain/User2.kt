package com.jongmin.exam.domain

import org.springframework.data.annotation.Id
import org.springframework.data.redis.core.RedisHash
import java.util.UUID

@RedisHash(value = "user", timeToLive = 120L)
data class User2(
    @Id
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val age: Int
)
