package com.jongmin.exam.api

import com.jongmin.exam.domain.Post
import com.jongmin.exam.domain.PostMemoryRepository
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/posts")
class PostController(
    private val postMemoryRepository: PostMemoryRepository
) {
    private val log = LoggerFactory.getLogger(PostController::class.java)

    @GetMapping("/{id}")
    fun getPostById(@PathVariable id: String): ResponseEntity<Post> {
        log.info("getPostById - $id")

        return postMemoryRepository.find(id)
            ?.let { ResponseEntity.ok(it) }
            ?: run { ResponseEntity.notFound().build() }
    }

    @PostMapping
    fun createPost(): ResponseEntity<Post> {
        val post = Post()
        postMemoryRepository.save(post)

        log.info("createPost - ${post.id}")
        return ResponseEntity.ok(post)
    }

    @DeleteMapping("/{id}")
    fun deletePost(@PathVariable id: String) {
        postMemoryRepository.delete(id)
    }
}
