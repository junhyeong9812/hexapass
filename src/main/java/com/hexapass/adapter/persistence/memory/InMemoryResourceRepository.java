package com.hexapass.adapter.persistence.memory;

import com.hexapass.domain.model.Resource;
import com.hexapass.domain.port.outbound.ResourceRepository;
import com.hexapass.domain.type.ResourceType;

import java.time.DayOfWeek;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 리소스 저장소의 인메모리 구현체
 */
public class InMemoryResourceRepository implements ResourceRepository {

    private final Map<String, Resource> resources = new ConcurrentHashMap<>();

    @Override
    public Optional<Resource> findById(String resourceId) {
        return Optional.ofNullable(resources.get(resourceId));
    }

    @Override
    public void save(Resource resource) {
        resources.put(resource.getResourceId(), resource);
    }

    @Override
    public void delete(String resourceId) {
        resources.remove(resourceId);
    }

    @Override
    public boolean existsById(String resourceId) {
        return resources.containsKey(resourceId);
    }

    @Override
    public boolean existsByName(String name) {
        return resources.values().stream()
                .anyMatch(resource -> name.equals(resource.getName()));
    }

    @Override
    public List<Resource> findAll() {
        return new ArrayList<>(resources.values());
    }

    @Override
    public List<Resource> findActiveResources() {
        return resources.values().stream()
                .filter(Resource::isActive)
                .collect(Collectors.toList());
    }

    @Override
    public List<Resource> findInactiveResources() {
        return resources.values().stream()
                .filter(resource -> !resource.isActive())
                .collect(Collectors.toList());
    }

    @Override
    public List<Resource> findByType(ResourceType resourceType) {
        return resources.values().stream()
                .filter(resource -> resourceType.equals(resource.getType()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Resource> findActiveByType(ResourceType resourceType) {
        return resources.values().stream()
                .filter(resource -> resource.isActive() && resourceType.equals(resource.getType()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Resource> findByLocation(String location) {
        return resources.values().stream()
                .filter(resource -> location.equals(resource.getLocation()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Resource> findActiveByLocation(String location) {
        return resources.values().stream()
                .filter(resource -> resource.isActive() && location.equals(resource.getLocation()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Resource> findByMinCapacity(int minCapacity) {
        return resources.values().stream()
                .filter(resource -> resource.getCapacity() >= minCapacity)
                .collect(Collectors.toList());
    }

    @Override
    public List<Resource> findByCapacityRange(int minCapacity, int maxCapacity) {
        return resources.values().stream()
                .filter(resource -> {
                    int capacity = resource.getCapacity();
                    return capacity >= minCapacity && capacity <= maxCapacity;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Resource> findByExactCapacity(int capacity) {
        return resources.values().stream()
                .filter(resource -> resource.getCapacity() == capacity)
                .collect(Collectors.toList());
    }

    @Override
    public List<Resource> findLargeCapacityResources(int threshold) {
        return resources.values().stream()
                .filter(resource -> resource.getCapacity() > threshold)
                .sorted((a, b) -> Integer.compare(b.getCapacity(), a.getCapacity())) // 큰 순서대로
                .collect(Collectors.toList());
    }

    @Override
    public List<Resource> findByFeature(String feature) {
        return resources.values().stream()
                .filter(resource -> resource.hasFeature(feature))
                .collect(Collectors.toList());
    }

    @Override
    public List<Resource> findByFeatures(Set<String> features) {
        return resources.values().stream()
                .filter(resource -> resource.hasAllFeatures(features))
                .collect(Collectors.toList());
    }

    @Override
    public List<Resource> findByAnyFeatures(Set<String> features) {
        return resources.values().stream()
                .filter(resource -> features.stream().anyMatch(resource::hasFeature))
                .collect(Collectors.toList());
    }

    @Override
    public List<Resource> findOperatingOnDay(DayOfWeek dayOfWeek) {
        return resources.values().stream()
                .filter(resource -> !resource.getOperatingHours(dayOfWeek).isEmpty())
                .collect(Collectors.toList());
    }

    @Override
    public List<Resource> find24HourResources() {
        return resources.values().stream()
                .filter(resource -> {
                    // 모든 요일에 24시간 운영하는 리소스 찾기
                    for (DayOfWeek day : DayOfWeek.values()) {
                        if (resource.getOperatingHours(day).isEmpty()) {
                            return false; // 하루라도 운영하지 않으면 24시간이 아님
                        }
                        // 실제로는 더 정교한 24시간 체크가 필요하지만 간소화
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Resource> findWeekendOperatingResources() {
        return resources.values().stream()
                .filter(resource ->
                        !resource.getOperatingHours(DayOfWeek.SATURDAY).isEmpty() ||
                                !resource.getOperatingHours(DayOfWeek.SUNDAY).isEmpty())
                .collect(Collectors.toList());
    }

    @Override
    public List<Resource> findWeekdayOnlyResources() {
        return resources.values().stream()
                .filter(resource -> {
                    // 평일에는 운영하고 주말에는 운영하지 않는 리소스
                    boolean hasWeekdayOperating = false;
                    for (DayOfWeek day : Arrays.asList(DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
                            DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)) {
                        if (!resource.getOperatingHours(day).isEmpty()) {
                            hasWeekdayOperating = true;
                            break;
                        }
                    }

                    boolean hasWeekendOperating =
                            !resource.getOperatingHours(DayOfWeek.SATURDAY).isEmpty() ||
                                    !resource.getOperatingHours(DayOfWeek.SUNDAY).isEmpty();

                    return hasWeekdayOperating && !hasWeekendOperating;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Resource> findByNameContaining(String nameKeyword) {
        return resources.values().stream()
                .filter(resource -> resource.getName().toLowerCase()
                        .contains(nameKeyword.toLowerCase()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Resource> findByDescriptionContaining(String descriptionKeyword) {
        return resources.values().stream()
                .filter(resource -> resource.getDescription().toLowerCase()
                        .contains(descriptionKeyword.toLowerCase()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Resource> findByConditions(ResourceType type, String location, Integer minCapacity,
                                           Boolean isActive, Set<String> requiredFeatures) {
        return resources.values().stream()
                .filter(resource -> type == null || type.equals(resource.getType()))
                .filter(resource -> location == null || location.equals(resource.getLocation()))
                .filter(resource -> minCapacity == null || resource.getCapacity() >= minCapacity)
                .filter(resource -> isActive == null || resource.isActive() == isActive)
                .filter(resource -> requiredFeatures == null || resource.hasAllFeatures(requiredFeatures))
                .collect(Collectors.toList());
    }

    @Override
    public List<Resource> findAvailableResources() {
        return findActiveResources(); // 간단 구현: 활성 리소스 = 예약 가능 리소스
    }

    @Override
    public List<Resource> findAvailableResourcesByType(ResourceType resourceType) {
        return findActiveByType(resourceType);
    }

    @Override
    public List<Resource> findAllOrderByCapacityDesc() {
        return resources.values().stream()
                .sorted((a, b) -> Integer.compare(b.getCapacity(), a.getCapacity()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Resource> findAllOrderByCapacityAsc() {
        return resources.values().stream()
                .sorted((a, b) -> Integer.compare(a.getCapacity(), b.getCapacity()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Resource> findAllOrderByName() {
        return resources.values().stream()
                .sorted((a, b) -> a.getName().compareTo(b.getName()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Resource> findAllOrderByLocationAndName() {
        return resources.values().stream()
                .sorted((a, b) -> {
                    int locationComp = a.getLocation().compareTo(b.getLocation());
                    if (locationComp != 0) {
                        return locationComp;
                    }
                    return a.getName().compareTo(b.getName());
                })
                .collect(Collectors.toList());
    }

    @Override
    public long countAll() {
        return resources.size();
    }

    @Override
    public long countActive() {
        return resources.values().stream()
                .filter(Resource::isActive)
                .count();
    }

    @Override
    public long countByType(ResourceType resourceType) {
        return resources.values().stream()
                .filter(resource -> resourceType.equals(resource.getType()))
                .count();
    }

    @Override
    public long countByLocation(String location) {
        return resources.values().stream()
                .filter(resource -> location.equals(resource.getLocation()))
                .count();
    }

    @Override
    public long getTotalCapacity() {
        return resources.values().stream()
                .mapToLong(Resource::getCapacity)
                .sum();
    }

    @Override
    public long getTotalCapacityByType(ResourceType resourceType) {
        return resources.values().stream()
                .filter(resource -> resourceType.equals(resource.getType()))
                .mapToLong(Resource::getCapacity)
                .sum();
    }

    @Override
    public double getAverageCapacity() {
        return resources.values().stream()
                .mapToInt(Resource::getCapacity)
                .average()
                .orElse(0.0);
    }

    @Override
    public Optional<Resource> findLargestCapacityResource() {
        return resources.values().stream()
                .max((a, b) -> Integer.compare(a.getCapacity(), b.getCapacity()));
    }

    @Override
    public Optional<Resource> findSmallestCapacityResource() {
        return resources.values().stream()
                .min((a, b) -> Integer.compare(a.getCapacity(), b.getCapacity()));
    }

    @Override
    public List<String> findAllLocations() {
        return resources.values().stream()
                .map(Resource::getLocation)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    @Override
    public List<String> findActiveLocations() {
        return resources.values().stream()
                .filter(Resource::isActive)
                .map(Resource::getLocation)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    @Override
    public List<String> findLocationsByType(ResourceType resourceType) {
        return resources.values().stream()
                .filter(resource -> resourceType.equals(resource.getType()))
                .map(Resource::getLocation)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    @Override
    public Set<String> findAllFeatures() {
        return resources.values().stream()
                .flatMap(resource -> resource.getFeatures().stream())
                .collect(Collectors.toSet());
    }

    @Override
    public Set<String> findFeaturesByType(ResourceType resourceType) {
        return resources.values().stream()
                .filter(resource -> resourceType.equals(resource.getType()))
                .flatMap(resource -> resource.getFeatures().stream())
                .collect(Collectors.toSet());
    }

    @Override
    public List<String> findPopularFeatures(int limit) {
        // 기능별 사용 횟수를 계산하여 인기 기능 반환
        Map<String, Long> featureCount = resources.values().stream()
                .flatMap(resource -> resource.getFeatures().stream())
                .collect(Collectors.groupingBy(feature -> feature, Collectors.counting()));

        return featureCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    @Override
    public void saveAll(List<Resource> resourceList) {
        for (Resource resource : resourceList) {
            save(resource);
        }
    }

    @Override
    public List<Resource> findAll(int page, int size) {
        List<Resource> allResources = findAll();
        int start = page * size;
        int end = Math.min(start + size, allResources.size());

        if (start >= allResources.size()) {
            return new ArrayList<>();
        }

        return allResources.subList(start, end);
    }

    @Override
    public List<Resource> findActive(int page, int size) {
        List<Resource> activeResources = findActiveResources();
        int start = page * size;
        int end = Math.min(start + size, activeResources.size());

        if (start >= activeResources.size()) {
            return new ArrayList<>();
        }

        return activeResources.subList(start, end);
    }

    @Override
    public List<Resource> findByType(ResourceType resourceType, int page, int size) {
        List<Resource> typeResources = findByType(resourceType);
        int start = page * size;
        int end = Math.min(start + size, typeResources.size());

        if (start >= typeResources.size()) {
            return new ArrayList<>();
        }

        return typeResources.subList(start, end);
    }

    // 개발/테스트용 유틸리티 메서드들
    public void clear() {
        resources.clear();
    }

    public int size() {
        return resources.size();
    }

    public boolean isEmpty() {
        return resources.isEmpty();
    }
}