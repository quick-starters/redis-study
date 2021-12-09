## Redis 성능 튜닝

시작하기 전에 다음 파일로 redis 컨테이너를 띄우자.

```yml
version: "3.8"

services:
  redis:
    image: redis
    container_name: redis_local
    privileged: true
    restart: unless-stopped
    ports:
      - 6379:6379
    volumes:
      - /Users/addpage/DEV/Docker/data/redis:/data
      - /Users/addpage/DEV/Docker/data/redis/redis.conf:/usr/local/etc/redis/redis.conf
    command: redis-server /usr/local/etc/redis/redis.conf


```

*테스트를 위한 compose 파일*



## 성능 튜닝 방법론

성능 튜닝 단계는 다음과 같다.

1. 준비 단계
   - 성능 지연 이슈에 대한 불만사항을 파악하고 레디스 서버의 성능 지연 문제인지 파악을 해야한다. 객관적 분석을 위해서는 레디스 서버에서 제공하는 튜닝 툴을 활용하자.

2. 튜닝/분석 단계
   - 실제 튜닝을 진행하는 단계. 서버 튜닝, 쿼리 튜닝, 디자인 튜닝 순으로 진행하는 것이 효과적이다.

3. 결과 단계
   - 튜닝 전 수집된 분석 결과와 튜닝 후 분석 결과를 비교하여 얼마나 개선되었고 성능 목표에 도달했는지 여부를 판단한다.



## 성능 튜닝 포인트

1. 시스템 튜닝
   - 레디스는 인메모리 기반 NoSQL이다. 시스템 환경이 성능에 미치는 영향이 크다
   - 메모리 크기, CPU 개수, 시스템 환경 설정
     - transparent huge page
     - somaxconn
     - system process 개수
     - open file 개수
     - block size 등
   - 이와 같이 시스템 환경이 레디스 서버에 미치는 영향에 의해 성능 지연문제가 발생하는데 이에 대응하는 것이 시스템 튜닝
   - 시스템 튜닝이 차지하는 비율이 상당히 높다.
2. 쿼리 튜닝
   - 관계형 DB의 쿼리 튜닝에 비해 중요도가 조금 떨어진다.
   - 지연 문제를 유발하는 문장
     - 쿼리 문장 비 최적화
     - 인덱스의 생성 여부
     - 잘못된 인덱스 타입
3. 서버 튜닝
   - 레디스 서버 아키텍처는 메모리 영역, 프로세스 영역, 파일 영역으로 구성되어 있다.
   - 각 영역이 최적화된 상태로 설치, 구축되어야하는데 구축된 비지니스 환경, 설계된 데이터 저장 구조에 최적화되지 못한 경우 성능 지연 문제 발생
   - 이와 관련된 성능 튜닝을 서버 튜닝이라고 한다.
4. 디자인 튜닝
   - 데이터가 저장되는 오브젝트의 논리적 설계(테이블 구조, 인덱스 구조) 상에 구조적 문제로 발생하는 성능 튜닝 영역



## 시스템 튜닝

1. 디스크 블록 사이즈 (read ahead 향상)

   - Redis 서버 설치 전 가장 먼저 고려해야함. 디스크 장치의 블록 사이즈를 결정해야함. 포맷되는 디스크 블록 사이즌느 향후 레디스 서버 전용 메모리 영역의 블록 사이즈로 결졍된다. aof, rdb 파일의 블록 사이즈를 결정하는 기준이 됨

   - 운영체제에 따라 다르지만  8k 또는 16k 이상을 권장

   - 많은 사용자가 동시에 서버 접속한 후 입력, 수정, 삭제, 조회 위주 작업하면 기본값보다 한단계 낮은  4kb 권장

   - 배치, 소수의 사용자가 조회 위주면 16kb 권장

   - 블록 사이즈는 포맷 시점에 한번 결정되면 향후 변경할 수 없다.



2. NUMA & Transparent Huge Pages (THP) 설정 해제

   - 인메모리 기반 레디스 서버는 메모리 영역에 대한 할당과 운영, 관리와 관련된 다양한 메커니즘을 자체적으로 제공

   - 리눅스 서버도 자체적으로 NUMA(Non-Uniform Memory Access)와 THP를 제공. 이러한 메커니즘 위에서 레디서 서버의 메모리 운영 메커니즘은 정상 작동 불가하다. 이로 인해 성능 지연이 발생한다. 설정 해제를 권장

     ```sh
     # cat /sys/kernel/mm/transparent_hugepage/enabled
     [always] madvise never
     
     # echo never > /sys/kernel/mm/transparent_hugepage/enabled
     # cat /sys/kernel/mm/transparent_hugepage/enabled
     always madvise [never]
     
     # vi /etc/rc.local
     echo never > /sys/kernel/mm/transparent_hugepage/enabled
     ```



3. Client keepalive Time 설정(Default: 7200)
   - vi /etc/sysctl.conf:
     - net.ipv4.tcp_keepalive_time = 7200
       - 클라이언트가 레디스 서버에 접속할 때 대기시간이 발생한다. 일정 시간 동안 작업을 하지 않더라도 세션을 유지시킴으로서 재접속을 피하고 대기시간을 줄인다.



4. Key Expire를 통한 대기 시간 최소화

   - 오랫동안 참조되지 않는 Key들은 메모리에 지속적으로 저장되어 메모리 효율성을 저하시키고 메모리 영역 크기가 모자를 때 대기 시간을 증가시키는 원인

   - Expire 방법
     1. LazyFree 파라미터를 이용하여 더 이상 참조되지 않는 키를 메모리로부터 제거
        1. 레디스 서버가 내장하고있는 ACTIVE_EXPIRE_CYCLE_LOOKUPS_PER_LOOP 기능을 이용해 100ms 마다 만료된  키를 자동 삭제해주는 방법 기본값은 20인데 초당 200개로 바꿔주는 걸 권장



5. 최적화 System 환경 설정 파일

   - 서버 시스템 환경 설정 고려사항

     ```sh
     # vi /etc/sysctl.conf
     vm.overcommit_memory = 1
     
     # sysctl -a | grep somaxconn
     net.core.somaxconn = 128
     
     # sysctl -w net.core.somaxconn=65535
     net.core.somaxconn = 65535
     
     # cat /proc/sys/net/core/somaxconn
     65535
     
     # vi /etc/sysctl.conf
     net.core.somaxconn = 65535
     
     # Open File 설정
     # vi /etc/security/limits.conf
     redis soft nofile 65536
     redis hard nofile 65536
     redis soft nproc 131072
     redis hard nproc 131072
     ```

     

### 대기시간 모니터링

Redis 2.8.13 버전부터 서버에서 발생하는 다양한 문제로 인한 성능 지연 상태에 대한 대기시간을 분석할 수 있도록 기능을 제공한다. 운영체제 커널과 가상화 시스템을 운영하면 HyperVisor에서 발생하는 대기시간이 있고 Redis 서버를 실행했을 때 발생하는 대기시간도 있는데 이 둘 사이는 매우 밀접한 관련이 있다. 일반적으로 HyperVisor 대기시간이 성능 지연을 많이 유발하기 때문에 이를 모니터링해야한다.

1. OS 커널, HyperVisor에서 발생하는 고유 대기시간 모니터링

```bash
# 100초 내에 발생하는 Latency
root@4c8429c02862:/data# redis-cli --intrinsic-latency 100
Max latency so far: 4 microseconds.
Max latency so far: 5 microseconds.
Max latency so far: 43 microseconds.
Max latency so far: 74 microseconds.
Max latency so far: 105 microseconds.
Max latency so far: 125 microseconds.
Max latency so far: 161 microseconds.
Max latency so far: 435 microseconds.
Max latency so far: 478 microseconds.
Max latency so far: 809 microseconds.

12504578 total runs (avg latency: 7.9971 microseconds / 7997.07 nanoseconds per run).
Worst run took 101x longer than the average latency. 
```

이러한 방법으로 대기시간 수집 및 분석을 할 수 있는데, 문제가 발생했을 때와 비교하기 위해 시스템이 정상적으로 잘 수행되는 시점에 하는게 좋다. 최소 3번이상 하루 중 트래픽이 가장 많을 때 수행하는 것을 권장한다.



## Slow Query 튜닝

SLOWLOG 명령어를 이용해 성능 지연 문제가 발생한 Query를 수집하여 볼 수 있다.

1. 수집 및 분석

2. 2.2.12 버전부터 사용자가 실행한 쿼리 중 성능 지연 문제가 발생한 문장을 자동으로 수집한 후 제공한다.

   ```bash
   redis> set 317260 JMJOO
   OK
   redis> get 100
   (nil)
   redis> slowlog ?
   (error) ERR Unknown subcommand or wrong number of arguments for '?'. Try SLOWLOG HELP.
   redis> slowlog get 2
   1) 1) (integer) 0						# Slow Query ID
      2) (integer) 1638766275	# 운영체제 Timestamp
      3) (integer) 10089				# 실행 시간(ms)
      4) 1) "info"							# 실행된 명령어 배열
      5) "172.17.0.1:55298"		# 클라이언트 IP 주소 및 포트 (CLIENT SETNAME으로 설정한 경우)
      6) ""
      
   # 현재 저장되어 있는 slow 쿼리 개수
   redis> slowlog len
   (integer) 1
   
   # 저장되어 있느 모든 slow query를 초기화
   redis> slowlog reset
   OK
   ```



### 논리적 데이터베이스 설계

0~17개까지 데이터베이스를 생성 가능하다(자동 생성되어있음). 여러개 생성함으로서 DB 관리, 백업과 복구, 성능 문제에 대응이 가능하며 다음과 같은 장점이 생긴다.

- 데이터의 안전한 저장 및 관리
- 분산 데이터베이스 락을 지원



## 서버 튜닝

튜닝 핵심 중 하나. 레디스 서버를 설치하면 메모리 영역, 프로세스 영역, 파일 영역으로 구성되는 기본 아키텍처가 생성된다. 이러한 아키텍처가 최적화 되지 않은 상황에선 Redis 서버의 성능을 기대하기 어렵다.



### 스와핑 모니터링

레디스 서버의 사용 가능한 메모리가 부족하면 메모리에 저장된 데이터 일부를 디스크로 저장하거나 디스크에 저장된 데이터를 메모리로 적재한다. 이를 스와핑이라고 한다. 스와핑이 빈번하게 발생하면 성능 지연 문제가 발생한다.

- 데이터 셋 일부가 클라이언트에 의해 더 이상 참조되지 않은 유휴상태 일 때 OS 커널에 의해 스와핑 될 수 있다.
- 레디스 프로세스 일부는 aof, rdb파일을 디스크에 저장하게 되는데 스와핑이 발생 할 수 있다.

```bash
root@249891b4ad52:/proc/1# cat smaps | grep 'Swap:'
Swap:                  0 kB
Swap:                  0 kB
Swap:                  0 kB
Swap:                  0 kB
Size:                  8192 kB # 스왑 발생
Swap:                  0 kB
Swap:                  0 kB
```

*Redis 서버에서 확인 방법*



과도한 스와핑 방지 방법

1. redis 인스턴스 크기를 충분히 할당

   ```bash
   $vi redis.conf
   maxmemory 10000000000 # 레디스 인스턴스 크기를 늘려줌
   ```

   *[redis.conf](https://redis.io/topics/config)*

   - redis 서버가 시스템에 독립적으로 설치 운영된다면 전체 RAM의 90% 권장
   - 다양한 SW가 함께 설치되어 있다면 비중에 따라 비율 조정
   - redis 서버가 설치된 시스템에는 다른 서비스 같이 돌리지말자

2. 빅데이터 정렬(sorting) 작업이 빈번한 테이블이나 데이터 연산이 많은 테이블의 클라이언트들을 새로운 데이터베이스를 보게 스왑하자

   ```bash
   > swap 0 1 # 데이터베이스 0에 연결된 모든 클라이언트가 1을 바라보도록 함
   ```

   

### AOF 파일에서 발생하는 디스크 IO

- 레디스 서버는 기본적으로 인 메모리 아키텍처지만, 필요에 따라 AOF, RDB 파일로 데이터를 디스크에 저장할 수 있다. 파일 기반  DBMS와는 아키텍처와 운영 기술이 다르기 때문에 방대한 데이터를 디스크에 저장 관리하는데 주의해야한다.

- 데이터 저장을 위해 AOF를 쓸 때, 빅데이터 환경에서는 과도한 쓰기가 발생하여 성능 지연이 생길 수 있다. 이를 피하기 위해 관련 [파라미터](http://redisgate.kr/redis/configuration/param_appendonly.php)를 적절히 설정해야한다.

  ```
  appendonly                  yes
  appendfilename              "appendonly.aof"
  auto-aof-rewrite-percentage	100
  auto-aof-rewrite-min-size	  64mb
  appendfsync			            no
  no-appendfsync-on-rewrite	  no
  ```



### Scale Out

하나의 Redis 서버로 더 이상 성능을 기대할 수 없을 때 새로운 분산 서버를 추가 할당하자. 



### 손상된 메모리 검증

Redis 서버의 성능 지연 문제를 유발시키는 원인 중 하나는 RAM 장치의 손상으로 인한 예기치 못한 경우도 있다. redis-server.exe 에서 제공하는 -test-memory 옵션절을 사용해 메모리 손상을 확인할 수 있다.

```bash
root@249891b4ad52:/proc/1# redis-server --test-memory 2048
Addressing test [1]
AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAKilled..................................
```



### GDB(Gnu Debugger) 

레디스 서버 테스트, 디버그 용으로 사용할 수 있는 유틸리티이다. 자세한 사용법은 [링크](http://redisgate.kr/redis/server/debug.php) 참고