# MONSTER HOUSE — Backend

촬영 예약 · 한일 이중언어 미디어 사이트의 API 서버입니다.
조직 소개와 화면은 [MonsterHouseStudio](https://github.com/MonsterHouseStudio) 를 보세요.

Java 21 · Spring Boot 3.3 · JPA + QueryDSL · MySQL 8 · Flyway · Spring Security(JWT) · ShedLock

---

## 실행

MySQL 8 이 필요합니다. `docker-compose.yml` 로 띄울 수 있습니다.

```bash
docker compose up -d mysql
./gradlew bootRun --args='--spring.profiles.active=local'
```

| 포트 | 용도 |
|---|---|
| 8080 | API |
| 8080/actuator/health | 헬스체크 |

포트가 물려 있으면 `--server.port=18100` 처럼 덮어쓰면 됩니다.

```bash
./gradlew test
```

테스트는 **Testcontainers 로 실제 MySQL 8** 을 띄웁니다. H2 를 쓰지 않는 이유는
아래 예약 동시성 때문입니다 — 갭 락과 트랜잭션 격리가 재현되지 않으면 그 테스트가 무의미합니다.
Docker 가 떠 있어야 합니다.

---

## 도메인

```
com.monsterhouse
├── booking        예약 · 슬롯 · 영업시간 · 동시성
├── content        상품 · 갤러리 · 시합 일정 · 포스트 (+ 번역)
├── inquiry        통역/영상 문의
├── admin          관리자 인증 · 계정
├── notification   메일 · LINE 발송과 재시도
├── storage        이미지 · 영상 업로드 파이프라인
└── common         설정 · 예외 · 응답 규격 · 개인정보 파기
```

REST 엔드포인트 73개 · 테이블 18개 · 테스트 53개.

---

## 알아둘 것

### 1. 예약 겹침은 3층으로 막습니다

촬영팀이 하나라 같은 시간대에 두 건을 받을 수 없습니다.
`UNIQUE` 하나로는 "겹친다" 를 표현할 수 없어서 세 층으로 나눴습니다.

| 층 | 수단 | 코드 |
|---|---|---|
| 1 | `booking_day_lock` 행에 `SELECT … FOR UPDATE` | `BookingDayLockManager` |
| 2 | 겹침 쿼리 `start < :end AND end > :start` | `BookingRepository#existsOverlap` |
| 3 | `UNIQUE(slot_key)` — 취소 시 NULL | `Booking` |

**2층 조건에 등호가 없는 것이 중요합니다.** 10:00~11:30 과 11:30~13:00 은 겹치지 않습니다.
`<=` 로 바꾸면 붙어 있는 두 건이 충돌로 판정되어 하루 수용량이 절반이 됩니다.

**1층은 반드시 별도 트랜잭션에서 먼저 커밋해야 합니다.** InnoDB 는 존재하지 않는 행에
`FOR UPDATE` 를 걸면 행이 아니라 갭을 잠급니다. 갭 락끼리는 호환되어 아무도 안 막히다가
INSERT 시점에 insert-intention 락과 충돌해 데드락이 납니다.
`BookingDayLockManager` 의 주석에 그 실화가 적혀 있습니다.

### 2. 격리수준이 `READ_COMMITTED` 인 이유

기본값인 `REPEATABLE READ` 에서는 일반 SELECT 가 트랜잭션 첫 읽기 시점의 스냅샷을 봅니다.
즉 **락을 잡고도 방금 커밋된 예약이 안 보입니다.** 락은 순서를 세워주지만 가시성은 별개입니다.

### 3. 이중언어는 두 층위입니다

- **UI 문자열** — `messages_ko/ja.properties` (`MessageSource`)
- **콘텐츠** — `post` / `post_translation` 처럼 번역 테이블 분리

폴백 정책이 도메인마다 다릅니다. 콘텐츠는 번역이 없으면 그 언어 목록에서 **제외**하고,
상품·가격은 한국어로 **폴백**합니다 — 번역이 없다고 예약을 막으면 매출이 사라지니까요.

> ⚠️ `messages_ja.properties` 는 아직 **기계 번역 초안**입니다.
> 파일 첫 줄에 적어뒀듯이 릴리스 전 일본어 네이티브 감수가 필요합니다.

### 4. 알림은 예약의 조건이 아니라 결과입니다

메일·LINE 발송이 실패해도 예약은 성립해야 합니다. 그래서 보내기 **전에** 아웃박스에 기록하고,
실패하면 1 → 2 → 4 → 8분 간격으로 5회까지 재시도합니다(`OutboxDispatcher`).
발송이 터져도 "보내려 했다" 는 사실이 남아야 유실을 찾을 수 있습니다.

여러 인스턴스를 띄우면 `@Scheduled` 가 서버 수만큼 중복 실행되므로 ShedLock 으로 묶었습니다.
개인정보 파기 배치가 중복으로 돌면 곤란합니다.

### 5. 마이그레이션

`src/main/resources/db/migration` 의 Flyway 스크립트가 유일한 스키마 정의입니다.
`ddl-auto` 는 운영에서 `validate` 입니다.

`FlywayMigrationTest` 가 **엔티티와 마이그레이션의 정합성**을 검사합니다.
엔티티만 추가하고 마이그레이션을 빠뜨리면 이 테스트가 `missing table [...]` 로 잡아냅니다.
실제로 배너 테이블을 추가한 다음 날 이 테스트가 잡았습니다.

---

## 관련

- 프론트엔드 — [MonsterHouseStudio/FE](https://github.com/MonsterHouseStudio/FE)
- 같은 겹침 판정을 Dropwizard 로 이식한 저장소 — [MonsterHouseStudio/wiz](https://github.com/MonsterHouseStudio/wiz)
