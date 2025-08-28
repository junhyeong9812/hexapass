package com.hexapass.domain.port.outbound;

import com.hexapass.domain.model.Resource;
import com.hexapass.domain.type.ResourceType;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 리소스 저장소 포트 (Outbound Port)
 * 예약 가능한 리소스 관리를 위한 추상화
 */
public interface ResourceRepository {

    // =========================
    // 기본 CRUD 연산
    // =========================

    /**
     * 리소스 ID로 리소스 조회
     */
    Optional<Resource> findById(String resourceId);

    /**
     * 리소스 저장 (생성/수정)
     */
    void save(Resource resource);

    /**
     * 리소스 삭제
     */
    void delete(String resourceId);

    /**
     * 리소스 존재 여부 확인
     */
    boolean existsById(String resourceId);

    /**
     * 리소스명으로 중복 확인
     */
    boolean existsByName(String name);

    // =========================
    // 조건별 조회
    // =========================

    /**
     * 모든 리소스 조회
     */
    List<Resource> findAll();

    /**
     * 활성화된 리소스 조회
     */
    List<Resource> findActiveResources();

    /**
     * 비활성화된 리소스 조회
     */
    List<Resource> findInactiveResources();

    /**
     * 리소스 타입별 조회
     */
    List<Resource> findByType(ResourceType resourceType);

    /**
     * 활성 리소스 중 타입별 조회
     */
    List<Resource> findActiveByType(ResourceType resourceType);

    /**
     * 위치별 리소스 조회
     */
    List<Resource> findByLocation(String location);

    /**
     * 특정 위치의 활성 리소스 조회
     */
    List<Resource> findActiveByLocation(String location);

    // =========================
    // 수용 인원 기반 조회
    // =========================

    /**
     * 최소 수용 인원 이상인 리소스 조회
     */
    List<Resource> findByMinCapacity(int minCapacity);

    /**
     * 수용 인원 범위로 조회
     */
    List<Resource> findByCapacityRange(int minCapacity, int maxCapacity);

    /**
     * 특정 수용 인원 정확히 일치하는 리소스 조회
     */
    List<Resource> findByExactCapacity(int capacity);

    /**
     * 대형 리소스 조회 (수용인원 많은 순)
     */
    List<Resource> findLargeCapacityResources(int threshold);

    // =========================
    // 기능/시설 기반 조회
    // =========================

    /**
     * 특정 기능을 가진 리소스 조회
     */
    List<Resource> findByFeature(String feature);

    /**
     * 특정 기능들을 모두 가진 리소스 조회
     */
    List<Resource> findByFeatures(Set<String> features);

    /**
     * 특정 기능 중 하나라도 가진 리소스 조회
     */
    List<Resource> findByAnyFeatures(Set<String> features);

    // =========================
    // 운영 시간 기반 조회
    // =========================

    /**
     * 특정 요일에 운영하는 리소스 조회
     */
    List<Resource> findOperatingOnDay(DayOfWeek dayOfWeek);

    /**
     * 24시간 운영 리소스 조회
     */
    List<Resource> find24HourResources();

    /**
     * 주말 운영 리소스 조회
     */
    List<Resource> findWeekendOperatingResources();

    /**
     * 평일만 운영하는 리소스 조회
     */
    List<Resource> findWeekdayOnlyResources();

    // =========================
    // 검색 및 필터링
    // =========================

    /**
     * 리소스명으로 검색 (부분 일치)
     */
    List<Resource> findByNameContaining(String nameKeyword);

    /**
     * 설명으로 검색 (부분 일치)
     */
    List<Resource> findByDescriptionContaining(String descriptionKeyword);

    /**
     * 복합 조건으로 리소스 검색
     */
    List<Resource> findByConditions(
            ResourceType type,
            String location,
            Integer minCapacity,
            Boolean isActive,
            Set<String> requiredFeatures
    );

    /**
     * 예약 가능한 리소스 조회 (활성 + 운영중)
     */
    List<Resource> findAvailableResources();

    /**
     * 특정 타입의 예약 가능한 리소스 조회
     */
    List<Resource> findAvailableResourcesByType(ResourceType resourceType);

    // =========================
    // 정렬된 조회
    // =========================

    /**
     * 수용 인원 순 정렬 (많은 순)
     */
    List<Resource> findAllOrderByCapacityDesc();

    /**
     * 수용 인원 순 정렬 (적은 순)
     */
    List<Resource> findAllOrderByCapacityAsc();

    /**
     * 이름 순 정렬
     */
    List<Resource> findAllOrderByName();

    /**
     * 위치별, 이름순 정렬
     */
    List<Resource> findAllOrderByLocationAndName();

    // =========================
    // 통계 및 집계
    // =========================

    /**
     * 전체 리소스 수
     */
    long countAll();

    /**
     * 활성 리소스 수
     */
    long countActive();

    /**
     * 타입별 리소스 수
     */
    long countByType(ResourceType resourceType);

    /**
     * 위치별 리소스 수
     */
    long countByLocation(String location);

    /**
     * 전체 수용 인원 합계
     */
    long getTotalCapacity();

    /**
     * 타입별 수용 인원 합계
     */
    long getTotalCapacityByType(ResourceType resourceType);

    /**
     * 평균 수용 인원
     */
    double getAverageCapacity();

    /**
     * 최대/최소 수용 인원 리소스 조회
     */
    Optional<Resource> findLargestCapacityResource();
    Optional<Resource> findSmallestCapacityResource();

    // =========================
    // 위치 관련 메서드들
    // =========================

    /**
     * 모든 위치 목록 조회
     */
    List<String> findAllLocations();

    /**
     * 활성 리소스의 위치 목록 조회
     */
    List<String> findActiveLocations();

    /**
     * 특정 타입 리소스의 위치 목록 조회
     */
    List<String> findLocationsByType(ResourceType resourceType);

    // =========================
    // 기능/시설 관련 메서드들
    // =========================

    /**
     * 모든 기능 목록 조회
     */
    Set<String> findAllFeatures();

    /**
     * 특정 타입 리소스의 기능 목록 조회
     */
    Set<String> findFeaturesByType(ResourceType resourceType);

    /**
     * 인기 기능 목록 조회 (많이 보유한 순)
     */
    List<String> findPopularFeatures(int limit);

    // =========================
    // 배치 처리용 메서드들
    // =========================

    /**
     * 여러 리소스 일괄 저장
     */
    void saveAll(List<Resource> resources);

    /**
     * 페이징 처리된 리소스 목록 조회
     */
    List<Resource> findAll(int page, int size);

    /**
     * 활성 리소스 페이징 조회
     */
    List<Resource> findActive(int page, int size);

    /**
     * 타입별 페이징 조회
     */
    List<Resource> findByType(ResourceType resourceType, int page, int size);
}