# Brief Summary

## Redis Client
Java의 Redis Client는 크게 두 가지가 있다.
[Jedis](https://github.com/redis/jedis)와 [Lettuce](https://github.com/lettuce-io/lettuce-core)인데, 최근에는 거의 Lettuce를 사용하고 있다. Lettuce는 내부적으로 Netty를 사용하고 있다. 성능면에서도 Lettuce가 Jedis보다 좋은 성능을 보인다. ([참고 - edis 보다 Lettuce 를 쓰자](https://jojoldu.tistory.com/418))

Spring Boot 2.0 부터는 `spring-boot-starter-data-redis`가 내부적으로 lettuce를 포함하고 있다.

# How to run

## Run Redis using Docker

```shell
docker run --name redis -d -p 6379:6379 redis:alpine
```
