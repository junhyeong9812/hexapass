# Resource.java - 상세 주석 및 설명

## 클래스 개요
`Resource`는 예약 가능한 리소스(시설, 장비, 서비스 등)를 나타내는 **엔티티(Entity)**입니다.
운영 스케줄, 수용 인원, 부가 기능 등을 관리하며, `resourceId`를 기준으로 동일성을 판단합니다.

## 왜 이런 클래스가 필요한가?
1. **리소스별 운영 정책**: 요일별 다른 운영시간, 수용인원 제한 등
2. **예약 가능성 판단**: 운영시간, 수용인원을 고려한 예약 승인
3. **리소스 메타데이터**: 위치, 부가시설, 설명 등 상세 정보 관리
4. **확장성**: 새로운 리소스 타입과 정책을 쉽게 추가

## 상세 주석이 추가된 코드

```java
package com.hexapass.domain.model;

import com.hexapass.domain.common.TimeSlot; // 값 객체 - 시간대
import com.hexapass.domain.type.ResourceType; // 열거형 - 리소스 타입

import java.time.DayOfWeek; // 요일을 나타내는 열거형 (월~일)
import java.time.LocalTime; // 시간만 나타내는 클래스 (날짜 없음)
import java.util.*; // Collection 관련 클래스들

/**
 * 예약 가능한 리소스를 나타내는 엔티티
 * 시설, 장비, 또는 서비스 등 예약할 수 있는 모든 자원
 * resourceId를 기준으로 동일성 판단
 * 
 * 엔티티 특징:
 * 1. 식별자: resourceId로 구분
 * 2. 복잡한 정책: 요일별 다른 운영시간, 수용인원 관리
 * 3. 가변 상태: 활성/비활성 전환 가능
 * 4. 풍부한 행동: 예약 가능성 판단, 수용률 계산 등
 */
public class Resource {

    // === 불변 필드들 (생성 후 변경 불가) ===
    private final String resourceId;           // 리소스 고유 식별자
    private final String name;                 // 리소스명 (사용자에게 표시)
    private final ResourceType type;           // 리소스 타입 (헬스장/스터디룸 등)
    private final String location;             // 위치 정보
    private final int capacity;                // 최대 수용 인원
    private final String description;          // 상세 설명
    
    // Map<DayOfWeek, List<TimeSlot>>: 요일별로 여러 운영시간 보유 가능
    // 예: 월요일 09:00-12:00, 14:00-18:00 (점심시간 휴무)
    private final Map<DayOfWeek, List<TimeSlot>> operatingSchedule;
    
    private final Set<String> features;        // 부가 기능/시설 목록

    // === 가변 필드들 ===
    private boolean isActive;                  // 운영 상태 (점검, 폐쇄 등으로 비활성화 가능)

    /**
     * private 생성자 - 팩토리 메서드를 통해서만 생성 가능
     * 
     * 복잡한 생성 파라미터와 유효성 검증을 통한 안전한 객체 생성
     */
    private Resource(String resourceId, String name, ResourceType type, String location,
                     int capacity, String description, Map<DayOfWeek, List<TimeSlot>> operatingSchedule,
                     Set<String> features) {
        
        // 기본 필드 유효성 검증
        this.resourceId = validateNotBlank(resourceId, "리소스 ID");
        this.name = validateNotBlank(name, "리소스명");
        this.type = validateNotNull(type, "리소스 타입");
        this.location = validateNotBlank(location, "위치");
        this.capacity = validatePositive(capacity, "수용 인원");
        
        // 선택적 필드 처리 (null 허용)
        this.description = description != null ? description.trim() : "";
        
        // 복잡한 컬렉션 필드 처리
        this.operatingSchedule = validateAndCopySchedule(operatingSchedule);
        this.features = features != null ? Set.copyOf(features) : Set.of(); // 빈 Set으로 초기화
        
        // 기본값 설정
        this.isActive = true; // 생성 시 활성 상태
    }

    /**
     * 기본 리소스 생성
     * 
     * 필수 정보만으로 리소스 생성 (24시간 운영, 부가기능 없음)
     */
    public static Resource create(String resourceId, String name, ResourceType type,
                                  String location, int capacity) {
        return new Resource(resourceId, name, type, location, capacity, null,
                createDefault24HourSchedule(), null);
    }

    /**
     * 상세 정보가 포함된 리소스 생성
     * 
     * 모든 정보를 세밀하게 설정할 수 있는 생성 메서드
     */
    public static Resource createWithDetails(String resourceId, String name, ResourceType type,
                                             String location, int capacity, String description,
                                             Map<DayOfWeek, List<TimeSlot>> operatingSchedule,
                                             Set<String> features) {
        return new Resource(resourceId, name, type, location, capacity, description,
                operatingSchedule, features);
    }

    /**
     * 헬스장 리소스 생성 편의 메서드
     * 
     * 헬스장 특성에 맞는 기본값들로 리소스 생성
     * 실제 비즈니스에서 자주 사용되는 패턴을 템플릿화
     */
    public static Resource createGym(String resourceId, String name, String location, int capacity) {
        // 헬스장 일반적인 운영시간: 평일 06:00-23:00
        Map<DayOfWeek, List<TimeSlot>> schedule = createWeekdaySchedule(
                LocalTime.of(6, 0),   // 오전 6시 오픈
                LocalTime.of(23, 0)   // 오후 11시 마감
        );

        return createWithDetails(resourceId, name, ResourceType.GYM, location, capacity,
                "헬스 기구와 웨이트 트레이닝 시설", schedule,
                // 헬스장 일반적인 부대시설들
                Set.of("웨이트 기구", "런닝머신", "에어컨", "음향시설"));
    }

    /**
     * 스터디룸 리소스 생성 편의 메서드
     * 
     * 스터디룸 특성에 맞는 24시간 운영과 학습 관련 시설
     */
    public static Resource createStudyRoom(String resourceId, String name, String location, int capacity) {
        // 스터디룸은 보통 24시간 운영
        Map<DayOfWeek, List<TimeSlot>> schedule = createDefault24HourSchedule();

        return createWithDetails(resourceId, name, ResourceType.STUDY_ROOM, location, capacity,
                "조용한 개인 또는 그룹 스터디 공간", schedule,
                // 스터디룸 필수 시설들
                Set.of("책상", "의자", "화이트보드", "WiFi", "에어컨"));
    }

    // =========================
    // 예약 가능성 확인 메서드들
    // =========================

    /**
     * 지정된 시간대에 예약 가능한지 확인
     * 
     * 가장 기본적인 예약 가능성 검사
     * 리소스가 비활성 상태면 무조건 불가
     * 
     * @param timeSlot 확인할 시간대
     * @return 예약 가능하면 true
     */
    public boolean isAvailable(TimeSlot timeSlot) {
        if (!isActive) {
            return false; // 비활성 리소스는 예약 불가
        }

        return isOperatingDuring(timeSlot); // 운영시간 내인지 확인
    }

    /**
     * 운영 시간 내인지 확인
     * 
     * 복잡한 운영 스케줄을 고려한 시간 확인
     * 요일별로 다른 운영시간을 적용
     * 
     * @param timeSlot 확인할 시간대
     * @return 운영시간 내이면 true
     */
    public boolean isOperatingDuring(TimeSlot timeSlot) {
        if (timeSlot == null) {
            return false;
        }

        // 시간대의 시작 시간 기준으로 요일 결정
        DayOfWeek dayOfWeek = timeSlot.getStartTime().getDayOfWeek();
        
        // 해당 요일의 운영 스케줄 조회
        List<TimeSlot> dailySchedule = operatingSchedule.get(dayOfWeek);

        if (dailySchedule == null || dailySchedule.isEmpty()) {
            return false; // 해당 요일에 운영하지 않음 (휴무일)
        }

        // 요청한 시간대가 운영 시간 중 어느 하나에라도 완전히 포함되는지 확인
        // Stream.anyMatch(): 조건을 만족하는 요소가 하나라도 있으면 true
        return dailySchedule.stream()
                .anyMatch(operatingSlot -> operatingSlot.contains(timeSlot));
    }

    /**
     * 현재 운영 중인지 확인
     * 
     * 실시간 리소스 상태 확인용
     * 사용자가 지금 당장 이용할 수 있는지 판단
     * 
     * @return 현재 운영 중이면 true
     */
    public boolean isCurrentlyOperating() {
        if (!isActive) {
            return false;
        }

        // 현재 시각을 1분짜리 시간대로 만들어서 확인
        // TimeSlot.ofDuration(): 시작시간 + 지속시간으로 TimeSlot 생성
        TimeSlot currentMoment = TimeSlot.ofDuration(
                java.time.LocalDateTime.now(), 1); // 현재부터 1분간

        return isOperatingDuring(currentMoment);
    }

    /**
     * 특정 날짜의 운영 시간 조회
     * 
     * 사용자에게 운영시간 정보를 보여줄 때 사용
     * 
     * @param dayOfWeek 확인할 요일
     * @return 해당 요일의 운영시간 목록 (불변 복사본)
     */
    public List<TimeSlot> getOperatingHours(DayOfWeek dayOfWeek) {
        List<TimeSlot> schedule = operatingSchedule.get(dayOfWeek);
        // List.copyOf(): null-safe 불변 복사본 생성
        return schedule != null ? List.copyOf(schedule) : List.of(); // 빈 리스트 반환
    }

    /**
     * 주간 운영 스케줄 조회
     * 
     * 전체 주간 스케줄을 보여줄 때 사용
     * 
     * @return 요일별 운영시간의 불변 복사본
     */
    public Map<DayOfWeek, List<TimeSlot>> getWeeklySchedule() {
        Map<DayOfWeek, List<TimeSlot>> result = new EnumMap<>(DayOfWeek.class);
        // forEach(): Map의 각 엔트리에 대해 람다 실행
        operatingSchedule.forEach((day, slots) -> result.put(day, List.copyOf(slots)));
        return result;
    }

    // =========================
    // 수용 인원 관리 메서드들
    // =========================

    /**
     * 추가 예약 가능 인원 확인
     * 
     * 현재 이용 인원을 고려하여 추가 예약 가능성 판단
     * 
     * @param currentOccupancy 현재 이용 인원 수
     * @return 추가 예약이 가능하면 true
     */
    public boolean hasCapacity(int currentOccupancy) {
        if (currentOccupancy < 0) {
            throw new IllegalArgumentException("현재 이용 인원은 0 이상이어야 합니다");
        }

        return isActive && currentOccupancy < capacity;
    }

    /**
     * 남은 수용 인원 계산
     * 
     * 예약 시스템에서 "n명 더 예약 가능" 표시할 때 사용
     * 
     * @param currentOccupancy 현재 이용 인원 수
     * @return 추가 수용 가능 인원 (0 이상)
     */
    public int getRemainingCapacity(int currentOccupancy) {
        if (currentOccupancy < 0) {
            throw new IllegalArgumentException("현재 이용 인원은 0 이상이어야 합니다");
        }

        if (!isActive) {
            return 0; // 비활성 리소스는 추가 수용 불가
        }

        // Math.max(): 음수가 나오지 않도록 0 이상으로 보장
        return Math.max(0, capacity - currentOccupancy);
    }

    /**
     * 수용률 계산 (0.0 ~ 1.0)
     * 
     * 리소스 이용률 통계, 혼잡도 표시 등에 활용
     * 
     * @param currentOccupancy 현재 이용 인원 수
     * @return 수용률 (0.0 = 빈 상태, 1.0 = 만석)
     */
    public double getOccupancyRate(int currentOccupancy) {
        if (capacity == 0) {
            return 1.0; // 용량이 0이면 항상 가득참으로 처리
        }

        // Math.min(): 100%를 초과하지 않도록 제한
        return Math.min(1.0, (double) currentOccupancy / capacity);
    }

    /**
     * 만석인지 확인
     * 
     * UI에서 "만석" 표시나 예약 불가 안내에 사용
     * 
     * @param currentOccupancy 현재 이용 인원 수
     * @return 만석이면 true
     */
    public boolean isFull(int currentOccupancy) {
        return currentOccupancy >= capacity;
    }

    // =========================
    // 리소스 관리 메서드들
    // =========================

    /**
     * 리소스 비활성화
     * 
     * 점검, 수리, 임시 폐쇄 등의 이유로 비활성화
     * 기존 예약은 유지하되 신규 예약만 차단
     */
    public void deactivate() {
        this.isActive = false;
    }

    /**
     * 리소스 활성화
     * 
     * 점검 완료, 수리 완료 후 재개방
     */
    public void activate() {
        this.isActive = true;
    }

    /**
     * 특정 기능/시설 보유 여부 확인
     * 
     * 사용자의 특별 요구사항 만족 여부 확인
     * 예: "WiFi 있는 스터디룸", "에어컨 있는 헬스장"
     * 
     * @param feature 확인할 기능/시설명
     * @return 보유하고 있으면 true
     */
    public boolean hasFeature(String feature) {
        return features.contains(feature);
    }

    /**
     * 여러 기능/시설을 모두 보유하는지 확인
     * 
     * 복합 조건 확인에 사용
     * 
     * @param requiredFeatures 필요한 기능들
     * @return 모든 기능을 보유하면 true
     */
    public boolean hasAllFeatures(Set<String> requiredFeatures) {
        // Set.containsAll(): 모든 요소를 포함하는지 확인
        return features.containsAll(requiredFeatures);
    }

    /**
     * 리소스 정보 요약
     * 
     * 관리자 화면이나 사용자 선택 화면에서 보여줄 요약 정보
     * 
     * @return 리소스 정보 요약 문자열
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        
        // 기본 정보
        summary.append(String.format("%s (%s) - %s, 수용인원 %d명",
                name, type.getDisplayName(), location, capacity));

        // 설명 추가 (있는 경우)
        if (!description.isEmpty()) {
            summary.append(" | ").append(description);
        }

        // 부가 기능 추가 (있는 경우)
        if (!features.isEmpty()) {
            // String.join(): Collection을 구분자로 연결하여 문자열 생성
            summary.append(" | 시설: ").append(String.join(", ", features));
        }

        // 운영 상태 표시
        summary.append(isActive ? " [운영중]" : " [중단]");

        return summary.toString();
    }

    // =========================
    // Object 메서드 오버라이드
    // =========================

    /**
     * equals 메서드 오버라이드
     * 
     * 엔티티: 식별자(resourceId) 기준으로만 동일성 판단
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Resource resource = (Resource) obj;
        return Objects.equals(resourceId, resource.resourceId);
    }

    /**
     * hashCode 메서드 오버라이드
     */
    @Override
    public int hashCode() {
        return Objects.hash(resourceId);
    }

    /**
     * toString 메서드 오버라이드
     * 
     * 디버깅용 간결한 표현
     */
    @Override
    public String toString() {
        return String.format("Resource{id='%s', name='%s', type=%s, location='%s', capacity=%d, active=%s}",
                resourceId, name, type, location, capacity, isActive);
    }

    // =========================
    // Getter 메서드들
    // =========================

    public String getResourceId() {
        return resourceId;
    }

    public String getName() {
        return name;
    }

    public ResourceType getType() {
        return type;
    }

    public String getLocation() {
        return location;
    }

    public int getCapacity() {
        return capacity;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return isActive;
    }

    /**
     * 부가 기능 목록 반환
     * 
     * Set.copyOf(): 방어적 복사로 외부에서 수정 불가능한 불변 복사본 반환
     */
    public Set<String> getFeatures() {
        return Set.copyOf(features);
    }

    // =========================
    // 헬퍼 메서드들 (private static)
    // =========================

    /**
     * 24시간 운영 스케줄 생성
     * 
     * 스터디룸, 24시간 헬스장 등에서 사용
     * 모든 요일에 대해 하루 종일 운영하는 스케줄 생성
     * 
     * @return 24시간 운영 스케줄
     */
    private static Map<DayOfWeek, List<TimeSlot>> createDefault24HourSchedule() {
        Map<DayOfWeek, List<TimeSlot>> schedule = new EnumMap<>(DayOfWeek.class);
        
        // 하루 종일 운영하는 TimeSlot 생성
        // LocalTime.MIN: 00:00:00, LocalTime.MAX: 23:59:59.999999999
        // atDate(): LocalTime을 특정 날짜와 결합하여 LocalDateTime 생성
        TimeSlot fullDay = TimeSlot.of(
                LocalTime.MIN.atDate(java.time.LocalDate.now()),  // 00:00:00
                LocalTime.MAX.atDate(java.time.LocalDate.now())   // 23:59:59
        );

        // 모든 요일에 대해 24시간 운영 설정
        // DayOfWeek.values(): 월요일부터 일요일까지 모든 요일
        for (DayOfWeek day : DayOfWeek.values()) {
            schedule.put(day, List.of(fullDay)); // 단일 시간대로 설정
        }

        return schedule;
    }

    /**
     * 평일 운영 스케줄 생성 (주말 휴무)
     * 
     * 일반적인 비즈니스 운영 시간 패턴
     * 평일만 운영하고 주말은 휴무인 리소스용
     * 
     * @param openTime 오픈 시각
     * @param closeTime 마감 시각
     * @return 평일 운영 스케줄
     */
    private static Map<DayOfWeek, List<TimeSlot>> createWeekdaySchedule(LocalTime openTime, LocalTime closeTime) {
        Map<DayOfWeek, List<TimeSlot>> schedule = new EnumMap<>(DayOfWeek.class);

        // 운영시간 TimeSlot 생성
        // 임시로 오늘 날짜를 사용 (실제로는 날짜보다는 시간이 중요)
        TimeSlot operatingHours = TimeSlot.of(
                openTime.atDate(java.time.LocalDate.now()),   // 오픈 시각
                closeTime.atDate(java.time.LocalDate.now())   // 마감 시각
        );

        // 평일(월~금)만 운영 설정
        schedule.put(DayOfWeek.MONDAY, List.of(operatingHours));
        schedule.put(DayOfWeek.TUESDAY, List.of(operatingHours));
        schedule.put(DayOfWeek.WEDNESDAY, List.of(operatingHours));
        schedule.put(DayOfWeek.THURSDAY, List.of(operatingHours));
        schedule.put(DayOfWeek.FRIDAY, List.of(operatingHours));

        // 주말은 휴무 (빈 리스트로 설정)
        // List.of(): 불변 빈 리스트 생성
        schedule.put(DayOfWeek.SATURDAY, List.of());
        schedule.put(DayOfWeek.SUNDAY, List.of());

        return schedule;
    }

    // =========================
    // 검증 메서드들 (private)
    // =========================

    private String validateNotBlank(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + "은 null이거나 빈 값일 수 없습니다");
        }
        return value.trim();
    }

    private <T> T validateNotNull(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + "은 null일 수 없습니다");
        }
        return value;
    }

    private int validatePositive(int value, String fieldName) {
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + "은 0보다 커야 합니다. 입력값: " + value);
        }
        return value;
    }

    /**
     * 운영 스케줄 유효성 검증 및 방어적 복사
     * 
     * 복잡한 컬렉션 필드의 안전한 처리
     * 1. null 체크
     * 2. 방어적 복사 (외부 변경으로부터 보호)
     * 3. 정렬 (시간 순서대로 배치)
     * 4. 불변화 (외부에서 수정 불가)
     * 
     * @param schedule 검증할 운영 스케줄
     * @return 검증되고 정렬된 불변 스케줄
     */
    private Map<DayOfWeek, List<TimeSlot>> validateAndCopySchedule(Map<DayOfWeek, List<TimeSlot>> schedule) {
        if (schedule == null) {
            return new EnumMap<>(DayOfWeek.class); // 빈 맵 반환
        }

        Map<DayOfWeek, List<TimeSlot>> result = new EnumMap<>(DayOfWeek.class);
        
        // 각 요일별 TimeSlot 리스트 처리
        schedule.forEach((day, slots) -> {
            if (slots != null) {
                // 방어적 복사: 외부 List 변경이 내부에 영향주지 않도록
                List<TimeSlot> copiedSlots = new ArrayList<>(slots);
                
                // 시간 순서대로 정렬
                // Comparator: TimeSlot의 시작 시간 기준 정렬
                copiedSlots.sort((a, b) -> a.getStartTime().compareTo(b.getStartTime()));
                
                // 불변 리스트로 변환하여 저장
                result.put(day, List.copyOf(copiedSlots));
            }
        });

        return result;
    }
}
```

## 주요 설계 원칙 및 패턴

### 1. 복잡한 운영 정책 관리

#### 요일별 다른 운영시간
```java
// 예시: 헬스장이 평일과 주말에 다른 운영시간을 가지는 경우
Map<DayOfWeek, List<TimeSlot>> schedule = new EnumMap<>(DayOfWeek.class);

// 평일: 06:00-23:00
TimeSlot weekdayHours = TimeSlot.of(
    LocalTime.of(6, 0).atDate(LocalDate.now()),
    LocalTime.of(23, 0).atDate(LocalDate.now())
);

// 주말: 08:00-22:00  
TimeSlot weekendHours = TimeSlot.of(
    LocalTime.of(8, 0).atDate(LocalDate.now()),
    LocalTime.of(22, 0).atDate(LocalDate.now())
);

// 평일 설정
schedule.put(DayOfWeek.MONDAY, List.of(weekdayHours));
schedule.put(DayOfWeek.TUESDAY, List.of(weekdayHours));
// ...

// 주말 설정  
schedule.put(DayOfWeek.SATURDAY, List.of(weekendHours));
schedule.put(DayOfWeek.SUNDAY, List.of(weekendHours));
```

#### 하루 중 여러 운영시간 (점심시간 휴무)
```java
// 예시: 09:00-12:00, 14:00-18:00 (12:00-14:00 점심시간 휴무)
List<TimeSlot> mondaySchedule = List.of(
    TimeSlot.of(LocalTime.of(9, 0).atDate(today), LocalTime.of(12, 0).atDate(today)),
    TimeSlot.of(LocalTime.of(14, 0).atDate(today), LocalTime.of(18, 0).atDate(today))
);
schedule.put(DayOfWeek.MONDAY, mondaySchedule);
```

### 2. 수용 인원 관리 시스템

#### 다양한 수용률 계산
```java
Resource gym = Resource.createGym("GYM001", "메인 헬스장", "B1층", 50);

// 현재 30명 이용 중일 때
int currentUsers = 30;

boolean hasSpace = gym.hasCapacity(currentUsers);        // true (30 < 50)
int remaining = gym.getRemainingCapacity(currentUsers);  // 20명 더 가능
double occupancyRate = gym.getOccupancyRate(currentUsers); // 0.6 (60%)
boolean isFull = gym.isFull(currentUsers);               // false
```

#### 실시간 예약 가능성 판단
```java
public boolean canAcceptReservation(Resource resource, TimeSlot requestedTime, int requestedCount) {
    // 1. 리소스 활성 상태 확인
    if (!resource.isActive()) return false;
    
    // 2. 운영시간 확인  
    if (!resource.isOperatingDuring(requestedTime)) return false;
    
    // 3. 수용인원 확인
    int currentOccupancy = getCurrentOccupancy(resource, requestedTime);
    return resource.getRemainingCapacity(currentOccupancy) >= requestedCount;
}
```

### 3. 팩토리 메서드의 계층화

#### 기본 → 상세 → 특화된 팩토리
```java
// 1단계: 최소 정보로 생성
Resource.create(id, name, type, location, capacity);

// 2단계: 모든 상세 정보 지정
Resource.createWithDetails(id, name, type, location, capacity, description, schedule, features);

// 3단계: 도메인별 특화 생성
Resource.createGym(id, name, location, capacity);        // 헬스장 특화
Resource.createStudyRoom(id, name, location, capacity);  // 스터디룸 특화
```

### 4. 컬렉션의 안전한 관리

#### 방어적 복사 + 불변화
```java
// 생성자에서: 외부 컬렉션 변경으로부터 보호
this.features = features != null ? Set.copyOf(features) : Set.of();
this.operatingSchedule = validateAndCopySchedule(operatingSchedule);

// Getter에서: 내부 컬렉션을 외부에서 변경하지 못하도록
public Set<String> getFeatures() {
    return Set.copyOf(features); // 불변 복사본 반환
}

public Map<DayOfWeek, List<TimeSlot>> getWeeklySchedule() {
    Map<DayOfWeek, List<TimeSlot>> result = new EnumMap<>(DayOfWeek.class);
    operatingSchedule.forEach((day, slots) -> result.put(day, List.copyOf(slots)));
    return result;
}
```

### 5. EnumMap의 효과적 활용

#### DayOfWeek을 키로 하는 최적화된 Map
```java
// EnumMap 사용 이유:
// 1. HashMap보다 빠름 (배열 기반)
// 2. 메모리 효율적
// 3. 순서 보장 (enum 선언 순서)
// 4. null 키 불허 (안전성)
Map<DayOfWeek, List<TimeSlot>> schedule = new EnumMap<>(DayOfWeek.class);
```

### 6. 시간 처리의 복잡성 해결

#### LocalTime vs LocalDateTime
```java
// LocalTime: 시간만 (09:00, 18:00)
LocalTime openTime = LocalTime.of(9, 0);

// LocalDateTime: 날짜+시간 (2024-01-15 09:00)  
LocalDateTime openDateTime = openTime.atDate(LocalDate.now());

// TimeSlot은 LocalDateTime을 요구하므로 변환 필요
TimeSlot operatingHours = TimeSlot.of(openDateTime, closeDateTime);
```

### 7. 비즈니스 규칙의 캡슐화

#### 복잡한 예약 가능성 로직
```java
public boolean isAvailable(TimeSlot timeSlot) {
    if (!isActive) return false;           // 1. 활성 상태
    return isOperatingDuring(timeSlot);    // 2. 운영 시간
    // 3. 수용 인원은 별도 메서드에서 확인
}

public boolean isOperatingDuring(TimeSlot timeSlot) {
    DayOfWeek dayOfWeek = timeSlot.getStartTime().getDayOfWeek();
    List<TimeSlot> dailySchedule = operatingSchedule.get(dayOfWeek);
    
    if (dailySchedule == null || dailySchedule.isEmpty()) return false;
    
    // 요청 시간대가 운영 시간대 중 하나에 완전히 포함되는지 확인
    return dailySchedule.stream().anyMatch(slot -> slot.contains(timeSlot));
}
```

### 8. 확장성을 위한 설계

#### 새로운 리소스 타입 추가
```java
// 새로운 편의 메서드 쉽게 추가 가능
public static Resource createPool(String resourceId, String name, String location, int capacity) {
    Map<DayOfWeek, List<TimeSlot>> schedule = createWeekdaySchedule(
        LocalTime.of(6, 0), LocalTime.of(22, 0)
    );
    
    return createWithDetails(resourceId, name, ResourceType.POOL, location, capacity,
        "실내 수영장", schedule,
        Set.of("온수 풀", "샤워실", "락커", "수영 용품 대여"));
}
```

#### 복잡한 운영 정책 지원
```java
// 향후 확장: 시즌별 다른 운영시간
public void updateSeasonalSchedule(Season season, Map<DayOfWeek, List<TimeSlot>> schedule) {
    // 계절별 운영시간 변경 로직
}

// 향후 확장: 동적 수용인원 조정
public void adjustCapacity(int newCapacity, String reason) {
    // 수용인원 동적 변경 로직
}
```

### 9. 실제 사용 예시

#### 리소스 생성과 운영
```java
// 헬스장 리소스 생성
Resource gym = Resource.createGym("GYM001", "메인 헬스장", "지하 1층", 50);

// 현재 운영 중인지 확인
boolean isOpen = gym.isCurrentlyOperating();

// 특정 시간대 예약 가능성 확인
TimeSlot requestedTime = TimeSlot.of(
    LocalDateTime.of(2024, 1, 15, 10, 0),  // 2024-01-15 10:00
    LocalDateTime.of(2024, 1, 15, 11, 0)   // 2024-01-15 11:00
);
boolean canReserve = gym.isAvailable(requestedTime);

// 수용인원 확인
int currentUsers = getCurrentReservationCount(gym, requestedTime);
boolean hasCapacity = gym.hasCapacity(currentUsers);
```

#### 운영 스케줄 조회
```java
// 월요일 운영시간 확인
List<TimeSlot> mondayHours = gym.getOperatingHours(DayOfWeek.MONDAY);
// 결과: [2024-01-15 06:00 ~ 2024-01-15 23:00 (1020분)]

// 전체 주간 스케줄 확인
Map<DayOfWeek, List<TimeSlot>> weeklySchedule = gym.getWeeklySchedule();
```

이러한 설계로 Resource는 단순한 정보 저장소가 아닌, 복잡한 운영 정책과 비즈니스 규칙을 완전히 캡슐화한 풍부한 도메인 객체가 되었습니다.