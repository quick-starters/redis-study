package com.jongmin.exam.domain

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.findByIdOrNull

@SpringBootTest
class UserRedisRepositoryTest {

    @Autowired
    private lateinit var sut: UserRedisRepository

    @Autowired
    private lateinit var sut2: User2RedisRepository

    @DisplayName("저장, 조회, 삭제 테스트")
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

    @DisplayName("조회시 저장한 Class가 아닌 다른 Calss로 Deserialize가 가능한지 테스트")
    @Test
    fun test2() {
        val user = User(name = "Kim", age = 30)

        val saveUser = sut.save(user)
        println("saveUser: $saveUser")

        val findUser = sut2.findByIdOrNull(user.id)
        println("findUser: $findUser")
    }
}
