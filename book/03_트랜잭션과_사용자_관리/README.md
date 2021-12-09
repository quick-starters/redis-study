# 03. 트랜잭션과 락

Redis는 하나 이상의 명령으로 구성된 단일 작업 단위로 정의되는 *트랜잭션을* 지원 합니다. Redis 트랜잭션에는 다음과 같은 속성이 있습니다.

- 트랜잭션의 모든 명령은 실행된 순서대로 직렬화되고 실행됩니다. 트랜잭션 중에 Redis는 다른 클라이언트가 실행한 다른 명령에 응답하지 않으므로 **트랜잭션의 명령이 격리**됩니다.
- 레디스는 `Read Uncommitted, Read Committed` 격리 수준을 제공합니다.
- Redis 트랜잭션은 모든 트랜잭션 명령이 단일 작업 단위로 취급된다는 점에서 원자적입니다. 모든 명령이 실행되거나 실행되지 않습니다.
- 명령으로 인해 오류가 발생하면 Redis 서버는 QUEUED 응답 대신 오류를 반환하고 EXEC 명령으로 트랜잭션이 실행될 때 오류를 반환합니다. 이것은 일반적으로 명령에 잘못된 구문이 사용될 때 발생합니다.
- **그러나** 모든 명령이 Redis 서버에서 성공적으로 큐에 대기되고 한 명령 실행에서 오류가 발생하면 다른 모든 명령은 계속 실행됩니다. Redis는 각 명령을 실행한 결과와 함께 대량 응답을 반환합니다.
- Redis 트랜잭션은 대부분의 관계형 데이터베이스에서 구현되는 **롤백을 지원하지 않습니다.**



## 명령어

- MULTI
  - Redis의 트랜잭션을 시작하는 커맨드
  - 트랜잭션을 시작하면 Redis는 이후 커맨드는 바로 실행되지 않고 queue에 쌓입니다.
- EXEC
  - 정상적으로 처리되어 queue에 쌓여있는 명령어를 일괄적으로 실행합니다. 
  - RDBMS의 Commit과 동일합니다.
- DISCARD
  - queue에 쌓여있는 명령어를 실괄적으로 폐기합니다. 
  - RDMS의 Rollback과 동일합니다.
- WATCH
  - Redis에서 Lock을 담당하는 명령어입니다. 
  - 낙관적 락(Optimistic Lock) 기반입니다.
  - **Watch 명령어를 사용하면 이 후 UNWATCH 되기전에는 1번의 EXEC 또는 Transaction 아닌 다른 커맨드만 허용**합니다.



## 트랜잭션 실습

### 초기 데이터

```bash
127.0.0.1:6379> HSET Movie:345 name "Hell"
(integer) 1
127.0.0.1:6379> SET Movie:345:Likes 200
OK
```



### MULTI 트랜잭션 시작

```bash
127.0.0.1:6379> MULTI
OK
127.0.0.1:6379(TX)> HSET Movie:345 copyrightYear 2021
QUEUED
127.0.0.1:6379(TX)> INCR Movie:345:Likes
QUEUED
```

`MULTI` 커맨드를 사용해 트랜잭션을 사용할 수 있습니다. 이후에 들어오는 명령어는 바로 실행되는 것이 아니라 큐에 쌓이게("QUEUED") 됩니다.



### EXEC 트랜잭션 처리

```bash
127.0.0.1:6379(TX)> EXEC
1) (integer) 1
2) (integer) 201
```

 EXEC 커맨드를 통해 큐에 쌓인 쿼리를 일괄적으로 반영합니다.

정상적으로 커맨드가 실행 됬는지 한번 확인해보겠습니다. 트랜잭션 내부의 커맨드가 정상적으로 실행되었기때문에 아래와 같이 GET 했을 때 정상 출력되는 것을 확인할 수 있습니다.

```bash
127.0.0.1:6379> HGET Movie:345 name
"Hell"
127.0.0.1:6379> GET Movie:345:Likes
"201"
```



### DISCARD 롤백

`MULTI` 커맨드를 사용한 후 이용하여 `DISCARD` 명령어를 명시적으로 실행합니다. 이렇게 한다면 큐에 쌓여있던 명령어가 일괄적으로 없어지게 됩니다.

```bash
127.0.0.1:6379> MULTI
OK
127.0.0.1:6379(TX)> INCR Movie:345:Likes
QUEUED
127.0.0.1:6379(TX)> DISCARD
OK
127.0.0.1:6379> GET Movie:345:Likes
"201"
```



### 트랜잭션 중 에러 발생

트랜잭션 중에 두 종류의 커맨드 오류가 발생할 수 있습니다.

1. EXEC 명령어 실행 이전에 큐에 적재하는 도중 실패하는 경우
2. EXEC 명령어 실행 이후 실패하는 경우



#### EXEC 명령어 실행 이전에 큐에 적재하는 도중 실패하는 경우

- 명령어가 문법적으로 잘못된 경우
- 메모리 부족과 같은 심각한 사태인 경우

Command 의 응답 값으로 `QUEUED` 가 온 경우에는 성공적으로 처리되었다고 보면 됩니다. 그렇지 않은 경우에는 Error 를 응답 값으로 받습니다. 트랜잭션 중 에러가 발생하게 되면 레디스 2.6.5부터는 자동으로 DISCARD 처리합니다.

```bash
127.0.0.1:6379> MULTI
OK
127.0.0.1:6379(TX)> INCR Movie:345:Likes
QUEUED

# 정의되지 않은 명령어 입력 시 ERR가 바로 떨어집니다.
127.0.0.1:6379(TX)> DD HDK
(error) ERR unknown command `DD`, with args beginning with: `HDK`,

# EXEC 호출 시 트랜잭션이 버려졌다는 문구가 나옵니다.
127.0.0.1:6379(TX)> EXEC
(error) EXECABORT Transaction discarded because of previous errors.

# 데이터도 변경되지 않았습니다.
127.0.0.1:6379> GET Movie:345:Likes
"201"
```



#### EXEC 명령어 실행 이후 실패하는 경우

- 잘못된 명령어 호출
  - ex) 문자열 타입에 해쉬 타입 명령어를 실행한 경우

`EXEC` 이후 발생한 오류는 특별한 방법으로 처리되지 않습니다. 트랜잭션 중에 일부 명령이 실패하더라도 다른 모든 명령들이 실행됩니다.

```bash
127.0.0.1:6379> SET name "Evan Hwang"
OK
127.0.0.1:6379> MULTI
OK

# SET 타입에 HASH 타입의 HSET 명령어 사용
127.0.0.1:6379(TX)> HSET name "Evan Hwang" 30
QUEUED
127.0.0.1:6379(TX)> SET name "Jongmin Kim"
QUEUED

# EXEC 후 발생한 오류로 2번 명령어에 대해서는 실행됨
127.0.0.1:6379(TX)> EXEC
1) (error) WRONGTYPE Operation against a key holding the wrong kind of value
2) OK

# 2번 명령어가 잘 실행된 것을 확인할 수 있다.
127.0.0.1:6379> GET name
"Jongmin Kim"
```



**Redis는 왜 이런 트랜잭션 방법을 택했을까요?** 

Relational 데이터베이스에 대한 배경 지식이 있는 경우, Redis 의 Transaction 처리가 Command 가 일부 실패할 수 있지만 롤백하지 않고 나머지 트랜잭션을 실행한다는 점이 이상해 보일 수 있습니다.

그러나 이러한 Redis Transaction 처리 방법이 장점이 되기도 합니다.

Redis 명령은 잘못된 구문으로 호출된 경우에만 실패 할 수 있으며 (Queue 에서 문제가 감지되지 않음) 또는 잘못된 데이터 유형에 대한 요청이 실패할 수 있다. 즉, 실패한 명령은 프로그래밍 오류의 결과이며, 대부분 개발 단계에서 발견될 수 있는 종류의 오류이므로 Production 에 발생할 일은 거의 발생하지 않습니다. Redis는 rollback 할 필요가 없기 때문에 내부적으로 단순화되고 빨라졌습니다. Redis 의 Transaction 처리에 대한 논쟁은 일반적인 사용자가 이러한 동작 방식을 이해하지 못하고 있을 경우 버그가 발생할 수 있다는 점이지만, 일반적으로 rollback 이 프로그래밍 오류로부터 보호할 수는 없다. 예를 들어 Command 가 특정 key 를 1에서 2로 증가시키거나 잘못된 key 를 증가시키면 rollback mechanism 이 도움이 되지 않는다. 누구도 프로그래머의 실수를 막을 수 없으며, Redis 명령이 실패하는 데 필요한 오류의 종류가 프로덕션 환경에 들어가기가 쉽지 않기 때문에 오류에 대한 롤백을 지원하지 않는 보다 간단하고 빠른 방법을 선택했다고 합니다.



## 락 실습

`WATCH` 는 Redis 트랜잭션에 CAS(Check-and-Set) 동작을 제공하는 데 사용됩니다.

`WATCH` 된 키는 변경 사항을 감지하기 위해 모니터 됩니다. `EXEC` 명령 전에 하나 이상의 감시 키가 수정되면 전체 트랜잭션이 중단되고 `EXEC` 는 트랜잭션이 실패했음을 알리기 위해 Null 응답을 반환합니다.

```bash
# Redis Client1에서 Movie:345:Likes 키에 락을 겁니다.
redis1> WATCH Movie:345:Likes
OK
redis1> MULTI
OK
redis1(TX)> INCR Movie:345:Likes
QUEUED

# Redis Client2에서 해당 값을 증가시킵니다.
redis1> INCR Movie:345:Likes
(integer) 202

# Redis Client1에서 EXEC 명령어 사용 시 Null 응답이 반환됩니다.
redis1(TX)> EXEC
(nil)
redis1> GET Movie:345:Likes
"202"
```

`EXEC`가 호출되면 트랜잭션이 중단되었는지 여부에 관계없이 모든 키가 `UNWATCH` 가 됩니다. 또한 Client connection 이 닫히면 모든 키가 `UNWATCH`가 됩니다. 모든 `WATCH` 된 키를 해제하기 위해 인수없이 `UNWATCH` 명령을 사용할 수도 있습니다.



## 스크립트를 통한 트랜잭션

Redis script 는 정의상 Transaction 방식이므로 Redis Transaction 으로 수행 할 수 있는 모든 작업을 Script 로 수행 할 수 있습니다. 일반적으로 Script 는 더 간단하고 빠릅니다.

이 중복( Script 와 Transaction ) 은 이전에 Transaction 이 존재하였고, Scripting 이 Redis 2.6 에서 도입되었기 때문에 어쩔 수 없이 발생하게 되었다고 합니다.

 그러나 우리는 짧은 시간안에 Transaction 지원을 제거하지는 않을 것이라고 합니다. Redis Scripting 을 사용하지 않고도 경쟁 조건을 피하는 것이 여전히 가능하며 Scripting 을 이용하는 것보다 Transaction 을 이용하는 것이 복잡성이 최소화되기 때문입니다.

하지만, 가까운 시일 내에 모든 Redis 유저들이 Scripting 을 기반으로 Race condition 문제를 해결하고 있음을 확인하는 순간 레디스는 Transaction 기능을 제거할 수도 있다고 하네요.



## 참고

- [Redis.io](https://redis.io/topics/transactions)
- [Spring Data Redis](https://docs.spring.io/spring-data/data-redis/docs/current/reference/html/#tx)
- [jjeda blog](https://jjeda.tistory.com/13)
- [you dont need transaction rollbacks in redis](https://redis.com/blog/you-dont-need-transaction-rollbacks-in-redis/)

