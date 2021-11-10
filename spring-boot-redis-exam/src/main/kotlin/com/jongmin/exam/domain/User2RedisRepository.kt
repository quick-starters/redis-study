package com.jongmin.exam.domain

import org.springframework.data.repository.CrudRepository

interface User2RedisRepository : CrudRepository<User2, String>
