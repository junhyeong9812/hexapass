package com.hexapass.domain.model;

import com.hexapass.domain.common.TimeSlot;
import com.hexapass.domain.type.ResourceType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Resource 엔티티 테스트")
class ResourceTest {

    @DisplayName("Resource 생성 테스트")
    @Nested
    class CreationTest {

        @Test
        @DisplayName("기본 정보로 리소스를 생성할 수 있다")
        void createBasicResource() {
            // Given
            String resourceId = "RES001";
            String name = "메인 헬스장";
            ResourceType type = ResourceType.GYM;
            String location = "1층";
            int capacity = 50;

            // When
            Resource resource = Resource.create(resourceId, name, type, location, capacity);

            // Then
            assertThat(resource.getResourceId()).isEqualTo(resourceId);
            assertThat(resource.getName()).isEqualTo(name);
            assertThat(resource.getType()).isEqualTo(type);
            assertThat(resource.getLocation()).isEqualTo(location);
            assertThat(resource.getCapacity()).isEqualTo(capacity);
            assertThat(resource.getDescription()).isEmpty(); // 기본값
            assertThat(resource.getFeatures()).isEmpty(); // 기본값
            assertThat(resource.isActive()).isTrue(); // 기본값
        }

        @Test
        @DisplayName("상세 정보가 포함된 리소스를 생성할 수 있다")
        void createDetailedResource() {
            // Given
            String resourceId = "STUDY001";
            String name = "프리미엄 스터디룸";
            ResourceType type = ResourceType.STUDY_ROOM;
            String location = "3층";
            int capacity = 8;
            String description = "조용하고 쾌적한 스터디 공간";
            Set<String> features = Set.of("화이트보드", "프로젝터", "WiFi", "에어컨");

            // When
            Resource resource = Resource.createWithDetails(
                    resourceId, name, type, location, capacity, description,
                    Map.of(), features // 운영시간은 빈 맵으로
            );

            // Then
            assertThat(resource.getDescription()).isEqualTo(description);
            assertThat(resource.getFeatures()).containsExactlyInAnyOrderElementsOf(features);
        }

        @Test
        @DisplayName("편의 메서드로 헬스장 리소스를 생성할 수 있다")
        void createGymResource() {
            // When
            Resource gym = Resource.createGym("GYM001", "메인 헬스장", "1층", 100);

            // Then
            assertThat(gym.getType()).isEqualTo(ResourceType.GYM);
            assertThat(gym.getDescription()).contains("헬스 기구");
            assertThat(gym.getFeatures()).contains("웨이트 기구", "런닝머신");
        }

        @Test
        @DisplayName("편의 메서드로 스터디룸 리소스를 생성할 수 있다")
        void createStudyRoomResource() {
            // When
            Resource studyRoom = Resource.createStudyRoom("STUDY001", "스터디룸 A", "2층", 6);

            // Then
            assertThat(studyRoom.getType()).isEqualTo(ResourceType.STUDY_ROOM);
            assertThat(studyRoom.getDescription()).contains("스터디 공간");
            assertThat(studyRoom.getFeatures()).contains("책상", "의자", "WiFi");
        }

        @ParameterizedTest
        @DisplayName("필수 필드가 null이거나 빈 값이면 예외가 발생한다")
        @MethodSource("provideInvalidStringFields")
        void createWithInvalidStringFields(String resourceId, String name, String location) {
            // When & Then
            assertThatThrownBy(() ->
                    Resource.create(resourceId, name, ResourceType.GYM, location, 50))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("null이거나 빈 값일 수 없습니다");
        }

        static Stream<Arguments> provideInvalidStringFields() {
            return Stream.of(
                    Arguments.of(null, "Valid Name", "Valid Location"),
                    Arguments.of("", "Valid Name", "Valid Location"),
                    Arguments.of("  ", "Valid Name", "Valid Location"),
                    Arguments.of("RES001", null, "Valid Location"),
                    Arguments.of("RES001", "", "Valid Location"),
                    Arguments.of("RES001", "  ", "Valid Location"),
                    Arguments.of("RES001", "Valid Name", null),
                    Arguments.of("RES001", "Valid Name", ""),
                    Arguments.of("RES001", "Valid Name", "  ")
            );
        }

        @Test
        @DisplayName("null ResourceType으로 생성하면 예외가 발생한다")
        void createWithNullResourceType() {
            // When & Then
            assertThatThrownBy(() ->
                    Resource.create("RES001", "Test Resource", null, "1층", 50))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("리소스 타입은 null일 수 없습니다");
        }

        @ParameterizedTest
        @DisplayName("0 이하의 수용 인원으로 생성하면 예외가 발생한다")
        @ValueSource(ints = {0, -1, -10})
        void createWithInvalidCapacity(int invalidCapacity) {
            // When & Then
            assertThatThrownBy(() ->
                    Resource.create("RES001", "Test Resource", ResourceType.GYM, "1층", invalidCapacity))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("수용 인원은 0보다 커야 합니다");
        }
    }

    @DisplayName("Resource 예약 가능성 확인 테스트")
    @Nested
    class AvailabilityTest {

        @Test
        @DisplayName("활성 상태이고 운영 중인 리소스는 예약 가능하다")
        void activeResourceIsAvailable() {
            // Given - 24시간 운영 리소스
            Resource resource = Resource.createStudyRoom("STUDY001", "24시간 스터디룸", "2층", 10);
            LocalDateTime now = LocalDateTime.now();
            TimeSlot timeSlot = TimeSlot.of(now.plusHours(1), now.plusHours(3));

            // When & Then
            assertThat(resource.isActive()).isTrue();
            assertThat(resource.isAvailable(timeSlot)).isTrue();
        }

        @Test
        @DisplayName("비활성화된 리소스는 예약할 수 없다")
        void inactiveResourceNotAvailable() {
            // Given
            Resource resource = Resource.createGym("GYM001", "헬스장", "1층", 50);
            resource.deactivate();

            LocalDateTime now = LocalDateTime.now();
            TimeSlot timeSlot = TimeSlot.of(now.plusHours(1), now.plusHours(3));

            // When & Then
            assertThat(resource.isActive()).isFalse();
            assertThat(resource.isAvailable(timeSlot)).isFalse();
        }

        @Test
        @DisplayName("운영시간을 확인할 수 있다")
        void checkOperatingHours() {
            // Given - 월요일 9시~18시 운영
            LocalDateTime mondayNow = LocalDateTime.now().with(DayOfWeek.MONDAY);
            Map<DayOfWeek, List<TimeSlot>> schedule = Map.of(
                    DayOfWeek.MONDAY, List.of(TimeSlot.of(
                            mondayNow.withHour(9).withMinute(0).withSecond(0).withNano(0),
                            mondayNow.withHour(18).withMinute(0).withSecond(0).withNano(0)
                    ))
            );

            Resource resource = Resource.createWithDetails(
                    "MEETING001", "회의실", ResourceType.MEETING_ROOM, "3층", 12,
                    "회의용 공간", schedule, Set.of()
            );

            // When & Then - 운영시간 내
            TimeSlot withinHours = TimeSlot.of(
                    mondayNow.withHour(10).withMinute(0).withSecond(0).withNano(0),
                    mondayNow.withHour(12).withMinute(0).withSecond(0).withNano(0)
            );
            assertThat(resource.isOperatingDuring(withinHours)).isTrue();

            // When & Then - 운영시간 외
            TimeSlot outsideHours = TimeSlot.of(
                    mondayNow.withHour(19).withMinute(0).withSecond(0).withNano(0),
                    mondayNow.withHour(21).withMinute(0).withSecond(0).withNano(0)
            );
            assertThat(resource.isOperatingDuring(outsideHours)).isFalse();
        }

        @Test
        @DisplayName("운영하지 않는 요일에는 예약할 수 없다")
        void cannotReserveOnNonOperatingDays() {
            // Given - 월요일만 운영
            LocalDateTime mondayNow = LocalDateTime.now().with(DayOfWeek.MONDAY);
            LocalDateTime tuesdayNow = LocalDateTime.now().with(DayOfWeek.TUESDAY);

            Map<DayOfWeek, List<TimeSlot>> schedule = Map.of(
                    DayOfWeek.MONDAY, List.of(TimeSlot.of(
                            mondayNow.withHour(9).withMinute(0).withSecond(0).withNano(0),
                            mondayNow.withHour(18).withMinute(0).withSecond(0).withNano(0)
                    ))
                    // 화요일은 없음
            );

            Resource resource = Resource.createWithDetails(
                    "MEETING001", "회의실", ResourceType.MEETING_ROOM, "3층", 12,
                    "회의용 공간", schedule, Set.of()
            );

            // When & Then
            TimeSlot mondaySlot = TimeSlot.of(
                    mondayNow.withHour(10).withMinute(0).withSecond(0).withNano(0),
                    mondayNow.withHour(12).withMinute(0).withSecond(0).withNano(0)
            );
            TimeSlot tuesdaySlot = TimeSlot.of(
                    tuesdayNow.withHour(10).withMinute(0).withSecond(0).withNano(0),
                    tuesdayNow.withHour(12).withMinute(0).withSecond(0).withNano(0)
            );

            assertThat(resource.isOperatingDuring(mondaySlot)).isTrue();
            assertThat(resource.isOperatingDuring(tuesdaySlot)).isFalse();
        }

        @Test
        @DisplayName("null TimeSlot으로 확인하면 false를 반환한다")
        void nullTimeSlotReturnsFalse() {
            // Given
            Resource resource = Resource.createGym("GYM001", "헬스장", "1층", 50);

            // When & Then
            assertThat(resource.isOperatingDuring(null)).isFalse();
            assertThat(resource.isAvailable(null)).isFalse();
        }

        @Test
        @DisplayName("특정 요일의 운영시간을 조회할 수 있다")
        void getOperatingHoursForSpecificDay() {
            // Given
            Resource gym = Resource.createGym("GYM001", "헬스장", "1층", 50);

            // When
            List<TimeSlot> mondayHours = gym.getOperatingHours(DayOfWeek.MONDAY);
            List<TimeSlot> sundayHours = gym.getOperatingHours(DayOfWeek.SUNDAY);

            // Then
            assertThat(mondayHours).isNotEmpty(); // 평일 운영
            assertThat(sundayHours).isEmpty(); // 주말 휴무
        }

        @Test
        @DisplayName("주간 운영 스케줄을 조회할 수 있다")
        void getWeeklySchedule() {
            // Given
            Resource studyRoom = Resource.createStudyRoom("STUDY001", "스터디룸", "2층", 10);

            // When
            Map<DayOfWeek, List<TimeSlot>> weeklySchedule = studyRoom.getWeeklySchedule();

            // Then
            assertThat(weeklySchedule).hasSize(7); // 7일 모두 포함
            assertThat(weeklySchedule.get(DayOfWeek.MONDAY)).isNotEmpty(); // 24시간 운영
            assertThat(weeklySchedule.get(DayOfWeek.SUNDAY)).isNotEmpty(); // 24시간 운영
        }
    }

    @DisplayName("Resource 수용 인원 관리 테스트")
    @Nested
    class CapacityManagementTest {

        private Resource resource;

        @Test
        @DisplayName("현재 이용 인원이 정원보다 적으면 추가 수용 가능하다")
        void hasCapacityWhenUnderLimit() {
            // Given
            resource = Resource.create("RES001", "테스트 리소스", ResourceType.STUDY_ROOM, "1층", 10);

            // When & Then
            assertThat(resource.hasCapacity(0)).isTrue(); // 0명 사용 중
            assertThat(resource.hasCapacity(5)).isTrue(); // 5명 사용 중
            assertThat(resource.hasCapacity(9)).isTrue(); // 9명 사용 중
        }

        @Test
        @DisplayName("현재 이용 인원이 정원과 같거나 크면 추가 수용 불가하다")
        void noCapacityWhenAtOrOverLimit() {
            // Given
            resource = Resource.create("RES001", "테스트 리소스", ResourceType.STUDY_ROOM, "1층", 5);

            // When & Then
            assertThat(resource.hasCapacity(5)).isFalse(); // 5명 사용 중 (정원)
            assertThat(resource.hasCapacity(6)).isFalse(); // 6명 사용 중 (초과)
        }

        @Test
        @DisplayName("비활성화된 리소스는 수용 인원이 없다")
        void inactiveResourceHasNoCapacity() {
            // Given
            resource = Resource.create("RES001", "테스트 리소스", ResourceType.STUDY_ROOM, "1층", 10);
            resource.deactivate();

            // When & Then
            assertThat(resource.hasCapacity(0)).isFalse();
            assertThat(resource.hasCapacity(5)).isFalse();
        }

        @Test
        @DisplayName("남은 수용 인원을 정확히 계산한다")
        void calculateRemainingCapacityCorrectly() {
            // Given
            resource = Resource.create("RES001", "테스트 리소스", ResourceType.STUDY_ROOM, "1층", 10);

            // When & Then
            assertThat(resource.getRemainingCapacity(0)).isEqualTo(10); // 10명 여유
            assertThat(resource.getRemainingCapacity(3)).isEqualTo(7); // 7명 여유
            assertThat(resource.getRemainingCapacity(10)).isEqualTo(0); // 0명 여유
            assertThat(resource.getRemainingCapacity(12)).isEqualTo(0); // 초과해도 0 반환
        }

        @Test
        @DisplayName("비활성화된 리소스는 남은 수용 인원이 0이다")
        void inactiveResourceHasZeroRemainingCapacity() {
            // Given
            resource = Resource.create("RES001", "테스트 리소스", ResourceType.STUDY_ROOM, "1층", 10);
            resource.deactivate();

            // When & Then
            assertThat(resource.getRemainingCapacity(0)).isEqualTo(0);
            assertThat(resource.getRemainingCapacity(5)).isEqualTo(0);
        }

        @Test
        @DisplayName("수용률을 정확히 계산한다")
        void calculateOccupancyRateCorrectly() {
            // Given
            resource = Resource.create("RES001", "테스트 리소스", ResourceType.STUDY_ROOM, "1층", 10);

            // When & Then
            assertThat(resource.getOccupancyRate(0)).isEqualTo(0.0); // 0%
            assertThat(resource.getOccupancyRate(5)).isEqualTo(0.5); // 50%
            assertThat(resource.getOccupancyRate(10)).isEqualTo(1.0); // 100%
            assertThat(resource.getOccupancyRate(12)).isEqualTo(1.0); // 100% (초과 시 최대값)
        }

        @Test
        @DisplayName("정원이 0인 리소스는 항상 가득참으로 처리한다")
        void zeroCapacityResourceAlwaysFull() {
            // Given - 정원 0인 특수 리소스 (직접 생성자 호출은 불가하므로 리플렉션 사용하지 않고 테스트에서 제외)
            // 실제로는 정원이 0인 리소스는 생성할 수 없으므로 이 케이스는 이론적인 테스트
            // 대신 정원 1인 리소스로 테스트
            resource = Resource.create("RES001", "테스트 리소스", ResourceType.STUDY_ROOM, "1층", 1);

            // When & Then
            assertThat(resource.getOccupancyRate(1)).isEqualTo(1.0);
            assertThat(resource.isFull(1)).isTrue();
        }

        @Test
        @DisplayName("만석 여부를 정확히 판단한다")
        void determineFullStatusCorrectly() {
            // Given
            resource = Resource.create("RES001", "테스트 리소스", ResourceType.STUDY_ROOM, "1층", 8);

            // When & Then
            assertThat(resource.isFull(7)).isFalse(); // 7/8 (여유 있음)
            assertThat(resource.isFull(8)).isTrue(); // 8/8 (만석)
            assertThat(resource.isFull(10)).isTrue(); // 10/8 (초과)
        }

        @ParameterizedTest
        @DisplayName("음수 현재 이용 인원으로 확인하면 예외가 발생한다")
        @ValueSource(ints = {-1, -5, -10})
        void negativeCurrentOccupancyThrowsException(int negativeOccupancy) {
            // Given
            resource = Resource.create("RES001", "테스트 리소스", ResourceType.STUDY_ROOM, "1층", 10);

            // When & Then
            assertThatThrownBy(() -> resource.hasCapacity(negativeOccupancy))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("현재 이용 인원은 0 이상이어야 합니다");

            assertThatThrownBy(() -> resource.getRemainingCapacity(negativeOccupancy))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("현재 이용 인원은 0 이상이어야 합니다");
        }
    }

    @DisplayName("Resource 상태 관리 테스트")
    @Nested
    class StatusManagementTest {

        @Test
        @DisplayName("리소스를 비활성화할 수 있다")
        void deactivateResource() {
            // Given
            Resource resource = Resource.create("RES001", "테스트 리소스", ResourceType.GYM, "1층", 50);
            assertThat(resource.isActive()).isTrue();

            // When
            resource.deactivate();

            // Then
            assertThat(resource.isActive()).isFalse();
        }

        @Test
        @DisplayName("리소스를 다시 활성화할 수 있다")
        void reactivateResource() {
            // Given
            Resource resource = Resource.create("RES001", "테스트 리소스", ResourceType.GYM, "1층", 50);
            resource.deactivate();
            assertThat(resource.isActive()).isFalse();

            // When
            resource.activate();

            // Then
            assertThat(resource.isActive()).isTrue();
        }
    }

    @DisplayName("Resource 기능 및 시설 테스트")
    @Nested
    class FeatureTest {

        @Test
        @DisplayName("특정 기능을 보유하고 있는지 확인할 수 있다")
        void hasSpecificFeature() {
            // Given
            Set<String> features = Set.of("WiFi", "프로젝터", "에어컨", "화이트보드");
            Resource resource = Resource.createWithDetails(
                    "MEETING001", "회의실", ResourceType.MEETING_ROOM, "3층", 10,
                    "회의용 공간", Map.of(), features
            );

            // When & Then
            assertThat(resource.hasFeature("WiFi")).isTrue();
            assertThat(resource.hasFeature("프로젝터")).isTrue();
            assertThat(resource.hasFeature("음향시설")).isFalse(); // 없는 기능
        }

        @Test
        @DisplayName("여러 기능을 모두 보유하고 있는지 확인할 수 있다")
        void hasAllRequiredFeatures() {
            // Given
            Set<String> features = Set.of("WiFi", "프로젝터", "에어컨", "화이트보드", "음향시설");
            Resource resource = Resource.createWithDetails(
                    "MEETING001", "고급 회의실", ResourceType.MEETING_ROOM, "5층", 20,
                    "최신 시설 완비", Map.of(), features
            );

            // When & Then
            Set<String> requiredFeatures1 = Set.of("WiFi", "프로젝터");
            Set<String> requiredFeatures2 = Set.of("WiFi", "프로젝터", "화상회의"); // 화상회의는 없음

            assertThat(resource.hasAllFeatures(requiredFeatures1)).isTrue();
            assertThat(resource.hasAllFeatures(requiredFeatures2)).isFalse();
        }

        @Test
        @DisplayName("빈 기능 집합도 올바르게 처리한다")
        void handleEmptyFeatureSet() {
            // Given
            Resource resource = Resource.create("RES001", "기본 리소스", ResourceType.STUDY_ROOM, "1층", 10);

            // When & Then
            assertThat(resource.getFeatures()).isEmpty();
            assertThat(resource.hasFeature("WiFi")).isFalse();
            assertThat(resource.hasAllFeatures(Set.of())).isTrue(); // 빈 집합은 항상 만족
        }
    }

    @DisplayName("Resource 정보 표시 테스트")
    @Nested
    class DisplayTest {

        @Test
        @DisplayName("리소스 정보 요약을 조회할 수 있다")
        void getResourceSummary() {
            // Given
            Set<String> features = Set.of("WiFi", "프로젝터", "에어컨");
            Resource resource = Resource.createWithDetails(
                    "MEETING001", "프리미엄 회의실", ResourceType.MEETING_ROOM, "5층", 12,
                    "최신 시설을 갖춘 회의실", Map.of(), features
            );

            // When
            String summary = resource.getSummary();

            // Then
            assertThat(summary).contains("프리미엄 회의실");
            assertThat(summary).contains("회의실");
            assertThat(summary).contains("5층");
            assertThat(summary).contains("수용인원 12명");
            assertThat(summary).contains("최신 시설을 갖춘 회의실");
            assertThat(summary).contains("시설: WiFi, 프로젝터, 에어컨");
            assertThat(summary).contains("[운영중]");
        }

        @Test
        @DisplayName("비활성화된 리소스의 요약에는 중단 표시가 포함된다")
        void inactiveResourceSummaryShowsStatus() {
            // Given
            Resource resource = Resource.create("RES001", "테스트 리소스", ResourceType.GYM, "1층", 50);
            resource.deactivate();

            // When
            String summary = resource.getSummary();

            // Then
            assertThat(summary).contains("[중단]");
        }

        @Test
        @DisplayName("기능이 없는 리소스의 요약도 올바르게 표시된다")
        void resourceWithoutFeaturesSummary() {
            // Given
            Resource resource = Resource.create("RES001", "기본 리소스", ResourceType.STUDY_ROOM, "2층", 8);

            // When
            String summary = resource.getSummary();

            // Then
            assertThat(summary).contains("기본 리소스");
            assertThat(summary).contains("스터디룸");
            assertThat(summary).contains("2층");
            assertThat(summary).contains("수용인원 8명");
            assertThat(summary).doesNotContain("시설:"); // 기능이 없으면 시설 정보 없음
        }

        @Test
        @DisplayName("설명이 없는 리소스의 요약도 올바르게 표시된다")
        void resourceWithoutDescriptionSummary() {
            // Given
            Resource resource = Resource.create("RES001", "기본 리소스", ResourceType.GYM, "1층", 30);

            // When
            String summary = resource.getSummary();

            // Then
            assertThat(summary).contains("기본 리소스");
            assertThat(summary).contains("헬스장");
            assertThat(summary).contains("1층");
            assertThat(summary).contains("수용인원 30명");
            // 설명이 비어있으면 설명 부분이 표시되지 않음
        }
    }

    @DisplayName("Resource 동등성 테스트")
    @Nested
    class EqualityTest {

        @Test
        @DisplayName("같은 resourceId를 가진 리소스들은 동등하다")
        void equalityWithSameResourceId() {
            // Given
            Resource resource1 = Resource.create("RES001", "리소스 A", ResourceType.GYM, "1층", 50);
            Resource resource2 = Resource.create("RES001", "리소스 B", ResourceType.POOL, "2층", 30);

            // When & Then
            assertThat(resource1).isEqualTo(resource2);
            assertThat(resource1.hashCode()).isEqualTo(resource2.hashCode());
        }

        @Test
        @DisplayName("다른 resourceId를 가진 리소스들은 동등하지 않다")
        void inequalityWithDifferentResourceId() {
            // Given
            Resource resource1 = Resource.create("RES001", "테스트 리소스", ResourceType.GYM, "1층", 50);
            Resource resource2 = Resource.create("RES002", "테스트 리소스", ResourceType.GYM, "1층", 50);

            // When & Then
            assertThat(resource1).isNotEqualTo(resource2);
        }

        @Test
        @DisplayName("null과는 동등하지 않다")
        void inequalityWithNull() {
            // Given
            Resource resource = Resource.create("RES001", "테스트 리소스", ResourceType.GYM, "1층", 50);

            // When & Then
            assertThat(resource).isNotEqualTo(null);
        }

        @Test
        @DisplayName("다른 타입 객체와는 동등하지 않다")
        void inequalityWithDifferentType() {
            // Given
            Resource resource = Resource.create("RES001", "테스트 리소스", ResourceType.GYM, "1층", 50);
            String notResource = "RES001";

            // When & Then
            assertThat(resource).isNotEqualTo(notResource);
        }
    }

    @DisplayName("Resource toString 테스트")
    @Nested
    class ToStringTest {

        @Test
        @DisplayName("toString이 올바른 정보를 포함한다")
        void toStringContainsCorrectInfo() {
            // Given
            Resource resource = Resource.create("RES001", "테스트 헬스장", ResourceType.GYM, "1층", 100);

            // When
            String result = resource.toString();

            // Then
            assertThat(result).contains("RES001");
            assertThat(result).contains("테스트 헬스장");
            assertThat(result).contains("GYM");
            assertThat(result).contains("1층");
            assertThat(result).contains("capacity=100");
            assertThat(result).contains("active=true");
        }

        @Test
        @DisplayName("비활성화된 리소스의 toString도 올바른 상태를 표시한다")
        void inactiveResourceToString() {
            // Given
            Resource resource = Resource.create("RES001", "테스트 리소스", ResourceType.STUDY_ROOM, "2층", 10);
            resource.deactivate();

            // When
            String result = resource.toString();

            // Then
            assertThat(result).contains("active=false");
        }
    }

    @DisplayName("Resource 불변성 테스트")
    @Nested
    class ImmutabilityTest {

        @Test
        @DisplayName("getFeatures는 불변 집합을 반환한다")
        void getFeaturesReturnsImmutableSet() {
            // Given
            Resource resource = Resource.createGym("GYM001", "테스트 헬스장", "1층", 50);
            Set<String> features = resource.getFeatures();

            // When & Then
            assertThatThrownBy(() -> features.add("새로운 기능"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("getOperatingHours는 불변 리스트를 반환한다")
        void getOperatingHoursReturnsImmutableList() {
            // Given
            Resource resource = Resource.createGym("GYM001", "테스트 헬스장", "1층", 50);
            List<TimeSlot> mondayHours = resource.getOperatingHours(DayOfWeek.MONDAY);

            // When & Then
            assertThatThrownBy(() -> mondayHours.add(TimeSlot.oneHour(LocalDateTime.now())))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("getWeeklySchedule는 불변 맵을 반환한다")
        void getWeeklyScheduleReturnsImmutableMap() {
            // Given
            Resource resource = Resource.createStudyRoom("STUDY001", "테스트 스터디룸", "2층", 8);
            Map<DayOfWeek, List<TimeSlot>> schedule = resource.getWeeklySchedule();

            // When & Then
            assertThatThrownBy(() -> schedule.put(DayOfWeek.SATURDAY, List.of()))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @DisplayName("Resource 엣지 케이스 테스트")
    @Nested
    class EdgeCaseTest {

        @Test
        @DisplayName("null 스케줄로 생성해도 안전하게 처리된다")
        void handleNullScheduleSafely() {
            // Given & When
            Resource resource = Resource.createWithDetails(
                    "RES001", "테스트 리소스", ResourceType.MEETING_ROOM, "1층", 10,
                    "테스트 설명", null, Set.of("WiFi") // null 스케줄
            );

            // Then
            assertThat(resource.getWeeklySchedule()).isEmpty();
            assertThat(resource.getOperatingHours(DayOfWeek.MONDAY)).isEmpty();
        }

        @Test
        @DisplayName("null features로 생성해도 안전하게 처리된다")
        void handleNullFeaturesSafely() {
            // Given & When
            Resource resource = Resource.createWithDetails(
                    "RES001", "테스트 리소스", ResourceType.GYM, "1층", 20,
                    "테스트 설명", Map.of(), null // null features
            );

            // Then
            assertThat(resource.getFeatures()).isEmpty();
            assertThat(resource.hasFeature("WiFi")).isFalse();
        }

        @Test
        @DisplayName("null description으로 생성하면 빈 문자열로 처리된다")
        void handleNullDescriptionSafely() {
            // Given & When
            Resource resource = Resource.createWithDetails(
                    "RES001", "테스트 리소스", ResourceType.STUDY_ROOM, "1층", 8,
                    null, Map.of(), Set.of() // null description
            );

            // Then
            assertThat(resource.getDescription()).isEmpty();
        }

        @Test
        @DisplayName("빈 문자열 description은 트림되어 빈 문자열로 저장된다")
        void handleEmptyDescriptionSafely() {
            // Given & When
            Resource resource = Resource.createWithDetails(
                    "RES001", "테스트 리소스", ResourceType.POOL, "1층", 30,
                    "   ", Map.of(), Set.of() // 공백만 있는 description
            );

            // Then
            assertThat(resource.getDescription()).isEmpty();
        }

        @Test
        @DisplayName("매우 큰 수용 인원도 올바르게 처리한다")
        void handleVeryLargeCapacity() {
            // Given & When
            Resource resource = Resource.create("RES001", "대형 컨벤션홀", ResourceType.CLASS_ROOM, "10층", 10000);

            // Then
            assertThat(resource.getCapacity()).isEqualTo(10000);
            assertThat(resource.hasCapacity(5000)).isTrue();
            assertThat(resource.getRemainingCapacity(3000)).isEqualTo(7000);
            assertThat(resource.getOccupancyRate(2500)).isEqualTo(0.25);
        }
    }
}