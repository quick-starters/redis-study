# Brief Summary

## Run Redis using Docker

```shell
docker run --name redis -d -p 6379:6379 redis:alpine
```

## Redis Client

Java의 Redis Client는 크게 두 가지가 있다.
[Jedis](https://github.com/redis/jedis)와 [Lettuce](https://github.com/lettuce-io/lettuce-core)인데, 최근에는 거의
Lettuce를 사용하고 있다. Lettuce는 내부적으로 Netty를 사용하고 있다. 성능면에서도 Lettuce가 Jedis보다 좋은 성능을
보인다. ([참고 - edis 보다 Lettuce 를 쓰자](https://jojoldu.tistory.com/418))

Spring Boot 2.0 부터는 `spring-boot-starter-data-redis`가 내부적으로 lettuce를 포함하고 있다.

## Redis Config

`LettuceConnectionFactory`를 생성한다.

## RedisRepository

Spring Data Redis의 `RedisRepository`를 이용하면 간단하게 Domain Entity를 Redis Hash로 만들 수 있다. 다만 트랜잭션을 지원하지 않기 때문에 만약
트랜잭션을 적용하고 싶다면 `RedisTemplate`을 사용해야 한다.

## RedisTemplate

`RedisTemplate`을 사용하면 특정 Entity 뿐만 아니라 여러가지 원하는 타입을 넣을 수 있다.

## Spring Redis Cache

`@EnableCaching`로 사용가능.

- @Cacheable: 캐시가 있으면 캐시의 정보를 사용하고 없으면 새로 등록한다.
- @CachePut: 항상 캐시에 저장한다.
- @CacheEvict: 캐시를 삭제한다.

기본적으로 serializer로 `JdkSerializationRedisSerializer`를 사용하기 때문에 cache로 사용할 entity에 Serializable을 implements 해야한다. 그러므로 cache 저장 후, entity가 변경되는 경우 deserializer시에 깨질 수가 있다. (에러 발생)

가능하면 별도의 `CacheManager`를 만들어 serde와 TTL 같은 설정들을 해주는 것이 좋을 것 같다.
