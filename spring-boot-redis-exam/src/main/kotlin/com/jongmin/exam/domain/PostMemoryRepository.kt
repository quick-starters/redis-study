package com.jongmin.exam.domain

import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

@CacheConfig(cacheNames = ["post"])
@Component
class PostMemoryRepository {
    private val storage = mutableMapOf<String, Post>()

    fun save(post: Post) {
        storage[post.id] = post
    }

    @Cacheable(key = "#id")
    fun find(id: String): Post? {
        return storage[id]
    }

    @CacheEvict(key = "#id")
    fun delete(id: String) {
        storage.remove(id)
    }
}
