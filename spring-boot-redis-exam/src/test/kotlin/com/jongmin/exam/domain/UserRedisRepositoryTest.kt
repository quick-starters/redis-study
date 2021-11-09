package com.jongmin.exam.domain

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.findByIdOrNull

@SpringBootTest
class UserRedisRepositoryTest {

    @Autowired
    private lateinit var sut: UserRedisRepository

    @Test
    fun test() {
        val user = User(name = "Kim", age = 30)

        val saveUser = sut.save(user)
        println("saveUser: $saveUser")

        val findUser = sut.findByIdOrNull(user.id)
        println("findUser: $findUser")

        /**
         * 만료된(expired) 데이터까지 포함
         * 내부적으로 Sets을 다루는 SCARD 명령어를 사용
         */
        val userCount = sut.count()
        println("user count: $userCount")

        sut.delete(findUser!!)
    }
}
