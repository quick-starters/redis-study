package com.jongmin.exam.domain

import java.io.Serializable
import java.util.UUID

data class Post(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "name-$id",
    val content: String = "content-$id"
) : Serializable
