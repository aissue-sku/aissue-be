# AIssue 프로젝트 현황

> 최종 업데이트: 2026년 5월 27일

---

## 프로젝트 한 줄 소개

**AIssue**는 뉴스를 자동으로 수집하고, AI가 신뢰도를 분석하며, 관심 키워드의 트렌드를 실시간으로 보여주는 뉴스 큐레이션 서비스입니다.

---

## 시스템 구조

```
클라이언트 (브라우저 / 앱)
        ↓
  ┌─────────────────────────────────────────────────────┐
  │          게이트웨이  :8080  (진입점 · JWT 검증)         │
  └─────────────────────────────────────────────────────┘
        ↓ Eureka 서비스 디스커버리
  ┌──────────────┬───────────────┬──────────────────────────────────────┬──────────────────┐
  │  인증 서비스  │  사용자 서비스  │        콘텐츠 수집 서비스               │  알림 서비스      │
  │  (auth)     │  (user)       │   (content-collection)               │ (notification)   │
  └──────────────┴───────────────┴──────────────────────────────────────┴──────────────────┘
        ↓               ↓                       ↓                              ↓
     Redis         MySQL                    MySQL                          MySQL
   (토큰 저장)   (aissue_users)         (aissue_content)            (aissue_notifications)
                                              ↓
                                       ┌──────────────┐
                                       │  외부 서비스   │
                                       │ 네이버 API    │
                                       │ RSS 피드      │
                                       │ OpenAI API   │
                                       │ AWS S3       │
                                       └──────────────┘
  ┌─────────────────────┐
  │  인프라              │
  │  Eureka  :8761      │
  │  Config  :8888      │
  └─────────────────────┘
```

### 서비스 간 내부 통신

서비스 간 직접 호출은 `RestTemplate + Eureka LoadBalanced` 방식을 사용합니다.

| 호출 방향 | 엔드포인트 | 용도 |
|---|---|---|
| content-collection → notification | `POST /internal/notifications/match` | 이슈 카드 생성 시 구독 키워드 매칭 알림 |
| user → notification | `POST /internal/notifications/direct` | 포인트 획득 / 사용 알림 |
| auth → user | `GET /internal/users/{username}/credentials` | 로그인 시 사용자 인증 정보 조회 |
| quest(예정) → user | `POST /internal/users/{userId}/points` | 퀘스트 완료 시 포인트 지급 |

---

## 게이트웨이 라우팅

| 경로 패턴 | 대상 서비스 |
|---|---|
| `/api/auths/**` | auth-service |
| `/api/users/**` | user-service |
| `/api/contents/**` | content-collection-service |
| `/api/issues/**` | content-collection-service |
| `/api/subscriptions/**` | content-collection-service |
| `/api/notifications/**` | notification-service |

---

## 개발 완료 기능

### 1. 회원 관리 — `user-service`

| 기능 | 설명 |
|---|---|
| 회원가입 | 아이디 / 비밀번호 / 이름으로 계정 생성 |
| 로그인 | 아이디·비밀번호 인증 후 Access / Refresh 토큰 발급 |
| 로그아웃 | Refresh Token 무효화 |
| 내 정보 조회 | 로그인한 사용자 정보 + 색상 테마 + 보유 포인트 반환 |
| 회원 탈퇴 | DB 레코드 + Redis 토큰 완전 삭제 |
| 토큰 재발급 | Refresh Token으로 Access Token 갱신 |

---

### 2. 캐릭터 커스터마이징 — `user-service`

사용자가 자신만의 캐릭터를 꾸밀 수 있는 기능입니다.  
아이템은 **구매 후 착용** 가능하며, 구매에는 포인트가 사용됩니다.

#### 카테고리별 아이템

| 카테고리 | 필수 여부 | 기본값 | 아이템 목록 |
|---|---|---|---|
| 모자 (HAT) | 선택 | 없음 | 망고 모자 · 베레모 · 곰 후드 · 신사 모자 · 야구 모자 · 헤드폰 |
| 얼굴 (FACE) | 필수 | 기본 | 기본 · 행복 · 장난 · 슬픔 · 화남 · 예민 · 피곤 |
| 옷 (CLOTHES) | 선택 | 없음 | 브라운 셔츠 · 그린 셔츠 · 파자마 · 바지 · 핑크 치마 · 정장 |
| 색상 (COLOR) | 필수 | 아이슈 | 아이슈 · 잔망슈 · 선셋슈 · 레트로슈 · 달콤슈 · 쿨슈 |

- **기본 제공 아이템** (구매 불필요): 기본 얼굴, 아이슈 색상
- 얼굴·색상은 해제 불가 — 다른 아이템으로 교체만 가능
- 모자·옷은 해제 가능

#### API

| Method | 경로 | 설명 |
|---|---|---|
| `GET` | `/api/users/character` | 착용 상태 + 전체 아이템 목록(가격·구매 여부 포함) |
| `POST` | `/api/users/character/purchase` | 아이템 구매 |
| `PUT` | `/api/users/character/equip` | 아이템 착용 |
| `DELETE` | `/api/users/character/{itemType}` | 아이템 해제 (HAT·CLOTHES만) |

---

### 3. 포인트 시스템 — `user-service`

| 구분 | 설명 |
|---|---|
| 잔액 저장 | `users.points` 컬럼에 보관 |
| 획득 | 퀘스트 완료 시 지급 (퀘스트 서비스 예정) |
| 사용 | 캐릭터 아이템 구매 시 차감 |
| 알림 | 획득 / 사용 시 알림 서비스로 포인트 변동 내역 전송 |

---

### 4. 뉴스 자동 수집 — `content-collection-service`

- **네이버 뉴스 API** + **RSS 피드** (연합뉴스, SBS, 경향신문, 한국경제, 뉴시스, 중앙일보)로 **30분마다** 자동 수집
- 중복 기사는 자동 제거

---

### 5. 트렌딩 뉴스 — `content-collection-service`

| 주기 | 내용 |
|---|---|
| 매 시간 | 최근 1시간 내 가장 많이 수집된 뉴스 TOP 10 갱신 |
| 매일 자정 | 하루 기준 트렌딩 뉴스 갱신 + 7일 이전 데이터 자동 정리 |

---

### 6. 급상승 키워드 (Hot Topic) — `content-collection-service`

- 하루 4회 (00시·06시·12시·18시) 실행
- 최근 뉴스에서 평소 대비 급격히 언급 증가한 키워드 최대 10개 추출
- 유사 키워드는 하나로 묶어 중복 제거

---

### 7. AI 이슈 카드 — `content-collection-service`

- 급상승 키워드 기반으로 GPT-4o-mini가 카드 형태로 가공
- 각 카드: 핵심 키워드 + 후킹 문구 + 원문 URL
- 카드 생성 후 구독 키워드 매칭 → 알림 자동 발송

> 예시) `"트럼프"` — *"결국 터졌다, 누구도 예상 못한 반전"*

---

### 8. 뉴스 신뢰도 분석 — `content-collection-service`

| 분석 항목 | 만점 |
|---|---|
| 신뢰성 | 25점 |
| 정확성 | 25점 |
| 편향성 | 20점 |
| 교차 검증 | 20점 |
| 투명성 | 10점 |

- 총점과 함께 판정: **신뢰할 수 있음 / 주의 필요 / 신뢰하기 어려움**
- 분석 이력 사용자별 저장, 반복 요청 시 DB에서 즉시 반환

---

### 9. 키워드 구독 & 알림 — `notification-service`

- 관심 키워드 등록 시 해당 키워드가 이슈 카드에 등장하면 **자동 알림 생성**
- 구독 추가 / 취소 / 목록 조회
- 알림 읽음 처리, 전체 읽음, 읽은 알림 삭제

---

## DB 구조 요약

### aissue_users (user-service)

| 테이블 | 주요 컬럼 |
|---|---|
| `users` | id, username, password, name, role, points |
| `user_character` | user_id (PK), hat, face, clothes, color |
| `user_item` | id, user_id, item, purchased_at |

### aissue_content (content-collection-service)

| 테이블 | 설명 |
|---|---|
| `contents` | 수집된 뉴스 기사 |
| `trending_keyword` | 급상승 키워드 |
| `trending_snapshot` | 트렌딩 뉴스 스냅샷 |
| `issue_card` | AI 생성 이슈 카드 |
| `content_analysis` | 신뢰도 분석 결과 |
| `article_critique` | 기사 비평 결과 |

### aissue_notifications (notification-service)

| 테이블 | 설명 |
|---|---|
| `notifications` | 사용자 알림 (구독 키워드 매칭 + 포인트 변동) |
| `subscriptions` | 사용자 키워드 구독 목록 |

---

## 사용 중인 외부 서비스

| 서비스 | 용도 |
|---|---|
| 네이버 뉴스 API | 뉴스 수집 |
| RSS 피드 (6개 언론사) | 뉴스 수집 |
| OpenAI GPT-4o-mini | 이슈 카드 생성, 신뢰도 분석 |
| AWS S3 | 이슈 카드 이미지 저장 |
| Redis | Access/Refresh Token 저장, 이슈 카드 캐싱 |

---

## 개발 환경 및 인프라

- 모든 서비스는 **Docker 컨테이너**로 패키징
- **Eureka** `:8761` — 서비스 등록 및 디스커버리
- **Config Server** `:8888` — 환경별 설정 중앙 관리 (Git 기반)
- **Gateway** `:8080` — 단일 진입점, JWT 검증, CORS
- 서버 로그는 **Dozzle** `:9999` 에서 실시간 모니터링

---

## 개발 예정 기능

| 기능 | 설명 |
|---|---|
| 퀘스트 시스템 | 특정 활동 달성 시 포인트 지급 |
| 개인화 트렌드 카드 (RAG) | 구독 키워드 기반 맞춤 이슈 카드 제공 |