# 트랜잭션

### 트랜잭션이란?

트랜잭션이란 여러 쿼리를 논리적으로 하나의 작업으로 묶어주는 것입니다.



### 필요성

중고 거래를 예로들어봅시다.

```
구매자 계좌에서 10000원 출금
판매자 계좌에서 10000원 입금
```

구매자의 계좌에서 10000원을 출금 후 판매자의 계좌에 10000원을 넣어주고 물건을 받아오는 간단한 거래입니다.



```sql
1. 
UPDATE accounts
SET balance = balance - 10000	
WHERE user = '구매자';

2. 
UPDATE accounts
SET balance = balance + 10000	
WHERE user = '판매자';
```

위와 같이 데이터베이스에 쿼리를 두 번 날리면 원하는 비지니스 로직을 충족시키게 되죠.



```sql
1. 
UPDATE accounts
SET balance = balance - 10000	
WHERE user = '구매자';

######## 서버 다운 ########

2. 
UPDATE accounts
SET balance = balance + 10000	
WHERE user = '판매자';
```

하지만 위와 같이 서버 1번 쿼리와 2번 쿼리 사이에 서버 다운과 같은 오류가 발생하면 어떻게 될까요?

구매자의 계좌에서는 돈이 빠졌는데, 판매자의 계좌에는 입금이 안될겁니다. 위와 같은 상황을 방지하기 위해 트랜잭션이라는 논리적인 작업 단위로 묶어줍니다. 사용자, 시스템 실수가 있더라도 데이터베이스가 데이터를 안정적으로 보장하게 해주는 것이죠. 



### 커밋과 롤백

트랜잭션은 다음 두 가지 개념을 통해 논리적인 단위의 안정성을 보장합니다.

- 커밋 : 트랜잭션이 잘 처리되었을 때 데이터베이스에 반영
- 롤백 : 트랜잭션이 실패했을 때 기존 데이터로 되돌리기



![image-20211125181541549](/Users/addpage/Library/Application Support/typora-user-images/image-20211125181541549.png)

***커밋***

UPDATE, DELETE, INSERT로 이루어진 트랜잭션이 모두 실행되어 데이터베이스에 반영시키는 것을 커밋이라고 합니다.



![image-20211125181607768](/Users/addpage/Library/Application Support/typora-user-images/image-20211125181607768.png)

***롤백***

UPDATE, DELETE, INSERT로 이루어진 트랜잭션 중간에 결과를 취소하고 DB를 트랜잭션 이전 상태로 되돌리는 것을 롤백이라고 합니다.



## 트랜잭션의 성질

트랜잭션은 안전하게 수행된다는 것을 보장하기 위해서 ACID 라는 성질을 지니고 있습니다.



### ACID

- Atomic (원자성)
  - 트랜잭션은 DB에 모두 반영되거나, 전혀 반영되지 않아야한다.
  - 완료되지 않은 트랜잭션의 중간 상태를 DB에 반영해서는 안된다.
- Consistency (일관성)
  - 트랜잭션 작업 처리 결과는 항상 일관성 있어야한다.
  - 데이터베이스는 항상 일관된 상태로 유지되어야한다.
  - DB에 여러 제약 조건에 맞는 상태를 보장해준다.
    - 마이너스 통장을 허락하지 않는 제약조건이면 트랜잭션 종료
- Isolation (독립성)
  - 둘 이상의 트랜잭션이 동시 실행되고 있을 때 어떤 트랜잭션도 다른 트랜잭션 연산에 끼어들 수 없다.
  - 각각의 트랜잭션은 서로 간섭 없이 독립적으로 이루어져야한다.
  - 구매자의 계좌에서 돈이 빠져나가고 판매자의 계좌에 들어가지 않은 상황을 다른 트랜잭션이 조회하면 안된다.
- Durability (지속성)
  - 트랜잭션이 성공적으로 완료되었으면 결과는 영구히 반영되어야한다.



### ACID의 한계

ACID는 트랜잭션이 이론적으로 보장해야하는 성질이고 실제로는 성능을 위해 성질 보장이 완화되기도 합니다. ACID 중  Isolation(독립성)을 완벽히 보장하려면 동일 데이터에 100개의 연결 접근이 있을 시 이를 순차적으로 처리되어야겠죠. 동시성이 매우 떨어져 시스템이 느려지는 문제가 발생합니다.

동시성을 얻기 위한 한가지 방법으로 트랜잭션의 레벨 수준 설정이 있습니다.



## 트랜잭션 격리 레벨이란?

동시에 여러 트랜잭션이 처리될 때 특정 트랜잭션이 다른 트랜잭션에서 변경하거나 조회하는 데이터를 볼 수 있도록 허용할지 말지를 결정하는 수준을 정하는 단위입니다.

### 트랜잭션 격리 수준

- READ UNCOMMITTED
- READ COMMITTED
- REPEATABLE READ
- SERIALIZABLE

밑으로 갈수록 격리 수준이 높아지지만, 성능이 떨어집니다. 데이터 정합성과 성능이 반비례하므로 케이스에 맞게 잘 선택해야합니다.



#### READ UNCOMMITTED

- 각 트랜잭션에서의 변경 내용이 `COMMIT`이나 `ROLLBACK` 여부에 상관 없이 다른 트랜잭션에서 값을 읽을 수 있습니다.
- 정합성에 문제가 많은 격리 수준이기 때문에 사용하지 않는 것을 권장합니다.
- 아래의 그림과 같이 `Commit`이 되지 않는 상태지만 `Insert`된 값을 다른 트랜잭션에서 읽기 가능합니다.

![No Image](https://nesoy.github.io/assets/posts/img/2019-05-08-21-09-02.png)



**락**

Lock을 걸지 않고 쿼리를 수행하기 때문에 트랜잭션 처리 과정이 외부 트랜잭션에 그대로 노출됩니다.



**문제**

- DIRTY READ
  - 트랜잭션이 작업이 완료되지 않았는데도 다른 트랜잭션에서 볼 수 있게 되는 현상



#### READ COMMITTED

- 커밋이 완료된 트랜잭션의 변경사항만  조회 가능합니다.
- Dirty Read와 같은 현상은 발생하지 않습니다.
- 실제 테이블 값을 가져오는 것이 아니라 Undo 영역에 백업된 레코드에서 값을 가져옵니다.

![No Image](https://nesoy.github.io/assets/posts/img/2019-05-08-21-18-08.png)



**락**

Query에 연관된 Row에 Row Lock을 건뒤 Query를 수행하고, Query 수행이 마치면 해당 Row Lock을 풉니다. 트랜잭션 단위가 아닌 Query 단위로 Lock 동작을 수행하기 때문에 트랜잭션 수행 중에도 외부 트랜잭션 Commit 내용이 반영됩니다.



**문제**

![No Image](https://nesoy.github.io/assets/posts/img/2019-05-08-21-25-41.png)

- Non Repeatable Read
  - `트랜잭션-1`이 Commit한 이후 아직 끝나지 않는 `트랜잭션-2`가 다시 테이블 값을 읽으면 값이 변경됨을 알게됨
  - 하나의 트랜잭션내에서 똑같은 SELECT 쿼리를 실행했을 때는 항상 같은 결과를 가져와야 하는 `REPEATABLE READ`의 정합성에 어긋남
  - 데이터의 정합성은 깨지고, 버그는 찾기 어려워짐



#### REPEATABLE READ

- 트랜잭션이 수행되는 동안 한번 읽었던 Row를 반복해서 읽을경우 언제나 동일한 Data가 나오는 것을 보장해주는 레벨입니다.
- 외부 Transaction에 의해 추가된 **새로운 Row**가 Read 결과에 반영되기 때문에 완전한 Isolation을 보장하지는 못합니다.
- Undo 공간에 백업해두고 실제 레코드 값을 변경합니다.
  - 백업된 데이터는 불필요하다고 판단하는 시점에 주기적으로 삭제
  - Undo에 백업된 레코드가 많아지면 MySQL 서버의 처리 성능이 떨어질 수 있음

![No Image](https://nesoy.github.io/assets/posts/img/2019-05-08-21-52-08.png)



**락**

트랜잭션의 Query에 연관된 모든 테이블의 Row에 Row Lock을 걸고 수행하고, 트랜잭션이 종료될때 Lock을 풉니다. 따라서 트랜잭션에서 Read를 수행한 Row를 외부 트랜잭션에서 변경하지 못합니다. 하지만 Row Lock만 걸기 때문에 외부 Transaction에서 해당 Table에 새로운 Row를 추가 할 수 있습니다.



**문제**

![No Image](https://nesoy.github.io/assets/posts/img/2019-05-08-22-14-18.png)

- Phantom Read 
  - 외부 Transaction에 의해서 새롭게 추가된 Row가 결과에 반영되는 현상
  - 이를 방지하기 위해서는 쓰기 락을 걸어야합니다.



#### SERIALIZABLE

- 가장 단순한 격리 수준이지만 가장 엄격한 격리 수준
- 성능 측면에서는 동시 처리성능이 가장 낮습니다.
- `SERIALIZABLE`에서는 `PHANTOM READ`가 발생하지 않는다. 하지만. 데이터베이스에서 거의 사용되지 않습니다.



다음은 각 격리단계에서 발생할 수 있는 문제들을 정리해놓은 표입니다.

![Isolation levels and locking in relational databases](https://retool.com/blog/content/images/2020/03/Image-2020-01-21-at-5.48.02-PM.png)

*[**트랜잭션 격리 수준별 발생할 수 있는 문제**](https://retool.com/blog/isolation-levels-and-locking-in-relational-databases/)*



## 참고

- [트랜잭션 격리 레벨](https://ssup2.github.io/theory_analysis/DB_Isolation_Level/)