package com.hexapass.domain.policy.specification;

import com.hexapass.domain.common.DateRange;
import com.hexapass.domain.model.Member;
import com.hexapass.domain.model.MembershipPlan;
import com.hexapass.domain.policy.ReservationContext;
import com.hexapass.domain.type.MemberStatus;
import com.hexapass.domain.type.PlanType;
import com.hexapass.domain.type.ResourceType;
import com.hexapass.domain.common.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@DisplayName("활성 회원 사양 테스트")
class ActiveMemberSpecificationTest {

    private ActiveMemberSpecification specification;

    @BeforeEach
    void setUp() {
        specification = new ActiveMemberSpecification();
    }

    @DisplayName("활성 회원 조건 만족 테스트")
    @Nested
    class SatisfiedConditionTest {

        @Test
        @DisplayName("활성 상태이고 유효한 멤버십을 가진 회원은 조건을 만족한다")
        void satisfiedByActiveMemberWithValidMembership() {
            // Given
            Member member = Member.create("M001", "김회원", "member@test.com", "010-1234-5678");
            MembershipPlan plan = MembershipPlan.create("BASIC", "기본 플랜", PlanType.MONTHLY,
                    Money.won(30000), 30, Set.of(ResourceType.GYM));
            DateRange membershipPeriod = DateRange.of(LocalDate.now(), LocalDate.now().plusDays(30));

            member.assignMembership(plan, membershipPeriod);

            ReservationContext context = ReservationContext.create(
                    member, "RESOURCE_001", ResourceType.GYM,
                    LocalDateTime.now().plusHours(1), 0, 0, 10);

            // When & Then
            assertThat(specification.isSatisfiedBy(context)).isTrue();
        }

        @Test
        @DisplayName("활성 상태이고 만료되지 않은 멤버십을 가진 회원은 조건을 만족한다")
        void satisfiedByActiveMemberWithNonExpiredMembership() {
            // Given
            Member member = Member.create("M001", "김회원", "member@test.com", "010-1234-5678");
            MembershipPlan plan = MembershipPlan.create("PREMIUM", "프리미엄 플랜", PlanType.MONTHLY,
                    Money.won(50000), 30, Set.of(ResourceType.GYM, ResourceType.POOL));
            DateRange membershipPeriod = DateRange.of(LocalDate.now().minusDays(10), LocalDate.now().plusDays(20));

            member.assignMembership(plan, membershipPeriod);

            ReservationContext context = ReservationContext.create(
                    member, "RESOURCE_001", ResourceType.GYM,
                    LocalDateTime.now().plusHours(1), 0, 0, 10);

            // When & Then
            assertThat(specification.isSatisfiedBy(context)).isTrue();
        }
    }

    @DisplayName("활성 회원 조건 불만족 테스트")
    @Nested
    class UnsatisfiedConditionTest {

        @Test
        @DisplayName("회원이 null이면 조건을 만족하지 않는다")
        void notSatisfiedByNullMember() {
            // Given
            ReservationContext context = ReservationContext.create(
                    null, "RESOURCE_001", ResourceType.GYM,
                    LocalDateTime.now().plusHours(1), 0, 0, 10);

            // When & Then
            assertThat(specification.isSatisfiedBy(context)).isFalse();
        }

        @Test
        @DisplayName("정지된 회원은 조건을 만족하지 않는다")
        void notSatisfiedBySuspendedMember() {
            // Given
            Member member = Member.create("M001", "김회원", "member@test.com", "010-1234-5678");
            MembershipPlan plan = MembershipPlan.create("BASIC", "기본 플랜", PlanType.MONTHLY,
                    Money.won(30000), 30, Set.of(ResourceType.GYM));
            DateRange membershipPeriod = DateRange.of(LocalDate.now(), LocalDate.now().plusDays(30));

            member.assignMembership(plan, membershipPeriod);
            member.suspend("테스트 정지");

            ReservationContext context = ReservationContext.create(
                    member, "RESOURCE_001", ResourceType.GYM,
                    LocalDateTime.now().plusHours(1), 0, 0, 10);

            // When & Then
            assertThat(specification.isSatisfiedBy(context)).isFalse();
        }

        @Test
        @DisplayName("탈퇴한 회원은 조건을 만족하지 않는다")
        void notSatisfiedByWithdrawnMember() {
            // Given
            Member member = Member.create("M001", "김회원", "member@test.com", "010-1234-5678");
            MembershipPlan plan = MembershipPlan.create("BASIC", "기본 플랜", PlanType.MONTHLY,
                    Money.won(30000), 30, Set.of(ResourceType.GYM));
            DateRange membershipPeriod = DateRange.of(LocalDate.now(), LocalDate.now().plusDays(30));

            member.assignMembership(plan, membershipPeriod);
            member.withdraw();

            ReservationContext context = ReservationContext.create(
                    member, "RESOURCE_001", ResourceType.GYM,
                    LocalDateTime.now().plusHours(1), 0, 0, 10);

            // When & Then
            assertThat(specification.isSatisfiedBy(context)).isFalse();
        }

        @Test
        @DisplayName("멤버십이 없는 활성 회원은 조건을 만족하지 않는다")
        void notSatisfiedByActiveMemberWithoutMembership() {
            // Given
            Member member = Member.create("M001", "김회원", "member@test.com", "010-1234-5678");
            // 멤버십 할당하지 않음

            ReservationContext context = ReservationContext.create(
                    member, "RESOURCE_001", ResourceType.GYM,
                    LocalDateTime.now().plusHours(1), 0, 0, 10);

            // When & Then
            assertThat(specification.isSatisfiedBy(context)).isFalse();
        }

        @Test
        @DisplayName("멤버십이 만료된 활성 회원은 조건을 만족하지 않는다")
        void notSatisfiedByActiveMemberWithExpiredMembership() {
            // Given
            Member member = Member.create("M001", "김회원", "member@test.com", "010-1234-5678");
            MembershipPlan plan = MembershipPlan.create("BASIC", "기본 플랜", PlanType.MONTHLY,
                    Money.won(30000), 30, Set.of(ResourceType.GYM));

            // 과거 기간의 멤버십 (만료됨)
            DateRange expiredPeriod = DateRange.of(LocalDate.now().minusDays(60), LocalDate.now().minusDays(30));
            member.assignMembership(plan, expiredPeriod);

            ReservationContext context = ReservationContext.create(
                    member, "RESOURCE_001", ResourceType.GYM,
                    LocalDateTime.now().plusHours(1), 0, 0, 10);

            // When & Then
            assertThat(specification.isSatisfiedBy(context)).isFalse();
        }

        @Test
        @DisplayName("비활성화된 멤버십 플랜을 가진 회원은 조건을 만족하지 않는다")
        void notSatisfiedByMemberWithInactivePlan() {
            // Given
            Member member = Member.create("M001", "김회원", "member@test.com", "010-1234-5678");
            MembershipPlan plan = MembershipPlan.create("BASIC", "기본 플랜", PlanType.MONTHLY,
                    Money.won(30000), 30, Set.of(ResourceType.GYM));
            DateRange membershipPeriod = DateRange.of(LocalDate.now(), LocalDate.now().plusDays(30));

            member.assignMembership(plan, membershipPeriod);
            plan.deactivate(); // 플랜 비활성화

            ReservationContext context = ReservationContext.create(
                    member, "RESOURCE_001", ResourceType.GYM,
                    LocalDateTime.now().plusHours(1), 0, 0, 10);

            // When & Then
            assertThat(specification.isSatisfiedBy(context)).isFalse();
        }
    }

    @DisplayName("사양 설명 테스트")
    @Nested
    class DescriptionTest {

        @Test
        @DisplayName("사양 설명이 올바르게 반환된다")
        void correctDescription() {
            assertThat(specification.getDescription()).isEqualTo("활성 회원 여부");
        }
    }

    @DisplayName("경계값 테스트")
    @Nested
    class BoundaryValueTest {

        @Test
        @DisplayName("오늘 시작하는 멤버십은 조건을 만족한다")
        void satisfiedByMembershipStartingToday() {
            // Given
            Member member = Member.create("M001", "김회원", "member@test.com", "010-1234-5678");
            MembershipPlan plan = MembershipPlan.create("BASIC", "기본 플랜", PlanType.MONTHLY,
                    Money.won(30000), 30, Set.of(ResourceType.GYM));
            DateRange membershipPeriod = DateRange.of(LocalDate.now(), LocalDate.now().plusDays(30));

            member.assignMembership(plan, membershipPeriod);

            ReservationContext context = ReservationContext.create(
                    member, "RESOURCE_001", ResourceType.GYM,
                    LocalDateTime.now().plusHours(1), 0, 0, 10);

            // When & Then
            assertThat(specification.isSatisfiedBy(context)).isTrue();
        }

        @Test
        @DisplayName("오늘 끝나는 멤버십은 조건을 만족한다")
        void satisfiedByMembershipEndingToday() {
            // Given
            Member member = Member.create("M001", "김회원", "member@test.com", "010-1234-5678");
            MembershipPlan plan = MembershipPlan.create("BASIC", "기본 플랜", PlanType.MONTHLY,
                    Money.won(30000), 30, Set.of(ResourceType.GYM));
            DateRange membershipPeriod = DateRange.of(LocalDate.now().minusDays(30), LocalDate.now());

            member.assignMembership(plan, membershipPeriod);

            ReservationContext context = ReservationContext.create(
                    member, "RESOURCE_001", ResourceType.GYM,
                    LocalDateTime.now().plusHours(1), 0, 0, 10);

            // When & Then
            assertThat(specification.isSatisfiedBy(context)).isTrue();
        }

        @Test
        @DisplayName("어제 끝난 멤버십은 조건을 만족하지 않는다")
        void notSatisfiedByMembershipEndedYesterday() {
            // Given
            Member member = Member.create("M001", "김회원", "member@test.com", "010-1234-5678");
            MembershipPlan plan = MembershipPlan.create("BASIC", "기본 플랜", PlanType.MONTHLY,
                    Money.won(30000), 30, Set.of(ResourceType.GYM));
            DateRange membershipPeriod = DateRange.of(LocalDate.now().minusDays(30), LocalDate.now().minusDays(1));

            member.assignMembership(plan, membershipPeriod);

            ReservationContext context = ReservationContext.create(
                    member, "RESOURCE_001", ResourceType.GYM,
                    LocalDateTime.now().plusHours(1), 0, 0, 10);

            // When & Then
            assertThat(specification.isSatisfiedBy(context)).isFalse();
        }
    }
}