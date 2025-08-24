# HexaPass-12-ADR — 개념정리 (리팩토링 버전)

## 1) 정의

**ADR(Architecture Decision Record)** 는 중요한 설계 결정을 **작고, 자주, 추적 가능**하게 기록하는 문서 포맷이다. 변경의 배경(Context), 결정(Decision), 결과(Consequences)를 남겨 미래의 자신과 팀이 이유를 이해할 수 있게 한다.

---

## 2) 왜 필요한가 (장점)

1. 의사결정의 **투명성**과 **역사성** 보존 → 온보딩/감사/회귀 분석 용이.
2. 의사결정의 **맥락**과 **대안 비교**가 남아 재논의 비용↓.
3. 큰 설계 문서 대신 **작은 단위**로 빠르게 문서화.

---

## 3) 주의점 (단점)

* 문서화 비용 발생, 방치 시 품질 저하.
* 사후 기록만 하면 근거가 부정확할 수 있음 → 가능하면 **결정 직전/직후**에 작성.

---

## 4) 기본 템플릿

```md
# ADR-###: <결정 주제>
- **Status**: proposed | accepted | deprecated | superseded by ADR-###
- **Date**: 2025-08-24

## Context
문제가 된 배경, 제약, 요구사항.

## Decision
무엇을, 왜 결정했는지. (도메인 언어 사용)

## Consequences
긍정/부정 효과, 트레이드오프, 후속 작업(To-do).

## Alternatives Considered
고려했던 대안들과 포기 이유.

## Links
PR/이슈/문서 링크.
```

---

## 5) HexaPass 예시

### 5.1 ADR-017: 예약 일관성 경계

```
Status: accepted
Date: 2025-08-24

Context
- (resourceId, timeRange) 충돌이 잦으며, 결제와 묶인 상태 전이 필요.

Decision
- `Reservation`을 애그리게잇 루트로 하여, 예약 생성/취소/완료 전이를 단일 트랜잭션으로 처리.
- 가용성 관리(`Availability`)는 낙관적 락 + 유니크 인덱스로 가드.

Consequences
+ 일관성 보장, 모델 단순
- 쓰기 경합 시 재시도 필요

Alternatives
- 큐 직렬화: 지연 증가로 거부
- 전역 락: 확장성 한계로 거부

Links
- PR #214, 설계 스케치 /docs/diagrams/consistency.md
```

### 5.2 ADR-023: 결제 포트 교체 전략

```
Status: accepted
Date: 2025-08-24

Context
- 외부 PG 벤더를 상황에 따라 교체 가능해야 함.

Decision
- `PaymentPort` 포트 + 어댑터 구조 채택.
- 인메모리/샌드박스/실결제 3종 어댑터와 **계약 테스트** 의무화.

Consequences
+ 테스트 용이, 교체 비용↓
- 초기 인터페이스 설계 비용↑

Alternatives
- SDK 직접 사용: 도메인 오염/테스트 어려움

Links
- PR #237, Pact 계약 테스트 /tests/contracts/payment
```

---

## 6) 작성 팁

* **짧게, 자주**: PR과 함께 5\~10분 내 작성.
* **상태 전이 관리**: proposed → accepted → superseded 흐름 유지.
* **도메인 언어 사용**: "예약", "멤버십" 같은 용어를 그대로.
* **날짜/버전/링크**는 반드시.

---

## 7) 체크리스트

✅ 결정 시점과 맥락이 명확한가?
✅ 대안과 트레이드오프가 기술되었는가?
✅ 후속 작업이 정의되었는가?
✅ ADR 간 링크/상태 관리가 일관적인가?

---

## 8) 학습 과제

1. `Reservation` 일관성 경계 ADR을 작성하고, 대안(큐 직렬화/전역 락) 비교 표를 포함하라.
2. 외부 결제 벤더 교체 ADR을 작성하고, 계약 테스트 지침을 첨부하라.
3. 기존 의사결정 중 변경된 내용이 있으면 `superseded` 상태를 활용해 연결 고리를 만든다.

---

📌 이 문서는 HexaPass에서 ADR을 **실전 문서 도구**로 활용하기 위한 템플릿/예시/체크리스트를 제공한다.
