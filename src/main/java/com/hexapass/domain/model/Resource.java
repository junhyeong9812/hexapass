package com.hexapass.domain.model;

import com.hexapass.domain.common.TimeSlot;
import com.hexapass.domain.type.ResourceType;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;

/**
 * 예약 가능한 리소스를 나타내는 엔티티
 * 시설, 장비, 또는 서비스 등 예약할 수 있는 모든 자원
 * resourceId를 기준으로 동일성 판단
 */
public class Resource {

    private final String resourceId;
    private final String name;
    private final ResourceType type;
    private final String location;
    private final int capacity;
    private final String description;
    private final Map<DayOfWeek, List<TimeSlot>> operatingSchedule;
    private boolean isActive;
    private final Set<String> features; // 부가 기능/시설

    /**
     * private 생성자 - 팩토리 메서드를 통해서만 생성 가능
     */
    private Resource(String resourceId, String name, ResourceType type, String location,
                     int capacity, String description, Map<DayOfWeek, List<TimeSlot>> operatingSchedule,
                     Set<String> features) {
        this.resourceId = validateNotBlank(resourceId, "리소스 ID");
        this.name = validateNotBlank(name, "리소스명");
        this.type = validateNotNull(type, "리소스 타입");
        this.location = validateNotBlank(location, "위치");
        this.capacity = validatePositive(capacity, "수용 인원");
        this.description = description != null ? description.trim() : "";
        this.operatingSchedule = validateAndCopySchedule(operatingSchedule);
        this.features = features != null ? Set.copyOf(features) : Set.of();
        this.isActive = true;
    }

    /**
     * 기본 리소스 생성
     */
    public static Resource create(String resourceId, String name, ResourceType type,
                                  String location, int capacity) {
        return new Resource(resourceId, name, type, location, capacity, null,
                createDefault24HourSchedule(), null);
    }

    /**
     * 상세 정보가 포함된 리소스 생성
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
     */
    public static Resource createGym(String resourceId, String name, String location, int capacity) {
        Map<DayOfWeek, List<TimeSlot>> schedule = createWeekdaySchedule(
                LocalTime.of(6, 0), LocalTime.of(23, 0)
        );

        return createWithDetails(resourceId, name, ResourceType.GYM, location, capacity,
                "헬스 기구와 웨이트 트레이닝 시설", schedule,
                Set.of("웨이트 기구", "런닝머신", "에어컨", "음향시설"));
    }

    /**
     * 스터디룸 리소스 생성 편의 메서드
     */
    public static Resource createStudyRoom(String resourceId, String name, String location, int capacity) {
        Map<DayOfWeek, List<TimeSlot>> schedule = createDefault24HourSchedule();

        return createWithDetails(resourceId, name, ResourceType.STUDY_ROOM, location, capacity,
                "조용한 개인 또는 그룹 스터디 공간", schedule,
                Set.of("책상", "의자", "화이트보드", "WiFi", "에어컨"));
    }

    // =========================
    // 예약 가능성 확인 메서드들
    // =========================

    /**
     * 지정된 시간대에 예약 가능한지 확인
     */
    public boolean isAvailable(TimeSlot timeSlot) {
        if (!isActive) {
            return false;
        }

        return isOperatingDuring(timeSlot);
    }

    /**
     * 운영 시간 내인지 확인
     */
    public boolean isOperatingDuring(TimeSlot timeSlot) {
        if (timeSlot == null) {
            return false;
        }

        DayOfWeek dayOfWeek = timeSlot.getStartTime().getDayOfWeek();
        List<TimeSlot> dailySchedule = operatingSchedule.get(dayOfWeek);

        if (dailySchedule == null || dailySchedule.isEmpty()) {
            return false; // 해당 요일에 운영하지 않음
        }

        // 요청한 시간대가 운영 시간 중 어느 하나라도 포함되는지 확인
        return dailySchedule.stream()
                .anyMatch(operatingSlot -> operatingSlot.contains(timeSlot));
    }

    /**
     * 현재 운영 중인지 확인
     */
    public boolean isCurrentlyOperating() {
        if (!isActive) {
            return false;
        }

        TimeSlot currentMoment = TimeSlot.ofDuration(
                java.time.LocalDateTime.now(), 1); // 1분짜리 현재 시점

        return isOperatingDuring(currentMoment);
    }

    /**
     * 특정 날짜의 운영 시간 조회
     */
    public List<TimeSlot> getOperatingHours(DayOfWeek dayOfWeek) {
        List<TimeSlot> schedule = operatingSchedule.get(dayOfWeek);
        return schedule != null ? List.copyOf(schedule) : List.of();
    }

    /**
     * 주간 운영 스케줄 조회
     */
    public Map<DayOfWeek, List<TimeSlot>> getWeeklySchedule() {
        Map<DayOfWeek, List<TimeSlot>> result = new EnumMap<>(DayOfWeek.class);
        operatingSchedule.forEach((day, slots) -> result.put(day, List.copyOf(slots)));
        return result;
    }

    // =========================
    // 수용 인원 관리 메서드들
    // =========================

    /**
     * 추가 예약 가능 인원 확인
     */
    public boolean hasCapacity(int currentOccupancy) {
        if (currentOccupancy < 0) {
            throw new IllegalArgumentException("현재 이용 인원은 0 이상이어야 합니다");
        }

        return isActive && currentOccupancy < capacity;
    }

    /**
     * 남은 수용 인원 계산
     */
    public int getRemainingCapacity(int currentOccupancy) {
        if (currentOccupancy < 0) {
            throw new IllegalArgumentException("현재 이용 인원은 0 이상이어야 합니다");
        }

        if (!isActive) {
            return 0;
        }

        return Math.max(0, capacity - currentOccupancy);
    }

    /**
     * 수용률 계산 (0.0 ~ 1.0)
     */
    public double getOccupancyRate(int currentOccupancy) {
        if (capacity == 0) {
            return 1.0; // 용량이 0이면 항상 가득참
        }

        return Math.min(1.0, (double) currentOccupancy / capacity);
    }

    /**
     * 만석인지 확인
     */
    public boolean isFull(int currentOccupancy) {
        return currentOccupancy >= capacity;
    }

    // =========================
    // 리소스 관리 메서드들
    // =========================

    /**
     * 리소스 비활성화
     */
    public void deactivate() {
        this.isActive = false;
    }

    /**
     * 리소스 활성화
     */
    public void activate() {
        this.isActive = true;
    }

    /**
     * 특정 기능/시설 보유 여부 확인
     */
    public boolean hasFeature(String feature) {
        return features.contains(feature);
    }

    /**
     * 여러 기능/시설을 모두 보유하는지 확인
     */
    public boolean hasAllFeatures(Set<String> requiredFeatures) {
        return features.containsAll(requiredFeatures);
    }

    /**
     * 리소스 정보 요약
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append(String.format("%s (%s) - %s, 수용인원 %d명",
                name, type.getDisplayName(), location, capacity));

        if (!description.isEmpty()) {
            summary.append(" | ").append(description);
        }

        if (!features.isEmpty()) {
            summary.append(" | 시설: ").append(String.join(", ", features));
        }

        summary.append(isActive ? " [운영중]" : " [중단]");

        return summary.toString();
    }

    // =========================
    // Object 메서드 오버라이드
    // =========================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Resource resource = (Resource) obj;
        return Objects.equals(resourceId, resource.resourceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourceId);
    }

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

    public Set<String> getFeatures() {
        return Set.copyOf(features); // 불변 복사본 반환
    }

    // =========================
    // 헬퍼 메서드들 (private static)
    // =========================

    /**
     * 24시간 운영 스케줄 생성
     */
    private static Map<DayOfWeek, List<TimeSlot>> createDefault24HourSchedule() {
        Map<DayOfWeek, List<TimeSlot>> schedule = new EnumMap<>(DayOfWeek.class);
        TimeSlot fullDay = TimeSlot.of(
                LocalTime.MIN.atDate(java.time.LocalDate.now()),
                LocalTime.MAX.atDate(java.time.LocalDate.now())
        );

        for (DayOfWeek day : DayOfWeek.values()) {
            schedule.put(day, List.of(fullDay));
        }

        return schedule;
    }

    /**
     * 평일 운영 스케줄 생성 (주말 휴무)
     */
    private static Map<DayOfWeek, List<TimeSlot>> createWeekdaySchedule(LocalTime openTime, LocalTime closeTime) {
        Map<DayOfWeek, List<TimeSlot>> schedule = new EnumMap<>(DayOfWeek.class);

        TimeSlot operatingHours = TimeSlot.of(
                openTime.atDate(java.time.LocalDate.now()),
                closeTime.atDate(java.time.LocalDate.now())
        );

        // 평일만 운영
        schedule.put(DayOfWeek.MONDAY, List.of(operatingHours));
        schedule.put(DayOfWeek.TUESDAY, List.of(operatingHours));
        schedule.put(DayOfWeek.WEDNESDAY, List.of(operatingHours));
        schedule.put(DayOfWeek.THURSDAY, List.of(operatingHours));
        schedule.put(DayOfWeek.FRIDAY, List.of(operatingHours));

        // 주말은 휴무 (빈 리스트)
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

    private Map<DayOfWeek, List<TimeSlot>> validateAndCopySchedule(Map<DayOfWeek, List<TimeSlot>> schedule) {
        if (schedule == null) {
            return new EnumMap<>(DayOfWeek.class);
        }

        Map<DayOfWeek, List<TimeSlot>> result = new EnumMap<>(DayOfWeek.class);
        schedule.forEach((day, slots) -> {
            if (slots != null) {
                List<TimeSlot> copiedSlots = new ArrayList<>(slots);
                copiedSlots.sort((a, b) -> a.getStartTime().compareTo(b.getStartTime()));
                result.put(day, List.copyOf(copiedSlots)); // 불변 리스트로 변환
            }
        });

        return result;
    }
}