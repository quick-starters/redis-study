package com.jongmin.exam.domain

import org.springframework.data.repository.CrudRepository

interface UserRedisRepository : CrudRepository<User, String>
