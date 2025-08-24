package com.hexapass.domain.scenario;

import com.hexapass.domain.common.DateRange;
import com.hexapass.domain.common.Money;
import com.hexapass.domain.common.TimeSlot;
import com.hexapass.domain.model.*;
import com.hexapass.domain.type.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@DisplayName("멤버십 시스템 통합 시나리오 테스트")
class MembershipIntegrationTest {

    @DisplayName("완전한 회원가입-멤버십-예약 시나리오")
    @Nested
    class FullMembershipScenarioTest {

        @Test
        @DisplayName("신규 회원이 가입부터 예약 완료까지 전체 프로세스를 수행할 수 있다")
        void completeNewMemberJourney() {
            // Given: 회원 생성
            Member member = Member.create("M001", "김헥사", "hexpass@example.com", "010-1234-5678");
            assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);

            // And: 멤버십 플랜 선택
            MembershipPlan monthlyPlan = MembershipPlan.premiumMonthly();
            DateRange membershipPeriod = DateRange.fromTodayFor(30);

            // When: 멤버십 할당
            member.assignMembership(monthlyPlan, membershipPeriod);

            // Then: 멤버십 할당 확인
            assertThat(member.getCurrentPlan()).isEqualTo(monthlyPlan);
            assertThat(member.getMembershipPeriod()).isEqualTo(membershipPeriod);
            assertThat(member.hasMembershipActive()).isTrue();

            // And: 예약 권한 확인
            LocalDate tomorrowDate = LocalDate.now().plusDays(1);
            assertThat(member.canReserve(ResourceType.GYM, tomorrowDate)).isTrue();
            assertThat(member.canReserve(ResourceType.POOL, tomorrowDate)).isTrue();
            assertThat(member.canReserve(ResourceType.TENNIS_COURT, tomorrowDate)).isFalse(); // 권한 없는 리소스

            // Given: 리소스 생성
            Resource gymResource = Resource.createGym("GYM_001", "메인 헬스장", "1층", 50);
            assertThat(gymResource.isActive()).isTrue();

            // And: 예약 시간대 설정
            LocalDateTime reservationStart = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
            TimeSlot reservationSlot = TimeSlot.of(reservationStart, reservationStart.plusHours(2));

            // When: 예약 생성
            Reservation reservation = Reservation.create("R001", member.getMemberId(), gymResource.getResourceId(), reservationSlot);

            // Then: 예약 상태 확인
            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.REQUESTED);
            assertThat(reservation.isActive()).isFalse(); // 아직 요청 단계

            // When: 예약 확정
            reservation.confirm();

            // Then: 예약 확정 상태 확인
            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
            assertThat(reservation.isActive()).isTrue();
            assertThat(reservation.getConfirmedAt()).isNotNull();

            // When: 예약 시간에 사용 시작
            reservation.startUsing();

            // Then: 사용 중 상태 확인
            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.IN_USE);
            assertThat(reservation.isActive()).isTrue();
            assertThat(reservation.getStartedAt()).isNotNull();

            // When: 사용 완료
            reservation.complete();

            // Then: 완료 상태 확인
            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.COMPLETED);
            assertThat(reservation.isActive()).isFalse();
            assertThat(reservation.isFinal()).isTrue();
            assertThat(reservation.getCompletedAt()).isNotNull();

            // Final: 전체 시나리오 검증
            assertThat(reservation.getStatusHistory()).hasSize(4); // 생성->확정->사용->완료
            assertThat(reservation.getDurationMinutes()).isEqualTo(120L); // 2시간 사용
        }

        @Test
        @DisplayName("회원이 멤버십 없이 예약하려 하면 실패한다")
        void memberWithoutMembershipCannotReserve() {
            // Given: 멤버십이 없는 회원
            Member member = Member.create("M002", "김노멤버", "nomember@example.com", "010-9876-5432");

            // When & Then: 예약 권한 확인
            LocalDate tomorrow = LocalDate.now().plusDays(1);
            assertThat(member.canMakeReservation()).isFalse();
            assertThat(member.canReserve(ResourceType.GYM, tomorrow)).isFalse();
            assertThat(member.hasMembershipActive()).isFalse();
        }

        @Test
        @DisplayName("만료된 멤버십으로는 예약할 수 없다")
        void expiredMembershipCannotReserve() {
            // Given: 만료된 멤버십을 가진 회원
            Member member = Member.create("M003", "김만료", "expired@example.com", "010-1111-2222");
            MembershipPlan plan = MembershipPlan.basicMonthly();
            DateRange expiredPeriod = DateRange.of(LocalDate.now().minusDays(60), LocalDate.now().minusDays(30));

            // When: 만료된 멤버십 할당
            member.assignMembership(plan, expiredPeriod);

            // Then: 예약 불가 확인
            assertThat(member.isMembershipExpired()).isTrue();
            assertThat(member.hasMembershipActive()).isFalse();
            assertThat(member.canMakeReservation()).isFalse();
        }
    }

    @DisplayName("예약 충돌 및 리소스 관리 시나리오")
    @Nested
    class ReservationConflictScenarioTest {

        @Test
        @DisplayName("같은 시간대에 같은 리소스를 예약하면 충돌이 감지된다")
        void detectReservationConflict() {
            // Given: 두 명의 회원
            Member member1 = Member.create("M001", "회원1", "member1@test.com", "010-1111-1111");
            Member member2 = Member.create("M002", "회원2", "member2@test.com", "010-2222-2222");

            MembershipPlan plan = MembershipPlan.basicMonthly();
            DateRange period = DateRange.fromTodayFor(30);

            member1.assignMembership(plan, period);
            member2.assignMembership(plan, period);

            // And: 같은 리소스와 시간대
            Resource studyRoom = Resource.createStudyRoom("STUDY_001", "스터디룸 A", "2층", 4);
            LocalDateTime reservationTime = LocalDateTime.now().plusDays(1).withHour(14).withMinute(0).withSecond(0).withNano(0);
            TimeSlot timeSlot = TimeSlot.of(reservationTime, reservationTime.plusHours(2));

            // When: 두 예약 생성
            Reservation reservation1 = Reservation.create("R001", member1.getMemberId(), studyRoom.getResourceId(), timeSlot);
            Reservation reservation2 = Reservation.create("R002", member2.getMemberId(), studyRoom.getResourceId(), timeSlot);

            // Then: 충돌 감지
            assertThat(reservation1.conflictsWith(reservation2)).isTrue();
            assertThat(reservation2.conflictsWith(reservation1)).isTrue();
        }

        @Test
        @DisplayName("리소스의 운영시간 외에는 예약할 수 없다")
        void cannotReserveOutsideOperatingHours() {
            // Given: 평일 오전 9시~오후 6시만 운영하는 회의실
            Resource meetingRoom = Resource.createWithDetails(
                    "MEETING_001", "회의실 A", ResourceType.MEETING_ROOM, "3층", 8,
                    "프로젝터와 화이트보드 구비",
                    java.util.Map.of(
                            java.time.DayOfWeek.MONDAY, java.util.List.of(
                                    TimeSlot.of(
                                            LocalDateTime.now().with(java.time.DayOfWeek.MONDAY).withHour(9).withMinute(0).withSecond(0).withNano(0),
                                            LocalDateTime.now().with(java.time.DayOfWeek.MONDAY).withHour(18).withMinute(0).withSecond(0).withNano(0)
                                    )
                            )
                    ),
                    Set.of("프로젝터", "화이트보드", "WiFi")
            );

            // When & Then: 운영시간 확인
            LocalDateTime mondayMorning = LocalDateTime.now().with(java.time.DayOfWeek.MONDAY).withHour(10).withMinute(0);
            LocalDateTime mondayEvening = LocalDateTime.now().with(java.time.DayOfWeek.MONDAY).withHour(20).withMinute(0);

            TimeSlot validSlot = TimeSlot.of(mondayMorning, mondayMorning.plusHours(1));
            TimeSlot invalidSlot = TimeSlot.of(mondayEvening, mondayEvening.plusHours(1));

            assertThat(meetingRoom.isOperatingDuring(validSlot)).isTrue();
            assertThat(meetingRoom.isOperatingDuring(invalidSlot)).isFalse();
        }

        @Test
        @DisplayName("리소스 정원을 초과하면 추가 예약을 할 수 없다")
        void cannotExceedResourceCapacity() {
            // Given: 정원 2명인 소규모 스터디룸
            Resource smallRoom = Resource.createStudyRoom("STUDY_SMALL", "소형 스터디룸", "3층", 2);

            // When & Then: 정원 확인
            assertThat(smallRoom.hasCapacity(0)).isTrue(); // 0명 사용 중
            assertThat(smallRoom.hasCapacity(1)).isTrue(); // 1명 사용 중
            assertThat(smallRoom.hasCapacity(2)).isFalse(); // 2명 사용 중 (정원 초과)
            assertThat(smallRoom.hasCapacity(3)).isFalse(); // 3명 사용 중

            assertThat(smallRoom.getRemainingCapacity(0)).isEqualTo(2);
            assertThat(smallRoom.getRemainingCapacity(1)).isEqualTo(1);
            assertThat(smallRoom.getRemainingCapacity(2)).isEqualTo(0);

            assertThat(smallRoom.isFull(2)).isTrue();
            assertThat(smallRoom.isFull(1)).isFalse();
        }
    }

    @DisplayName("회원 상태 관리 시나리오")
    @Nested
    class MemberStatusScenarioTest {

        @Test
        @DisplayName("정지된 회원은 예약할 수 없다")
        void suspendedMemberCannotReserve() {
            // Given: 활성 멤버십을 가진 회원
            Member member = Member.create("M001", "김정지", "suspended@test.com", "010-3333-3333");
            MembershipPlan plan = MembershipPlan.basicMonthly();
            DateRange period = DateRange.fromTodayFor(30);
            member.assignMembership(plan, period);

            // 초기 상태 확인
            assertThat(member.canMakeReservation()).isTrue();

            // When: 회원 정지
            member.suspend("규정 위반");

            // Then: 예약 불가 확인
            assertThat(member.getStatus()).isEqualTo(MemberStatus.SUSPENDED);
            assertThat(member.getSuspensionReason()).isEqualTo("규정 위반");
            assertThat(member.canMakeReservation()).isFalse();

            LocalDate tomorrow = LocalDate.now().plusDays(1);
            assertThat(member.canReserve(ResourceType.GYM, tomorrow)).isFalse();
        }

        @Test
        @DisplayName("정지된 회원을 다시 활성화할 수 있다")
        void reactivateSuspendedMember() {
            // Given: 정지된 회원
            Member member = Member.create("M001", "김재활성", "reactivate@test.com", "010-4444-4444");
            MembershipPlan plan = MembershipPlan.basicMonthly();
            DateRange period = DateRange.fromTodayFor(30);
            member.assignMembership(plan, period);
            member.suspend("테스트 정지");

            // When: 회원 활성화
            member.activate();

            // Then: 예약 가능 확인
            assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
            assertThat(member.getSuspensionReason()).isNull(); // 정지 사유 초기화
            assertThat(member.canMakeReservation()).isTrue();
        }

        @Test
        @DisplayName("탈퇴한 회원은 어떤 작업도 할 수 없다")
        void withdrawnMemberCannotDoAnything() {
            // Given: 활성 회원
            Member member = Member.create("M001", "김탈퇴", "withdraw@test.com", "010-5555-5555");
            MembershipPlan plan = MembershipPlan.basicMonthly();
            DateRange period = DateRange.fromTodayFor(30);
            member.assignMembership(plan, period);

            // When: 회원 탈퇴
            member.withdraw();

            // Then: 모든 활동 불가 확인
            assertThat(member.getStatus()).isEqualTo(MemberStatus.WITHDRAWN);
            assertThat(member.canMakeReservation()).isFalse();

            // 탈퇴한 회원은 재활성화 불가
            assertThatThrownBy(() -> member.activate())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("탈퇴한 회원은 활성화할 수 없습니다");

            // 탈퇴한 회원은 재정지 불가
            assertThatThrownBy(() -> member.suspend("테스트"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("탈퇴한 회원은 정지할 수 없습니다");
        }
    }

    @DisplayName("멤버십 플랜 업그레이드 시나리오")
    @Nested
    class PlanUpgradeScenarioTest {

        @Test
        @DisplayName("회원이 상위 플랜으로 업그레이드할 수 있다")
        void memberCanUpgradePlan() {
            // Given: 기본 플랜 회원
            Member member = Member.create("M001", "김업그레이드", "upgrade@test.com", "010-6666-6666");
            MembershipPlan basicPlan = MembershipPlan.basicMonthly();
            MembershipPlan premiumPlan = MembershipPlan.premiumMonthly();
            DateRange period = DateRange.fromTodayFor(30);

            member.assignMembership(basicPlan, period);

            // 기본 플랜으로는 POOL 이용 불가 확인
            LocalDate tomorrow = LocalDate.now().plusDays(1);
            assertThat(member.canReserve(ResourceType.GYM, tomorrow)).isTrue();
            assertThat(member.canReserve(ResourceType.POOL, tomorrow)).isFalse();

            // When: 프리미엄 플랜으로 변경
            member.changePlan(premiumPlan);

            // Then: 추가 권한 확인
            assertThat(member.getCurrentPlan()).isEqualTo(premiumPlan);
            assertThat(member.canReserve(ResourceType.GYM, tomorrow)).isTrue();
            assertThat(member.canReserve(ResourceType.POOL, tomorrow)).isTrue();
            assertThat(member.canReserve(ResourceType.SAUNA, tomorrow)).isTrue();

            // 업그레이드 비용 계산 확인
            Money upgradeCost = basicPlan.calculateUpgradeCost(premiumPlan, 15); // 15일 남음
            assertThat(upgradeCost.isPositive()).isTrue();
        }

        @Test
        @DisplayName("플랜별 예약 제한이 올바르게 적용된다")
        void planReservationLimitsApplied() {
            // Given: VIP 플랜 (최대 동시 예약 10개)
            Member vipMember = Member.create("M001", "김VIP", "vip@test.com", "010-7777-7777");
            MembershipPlan vipPlan = MembershipPlan.vipYearly();
            DateRange period = DateRange.fromTodayFor(365);
            vipMember.assignMembership(vipPlan, period);

            // When & Then: 예약 제한 확인
            assertThat(vipPlan.getMaxSimultaneousReservations()).isEqualTo(10);
            assertThat(vipMember.canReserveSimultaneously(5)).isTrue(); // 5개 예약 중
            assertThat(vipMember.canReserveSimultaneously(10)).isFalse(); // 10개 예약 중 (한계)

            // 선예약 제한 확인
            assertThat(vipPlan.getMaxAdvanceReservationDays()).isEqualTo(90);
            assertThat(vipMember.canReserveInAdvance(LocalDate.now().plusDays(60))).isTrue();
            assertThat(vipMember.canReserveInAdvance(LocalDate.now().plusDays(100))).isFalse();
        }
    }

    @DisplayName("예약 취소 및 환불 시나리오")
    @Nested
    class CancellationRefundScenarioTest {

        @Test
        @DisplayName("예약을 취소할 수 있다")
        void canCancelReservation() {
            // Given: 확정된 예약
            Member member = Member.create("M001", "김취소", "cancel@test.com", "010-8888-8888");
            MembershipPlan plan = MembershipPlan.basicMonthly();
            DateRange period = DateRange.fromTodayFor(30);
            member.assignMembership(plan, period);

            LocalDateTime futureTime = LocalDateTime.now().plusDays(2).withHour(15).withMinute(0).withSecond(0).withNano(0);
            TimeSlot timeSlot = TimeSlot.of(futureTime, futureTime.plusHours(1));
            Reservation reservation = Reservation.create("R001", member.getMemberId(), "GYM_001", timeSlot);
            reservation.confirm();

            // When: 예약 취소
            String cancellationReason = "개인 사정";
            reservation.cancel(cancellationReason);

            // Then: 취소 상태 확인
            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
            assertThat(reservation.getCancellationReason()).isEqualTo(cancellationReason);
            assertThat(reservation.getCancelledAt()).isNotNull();
            assertThat(reservation.isActive()).isFalse();
            assertThat(reservation.isFinal()).isTrue();
        }

        @Test
        @DisplayName("사용 중인 예약도 응급상황에서 취소할 수 있다")
        void canCancelInUseReservation() {
            // Given: 사용 중인 예약
            Member member = Member.create("M001", "김응급", "emergency@test.com", "010-9999-9999");
            MembershipPlan plan = MembershipPlan.basicMonthly();
            DateRange period = DateRange.fromTodayFor(30);
            member.assignMembership(plan, period);

            LocalDateTime futureTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
            TimeSlot timeSlot = TimeSlot.of(futureTime, futureTime.plusHours(2));
            Reservation reservation = Reservation.create("R001", member.getMemberId(), "GYM_001", timeSlot);

            reservation.confirm();
            reservation.startUsing();
            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.IN_USE);

            // When: 응급 취소
            reservation.cancel("응급상황 발생");

            // Then: 취소 성공 확인
            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
            assertThat(reservation.getCancellationReason()).isEqualTo("응급상황 발생");
        }

        @Test
        @DisplayName("완료된 예약은 취소할 수 없다")
        void cannotCancelCompletedReservation() {
            // Given: 완료된 예약
            Member member = Member.create("M001", "김완료", "completed@test.com", "010-0000-0000");
            MembershipPlan plan = MembershipPlan.basicMonthly();
            DateRange period = DateRange.fromTodayFor(30);
            member.assignMembership(plan, period);

            LocalDateTime futureTime = LocalDateTime.now().plusDays(1).withHour(16).withMinute(0).withSecond(0).withNano(0);
            TimeSlot timeSlot = TimeSlot.of(futureTime, futureTime.plusHours(1));
            Reservation reservation = Reservation.create("R001", member.getMemberId(), "GYM_001", timeSlot);

            reservation.confirm();
            reservation.startUsing();
            reservation.complete();

            // When & Then: 취소 시도 시 예외 발생
            assertThatThrownBy(() -> reservation.cancel("테스트 취소"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("현재 상태에서는 취소할 수 없습니다");
        }
    }

    @DisplayName("할인 및 가격 계산 시나리오")
    @Nested
    class PricingDiscountScenarioTest {

        @Test
        @DisplayName("할인이 적용된 멤버십 가격을 계산할 수 있다")
        void calculateDiscountedMembershipPrice() {
            // Given: 20% 할인이 적용된 VIP 플랜
            MembershipPlan vipPlan = MembershipPlan.vipYearly();

            // When & Then: 할인 가격 확인
            Money originalPrice = vipPlan.getPrice();
            Money discountedPrice = vipPlan.getDiscountedPrice();
            Money discountAmount = vipPlan.getDiscountAmount();

            assertThat(originalPrice).isEqualTo(Money.won(1000000)); // 원가 100만원
            assertThat(discountedPrice).isEqualTo(Money.won(800000)); // 20% 할인 적용
            assertThat(discountAmount).isEqualTo(Money.won(200000)); // 할인액 20만원
            assertThat(vipPlan.getDiscountRate()).isEqualTo(java.math.BigDecimal.valueOf(0.2));
        }

        @Test
        @DisplayName("일할 계산이 정확하게 이루어진다")
        void calculateProRatedPriceAccurately() {
            // Given: 월간 플랜 (30일, 30,000원)
            MembershipPlan monthlyPlan = MembershipPlan.create("TEST_MONTHLY", "테스트 월간권",
                    PlanType.MONTHLY, Money.won(30000), 30, Set.of(ResourceType.GYM));

            // When & Then: 다양한 남은 일수에 대한 일할 계산
            assertThat(monthlyPlan.calculateProRatedPrice(30)).isEqualTo(Money.won(30000)); // 전체
            assertThat(monthlyPlan.calculateProRatedPrice(15)).isEqualTo(Money.won(15000)); // 절반
            assertThat(monthlyPlan.calculateProRatedPrice(10)).isEqualTo(Money.won(10000)); // 1/3
            assertThat(monthlyPlan.calculateProRatedPrice(0)).isEqualTo(Money.zeroWon()); // 없음
        }

        @Test
        @DisplayName("플랜 간 비교 및 업그레이드 비용 계산이 정확하다")
        void calculateUpgradeCostAccurately() {
            // Given: 기본 플랜과 프리미엄 플랜
            MembershipPlan basic = MembershipPlan.basicMonthly(); // 50,000원
            MembershipPlan premium = MembershipPlan.premiumMonthly(); // 100,000원 (10% 할인 = 90,000원)

            // When: 업그레이드 비용 계산 (15일 남음)
            Money upgradeCost = basic.calculateUpgradeCost(premium, 15);

            // Then: 비용 계산 검증
            Money basicRefund = Money.won(25000); // 기본 플랜 15일치 환불
            Money premiumCost = premium.getDiscountedPrice(); // 프리미엄 플랜 할인 가격
            Money expectedCost = premiumCost.subtract(basicRefund);

            assertThat(upgradeCost).isEqualTo(expectedCost);
            assertThat(upgradeCost.isPositive()).isTrue(); // 추가 비용 발생
        }
    }

    @DisplayName("복잡한 비즈니스 시나리오")
    @Nested
    class ComplexBusinessScenarioTest {

        @Test
        @DisplayName("멤버십 만료 임박 시나리오")
        void membershipExpiryWarningScenario() {
            // Given: 멤버십이 3일 후 만료되는 회원
            Member member = Member.create("M001", "김만료임박", "expiring@test.com", "010-1234-5678");
            MembershipPlan plan = MembershipPlan.basicMonthly();
            DateRange expiringPeriod = DateRange.of(LocalDate.now().minusDays(27), LocalDate.now().plusDays(3));
            member.assignMembership(plan, expiringPeriod);

            // When & Then: 만료 경고 확인
            assertThat(member.getRemainingMembershipDays()).isEqualTo(4); // 오늘 포함 4일
            assertThat(member.isMembershipExpiryWarning(7)).isTrue(); // 7일 이내 만료 경고
            assertThat(member.isMembershipExpiryWarning(2)).isFalse(); // 2일 이내 경고는 아직 아님

            // 현재는 여전히 예약 가능
            assertThat(member.hasMembershipActive()).isTrue();
            assertThat(member.canReserve(ResourceType.GYM, LocalDate.now().plusDays(2))).isTrue();

            // 하지만 만료일 이후는 예약 불가
            assertThat(member.canReserve(ResourceType.GYM, LocalDate.now().plusDays(5))).isFalse();
        }

        @Test
        @DisplayName("동시 예약 제한 시나리오")
        void simultaneousReservationLimitScenario() {
            // Given: 최대 3개 동시 예약 가능한 기본 플랜 회원
            Member member = Member.create("M001", "김동시예약", "concurrent@test.com", "010-1111-2222");
            MembershipPlan basicPlan = MembershipPlan.basicMonthly(); // 최대 3개 동시 예약
            DateRange period = DateRange.fromTodayFor(30);
            member.assignMembership(basicPlan, period);

            // When & Then: 동시 예약 제한 확인
            assertThat(basicPlan.getMaxSimultaneousReservations()).isEqualTo(3);
            assertThat(member.canReserveSimultaneously(0)).isTrue(); // 0개 → 1개 예약
            assertThat(member.canReserveSimultaneously(2)).isTrue(); // 2개 → 3개 예약
            assertThat(member.canReserveSimultaneously(3)).isFalse(); // 3개 → 4개 예약 (초과)

            // 프리미엄으로 업그레이드하면 제한 증가
            MembershipPlan premiumPlan = MembershipPlan.premiumMonthly(); // 최대 5개
            member.changePlan(premiumPlan);
            assertThat(member.canReserveSimultaneously(4)).isTrue(); // 이제 5개까지 가능
        }

        @Test
        @DisplayName("리소스 기능 요구사항 매칭 시나리오")
        void resourceFeatureMatchingScenario() {
            // Given: 특정 기능이 필요한 예약 상황
            Resource basicStudyRoom = Resource.createStudyRoom("STUDY_BASIC", "기본 스터디룸", "2층", 4);
            Resource advancedMeetingRoom = Resource.createWithDetails(
                    "MEETING_ADVANCED", "고급 회의실", ResourceType.MEETING_ROOM, "5층", 12,
                    "최신 시설을 갖춘 프리미엄 회의실",
                    java.util.Map.of(), // 운영시간은 기본값 사용
                    Set.of("4K 프로젝터", "전자칠판", "화상회의", "음향시설", "WiFi", "에어컨")
            );

            // When & Then: 기능 보유 여부 확인
            // 기본 스터디룸
            assertThat(basicStudyRoom.hasFeature("WiFi")).isTrue();
            assertThat(basicStudyRoom.hasFeature("4K 프로젝터")).isFalse();
            assertThat(basicStudyRoom.hasAllFeatures(Set.of("WiFi", "에어컨"))).isTrue();
            assertThat(basicStudyRoom.hasAllFeatures(Set.of("WiFi", "4K 프로젝터"))).isFalse();

            // 고급 회의실
            assertThat(advancedMeetingRoom.hasFeature("4K 프로젝터")).isTrue();
            assertThat(advancedMeetingRoom.hasFeature("화상회의")).isTrue();
            assertThat(advancedMeetingRoom.hasAllFeatures(
                    Set.of("4K 프로젝터", "전자칠판", "화상회의"))).isTrue();
        }
    }
}