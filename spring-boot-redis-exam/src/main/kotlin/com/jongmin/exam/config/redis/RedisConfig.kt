package com.jongmin.exam.config.redis

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.validation.annotation.Validated
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Positive

@Configuration
class RedisConfig(
    private val properties: RedisProperties
) {
    @Bean
    fun redisConnectionFactory(): RedisConnectionFactory {
        return LettuceConnectionFactory(properties.host, properties.port)
    }

    @Bean
    fun redisTemplate(): RedisTemplate<Any, Any> {
        return RedisTemplate<Any, Any>()
            .apply { setConnectionFactory(redisConnectionFactory()) }
    }
}

@Validated
@ConstructorBinding
@ConfigurationProperties(prefix = "spring.redis")
data class RedisProperties(
    @field:NotBlank
    val host: String,
    @field:Positive
    val port: Int
)
