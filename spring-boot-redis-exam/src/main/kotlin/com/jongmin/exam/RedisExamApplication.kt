package com.jongmin.exam

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@ConfigurationPropertiesScan
@SpringBootApplication
class SpringBootRedisExamApplication

fun main(args: Array<String>) {
    runApplication<SpringBootRedisExamApplication>(*args)
}
