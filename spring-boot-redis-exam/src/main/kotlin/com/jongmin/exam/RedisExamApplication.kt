package com.jongmin.exam

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching

@EnableCaching
@ConfigurationPropertiesScan
@SpringBootApplication
class SpringBootRedisExamApplication

fun main(args: Array<String>) {
    runApplication<SpringBootRedisExamApplication>(*args)
}
