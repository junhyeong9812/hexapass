# MemberTest.md

## 클래스 개요
`MemberTest`는 회원을 다루는 `Member` 도메인 엔티티의 기능을 검증하는 테스트 클래스입니다. 이 클래스는 회원 생성, 멤버십 관리, 예약 권한 확인, 상태 관리, 정보 조회 등의 핵심 비즈니스 로직을 테스트합니다.

## 왜 Member 객체가 필요한가?
- **회원 중심의 비즈니스**: 모든 예약과 서비스 이용이 회원을 기반으로 이루어짐
- **복잡한 상태 관리**: 활성, 정지, 탈퇴 등 다양한 회원 상태와 상태 전이 관리
- **멤버십과 권한**: 회원별 멤버십 플랜과 그에 따른 서비스 이용 권한 관리
- **비즈니스 규칙 적용**: 예약 제한, 만료 경고 등 회원 관련 정책 구현

```java
package com.hexapass.domain.model;

import com.hexapass.domain.common.DateRange;
import com.hexapass.domain.type.MemberStatus;
import com.hexapass.domain.type.ResourceType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

// Member 도메인 엔티티의 모든 기능을 검증하는 테스트 클래스
@DisplayName("Member 엔티티 테스트")
class MemberTest {

    @DisplayName("Member 생성 테스트")
    @Nested
    class CreationTest {

        @Test
        @DisplayName("유효한 정보로 회원을 생성할 수 있다")
        void createValidMember() {
            // Given: 회원 생성에 필요한 기본 정보들
            String memberId = "M001";
            String name = "김헥사";
            String email = "hexpass@example.com";
            String phone = "010-1234-5678";

            // When: 정적 팩토리 메서드로 회원 생성
            // create(): 회원을 생성하는 정적 팩토리 메서드
            Member member = Member.create(memberId, name, email, phone);

            // Then: 생성된 회원의 속성들 검증
            assertThat(member.getMemberId()).isEqualTo(memberId);
            assertThat(member.getName()).isEqualTo(name);
            assertThat(member.getEmail()).isEqualTo(email);
            assertThat(member.getPhone()).isEqualTo(phone);
            // 기본 상태는 활성 상태
            assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
            // 생성 시간이 자동으로 설정됨
            assertThat(member.getCreatedAt()).isNotNull();
            // 상태 변경 시간도 생성 시점에 설정
            assertThat(member.getLastStatusChangedAt()).isNotNull();
            // 초기에는 멤버십 플랜이 없음
            assertThat(member.getCurrentPlan()).isNull();
            assertThat(member.getMembershipPeriod()).isNull();
        }

        @ParameterizedTest
        @DisplayName("필수 필드가 null이거나 빈 값이면 예외가 발생한다")
        @MethodSource("provideInvalidStringFields")
        void createWithInvalidStringFields(String memberId, String name, String email, String phone) {
            // When & Then: 필수 문자열 필드가 유효하지 않으면 예외 발생
            // 비즈니스 규칙: 모든 필수 필드는 null이거나 공백일 수 없음
            assertThatThrownBy(() -> Member.create(memberId, name, email, phone))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("null이거나 빈 값일 수 없습니다");
        }

        // 유효하지 않은 필수 필드 조합을 제공하는 데이터 소스
        static Stream<Arguments> provideInvalidStringFields() {
            return Stream.of(
                    Arguments.of(null, "Valid Name", "valid@email.com", "010-1234-5678"),
                    Arguments.of("", "Valid Name", "valid@email.com", "010-1234-5678"),
                    Arguments.of("  ", "Valid Name", "valid@email.com", "010-1234-5678"),
                    Arguments.of("M001", null, "valid@email.com", "010-1234-5678"),
                    Arguments.of("M001", "", "valid@email.com", "010-1234-5678"),
                    Arguments.of("M001", "  ", "valid@email.com", "010-1234-5678")
            );
        }

        @ParameterizedTest
        @DisplayName("잘못된 이메일 형식으로 생성하면 예외가 발생한다")
        @ValueSource(strings = {"invalid", "invalid@", "@invalid.com", "invalid@invalid", "invalid.com"})
        void createWithInvalidEmailFormat(String invalidEmail) {
            // When & Then: 잘못된 이메일 형식으로 생성 시도
            // 비즈니스 규칙: 이메일은 올바른 형식이어야 함 (정규식 검증)
            assertThatThrownBy(() -> Member.create("M001", "Valid Name", invalidEmail, "010-1234-5678"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("유효하지 않은 이메일 형식입니다");
        }

        @ParameterizedTest
        @DisplayName("잘못된 전화번호 형식으로 생성하면 예외가 발생한다")
        @ValueSource(strings = {"010-123-4567", "010-12345678", "02-1234-5678", "010 1234 5678", "010-1234-567"})
        void createWithInvalidPhoneFormat(String invalidPhone) {
            // When & Then: 잘못된 전화번호 형식으로 생성 시도
            // 비즈니스 규칙: 전화번호는 010-XXXX-XXXX 형식이어야 함
            assertThatThrownBy(() -> Member.create("M001", "Valid Name", "valid@email.com", invalidPhone))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("전화번호는 010-XXXX-XXXX 형식이어야 합니다");
        }

        @Test
        @DisplayName("유효한 이메일 형식들을 올바르게 처리한다")
        void acceptValidEmailFormats() {
            // Given: 다양한 유효한 이메일 형식들
            String[] validEmails = {
                    "test@example.com",
                    "user.name@domain.co.kr", 
                    "test123@test-domain.com",
                    "user+tag@example.org"
            };

            // When & Then: 모든 유효한 이메일 형식이 정상적으로 처리되는지 확인
            // assertThatNoException(): 예외가 발생하지 않음을 확인하는 AssertJ 메서드
            for (String email : validEmails) {
                assertThatNoException().isThrownBy(() ->
                        Member.create("M001", "Test User", email, "010-1234-5678"));
            }
        }
    }

    @DisplayName("Member 멤버십 관리 테스트")
    @Nested
    class MembershipManagementTest {

        private Member member;
        private MembershipPlan plan;
        private DateRange period;

        @Test
        @DisplayName("멤버십을 할당할 수 있다")
        void assignMembership() {
            // Given: 회원과 멤버십 플랜, 기간 준비
            member = Member.create("M001", "김헥사", "test@example.com", "010-1234-5678");
            plan = MembershipPlan.basicMonthly();
            period = DateRange.fromTodayFor(30);

            // When: 멤버십 할당
            // assignMembership(): 회원에게 멤버십 플랜과 기간을 할당
            member.assignMembership(plan, period);

            // Then: 멤버십 할당 결과 확인
            assertThat(member.getCurrentPlan()).isEqualTo(plan);
            assertThat(member.getMembershipPeriod()).isEqualTo(period);
            // hasMembershipActive(): 활성 멤버십 보유 여부 확인
            assertThat(member.hasMembershipActive()).isTrue();
        }

        @Test
        @DisplayName("null 멤버십 플랜으로 할당하면 예외가 발생한다")
        void assignNullMembershipPlan() {
            // Given: 회원과 유효한 기간
            member = Member.create("M001", "김헥사", "test@example.com", "010-1234-5678");
            period = DateRange.fromTodayFor(30);

            // When & Then: null 플랜으로 할당 시도
            assertThatThrownBy(() -> member.assignMembership(null, period))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("멤버십 플랜은 null일 수 없습니다");
        }

        @Test
        @DisplayName("null 멤버십 기간으로 할당하면 예외가 발생한다")
        void assignNullMembershipPeriod() {
            // Given: 회원과 유효한 플랜
            member = Member.create("M001", "김헥사", "test@example.com", "010-1234-5678");
            plan = MembershipPlan.basicMonthly();

            // When & Then: null 기간으로 할당 시도
            assertThatThrownBy(() -> member.assignMembership(plan, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("멤버십 기간은 null일 수 없습니다");
        }

        @Test
        @DisplayName("과거 종료일의 멤버십 기간으로 할당하면 예외가 발생한다")
        void assignPastEndDateMembership() {
            // Given: 회원과 플랜, 그리고 이미 지난 기간
            member = Member.create("M001", "김헥사", "test@example.com", "010-1234-5678");
            plan = MembershipPlan.basicMonthly();
            // 과거의 기간 (60일 전~30일 전)
            DateRange pastPeriod = DateRange.of(LocalDate.now().minusDays(60), LocalDate.now().minusDays(30));

            // When & Then: 이미 만료된 기간으로 할당 시도
            // 비즈니스 규칙: 멤버십 종료일은 현재 날짜 이후여야 함
            assertThatThrownBy(() -> member.assignMembership(plan, pastPeriod))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("멤버십 종료일은 현재 날짜 이후여야 합니다");
        }

        @Test
        @DisplayName("비활성화된 플랜으로 할당하면 예외가 발생한다")
        void assignInactivePlan() {
            // Given: 회원과 비활성화된 플랜
            member = Member.create("M001", "김헥사", "test@example.com", "010-1234-5678");
            plan = MembershipPlan.basicMonthly();
            plan.deactivate(); // 플랜 비활성화
            period = DateRange.fromTodayFor(30);

            // When & Then: 비활성화된 플랜으로 할당 시도
            // 비즈니스 규칙: 비활성화된 플랜은 할당할 수 없음
            assertThatThrownBy(() -> member.assignMembership(plan, period))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("비활성화된 플랜은 할당할 수 없습니다");
        }

        @Test
        @DisplayName("멤버십을 연장할 수 있다")
        void extendMembership() {
            // Given: 멤버십이 할당된 회원
            member = Member.create("M001", "김헥사", "test@example.com", "010-1234-5678");
            plan = MembershipPlan.basicMonthly();
            period = DateRange.fromTodayFor(30);
            member.assignMembership(plan, period);

            // When: 멤버십 15일 연장
            // extendMembership(): 현재 멤버십 기간을 지정된 일수만큼 연장
            member.extendMembership(15);

            // Then: 연장된 멤버십 기간 확인
            DateRange extendedPeriod = member.getMembershipPeriod();
            assertThat(extendedPeriod.getDays()).isEqualTo(45L); // 30 + 15
            assertThat(extendedPeriod.getEndDate()).isEqualTo(period.getEndDate().plusDays(15));
        }

        @Test
        @DisplayName("멤버십 없이 연장하려 하면 예외가 발생한다")
        void extendMembershipWithoutAssignment() {
            // Given: 멤버십이 할당되지 않은 회원
            member = Member.create("M001", "김헥사", "test@example.com", "010-1234-5678");

            // When & Then: 멤버십 없이 연장 시도
            // 비즈니스 규칙: 멤버십이 있어야만 연장 가능
            assertThatThrownBy(() -> member.extendMembership(15))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("멤버십이 할당되지 않은 상태에서는 연장할 수 없습니다");
        }

        @ParameterizedTest
        @DisplayName("0 이하의 일수로 연장하면 예외가 발생한다")
        @ValueSource(ints = {0, -1, -15})
        void extendMembershipWithInvalidDays(int invalidDays) {
            // Given: 멤버십이 할당된 회원
            member = Member.create("M001", "김헥사", "test@example.com", "010-1234-5678");
            plan = MembershipPlan.basicMonthly();
            period = DateRange.fromTodayFor(30);
            member.assignMembership(plan, period);

            // When & Then: 0 이하의 일수로 연장 시도
            // 비즈니스 규칙: 연장 일수는 양수여야 함
            assertThatThrownBy(() -> member.extendMembership(invalidDays))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("연장 일수는 0보다 커야 합니다");
        }

        @Test
        @DisplayName("멤버십 플랜을 변경할 수 있다")
        void changeMembershipPlan() {
            // Given: 기본 플랜이 할당된 회원
            member = Member.create("M001", "김헥사", "test@example.com", "010-1234-5678");
            MembershipPlan basicPlan = MembershipPlan.basicMonthly();
            MembershipPlan premiumPlan = MembershipPlan.premiumMonthly();
            period = DateRange.fromTodayFor(30);
            member.assignMembership(basicPlan, period);

            // When: 프리미엄 플랜으로 변경
            // changePlan(): 현재 멤버십 플랜을 다른 플랜으로 변경
            member.changePlan(premiumPlan);

            // Then: 플랜 변경 결과 확인
            assertThat(member.getCurrentPlan()).isEqualTo(premiumPlan);
            // 기간은 그대로 유지됨
            assertThat(member.getMembershipPeriod()).isEqualTo(period);
        }

        @Test
        @DisplayName("멤버십 기간 없이 플랜 변경하려 하면 예외가 발생한다")
        void changePlanWithoutMembershipPeriod() {
            // Given: 멤버십 기간이 설정되지 않은 회원
            member = Member.create("M001", "김헥사", "test@example.com", "010-1234-5678");
            MembershipPlan newPlan = MembershipPlan.premiumMonthly();

            // When & Then: 멤버십 기간 없이 플랜 변경 시도
            // 비즈니스 규칙: 멤버십 기간이 있어야만 플랜 변경 가능
            assertThatThrownBy(() -> member.changePlan(newPlan))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("멤버십 기간이 설정되지 않은 상태에서는 플랜을 변경할 수 없습니다");
        }

        @Test
        @DisplayName("멤버십 만료 여부를 올바르게 판단한다")
        void checkMembershipExpiry() {
            member = Member.create("M001", "김헥사", "test@example.com", "010-1234-5678");
            plan = MembershipPlan.basicMonthly();

            // 유효한 멤버십 (30일간)
            DateRange validPeriod = DateRange.fromTodayFor(30);
            member.assignMembership(plan, validPeriod);
            // isMembershipExpired(): 멤버십 만료 여부 확인
            assertThat(member.isMembershipExpired()).isFalse();

            // 만료된 멤버십 (60일 전~30일 전)
            DateRange expiredPeriod = DateRange.of(LocalDate.now().minusDays(60), LocalDate.now().minusDays(30));
            member.assignMembership(plan, expiredPeriod);
            assertThat(member.isMembershipExpired()).isTrue();

            // 멤버십 없음
            Member memberWithoutPlan = Member.create("M002", "김노플랜", "no@plan.com", "010-9999-9999");
            assertThat(memberWithoutPlan.isMembershipExpired()).isTrue(); // 멤버십 없으면 만료된 것으로 처리
        }

        @Test
        @DisplayName("멤버십 활성 여부를 종합적으로 판단한다")
        void checkMembershipActiveStatus() {
            member = Member.create("M001", "김헥사", "test@example.com", "010-1234-5678");
            plan = MembershipPlan.basicMonthly();
            period = DateRange.fromTodayFor(30);

            // 정상 활성 상태 (회원 활성 + 플랜 활성 + 기간 유효)
            member.assignMembership(plan, period);
            assertThat(member.hasMembershipActive()).isTrue();

            // 회원 정지 상태
            member.suspend("테스트 정지");
            assertThat(member.hasMembershipActive()).isFalse();

            // 회원 재활성화
            member.activate();
            assertThat(member.hasMembershipActive()).isTrue();

            // 플랜 비활성화
            plan.deactivate();
            assertThat(member.hasMembershipActive()).isFalse();
        }
    }

    @DisplayName("Member 예약 권한 확인 테스트")
    @Nested
    class ReservationAuthorizationTest {

        private Member member;
        private MembershipPlan plan;
        private DateRange period;

        @Test
        @DisplayName("활성 멤버십으로 권한 내 리소스를 예약할 수 있다")
        void canReserveAllowedResource() {
            // Given: 활성 멤버십을 가진 회원
            member = Member.create("M001", "김헥사", "test@example.com", "010-1234-5678");
            plan = MembershipPlan.basicMonthly(); // GYM, STUDY_ROOM 이용 가능
            period = DateRange.fromTodayFor(30);
            member.assignMembership(plan, period);

            LocalDate tomorrow = LocalDate.now().plusDays(1);

            // When & Then: 예약 권한 확인
            // canMakeReservation(): 예약 가능한 상태인지 전반적인 확인
            assertThat(member.canMakeReservation()).isTrue();
            // canReserve(): 특정 리소스와 날짜에 예약 가능한지 확인
            assertThat(member.canReserve(ResourceType.GYM, tomorrow)).isTrue();
            assertThat(member.canReserve(ResourceType.STUDY_ROOM, tomorrow)).isTrue();
        }

        @Test
        @DisplayName("권한 없는 리소스는 예약할 수 없다")
        void cannotReserveUnauthorizedResource() {
            // Given: 기본 플랜 회원 (GYM, STUDY_ROOM만 이용 가능)
            member = Member.create("M001", "김헥사", "test@example.com", "010-1234-5678");
            plan = MembershipPlan.basicMonthly();
            period = DateRange.fromTodayFor(30);
            member.assignMembership(plan, period);

            LocalDate tomorrow = LocalDate.now().plusDays(1);

            // When & Then: 권한 없는 리소스 예약 시도
            // 기본 플랜에서는 POOL, SAUNA, TENNIS_COURT 이용 불가
            assertThat(member.canReserve(ResourceType.POOL, tomorrow)).isFalse();
            assertThat(member.canReserve(ResourceType.SAUNA, tomorrow)).isFalse();
            assertThat(member.canReserve(ResourceType.TENNIS_COURT, tomorrow)).isFalse();
        }

        @Test
        @DisplayName("멤버십 기간 외의 날짜는 예약할 수 없다")
        void cannotReserveOutsideMembershipPeriod() {
            // Given: 30일간 유효한 멤버십을 가진 회원
            member = Member.create("M001", "김헥사", "test@example.com", "010-1234-5678");
            plan = MembershipPlan.basicMonthly();
            period = DateRange.fromTodayFor(30); // 30일간 유효
            member.assignMembership(plan, period);

            // When & Then: 멤버십 기간 내외 예약 확인
            LocalDate withinPeriod = LocalDate.now().plusDays(15); // 기간 내
            LocalDate outsidePeriod = LocalDate.now().plusDays(35); // 기간 외

            assertThat(member.canReserve(ResourceType.GYM, withinPeriod)).isTrue();
            assertThat(member.canReserve(ResourceType.GYM, outsidePeriod)).isFalse();
        }

        @Test
        @DisplayName("선예약 가능 여부를 확인할 수 있다")
        void checkAdvanceReservation() {
            // Given: 30일 선예약이 가능한 기본 플랜 회원
            member = Member.create("M001", "김헥사", "test@example.com", "010-1234-5678");
            plan = MembershipPlan.basicMonthly(); // 30일 선예약 가능
            period = DateRange.fromTodayFor(60); // 60일간 멤버십
            member.assignMembership(plan, period);

            // When & Then: 선예약 제한 확인
            // canReserveInAdvance(): 특정 날짜까지 선예약 가능한지 확인
            LocalDate within30Days = LocalDate.now().plusDays(25);
            LocalDate beyond30Days = LocalDate.now().plusDays(35);

            assertThat(member.canReserveInAdvance(within30Days)).isTrue();
            assertThat(member.canReserveInAdvance(beyond30Days)).isFalse();
        }

        @Test
        @DisplayName("오늘이나 과거 날짜는 선예약 제한 없이 예약 가능하다")
        void noAdvanceLimitForTodayOrPast() {
            // Given: 회원과 멤버십
            member = Member.create("M001", "김헥사", "test@example.com", "010-1234-5678");
            plan = MembershipPlan.basicMonthly();
            period = DateRange.fromTodayFor(30);
            member.assignMembership(plan, period);

            // When & Then: 오늘과 과거 날짜의 선예약 제한 확인
            // 비즈니스 규칙: 오늘이나 과거는 선예약 제한 적용하지 않음
            LocalDate today = LocalDate.now();
            LocalDate yesterday = LocalDate.now().minusDays(1);

            assertThat(member.canReserveInAdvance(today)).isTrue();
            assertThat(member.canReserveInAdvance(yesterday)).isTrue();
        }

        @Test
        @DisplayName("동시 예약 가능 여부를 확인할 수 있다")
        void checkSimultaneousReservation() {
            // Given: 최대 3개 동시 예약이 가능한 기본 플랜 회원
            member = Member.create("M001", "김헥사", "test@example.com", "010-1234-5678");
            plan = MembershipPlan.basicMonthly(); // 최대 3개 동시 예약
            period = DateRange.fromTodayFor(30);
            member.assignMembership(plan, period);

            // When & Then: 동시 예약 제한 확인
            // canReserveSimultaneously(): 현재 예약 수 기준으로 추가 예약 가능한지 확인
            assertThat(member.canReserveSimultaneously(0)).isTrue(); // 0개 → 1개
            assertThat(member.canReserveSimultaneously(2)).isTrue(); // 2개 → 3개
            assertThat(member.canReserveSimultaneously(3)).isFalse(); // 3개 → 4개 (초과)
            assertThat(member.canReserveSimultaneously(5)).isFalse(); // 5개 → 6개 (초과)
        }

        @ParameterizedTest
        @DisplayName("비활성 상태에서는 예약할 수 없다")
        @EnumSource(value = MemberStatus.class, names = {"SUSPENDED", "WITHDRAWN"})
        void cannotReserveWhenInactive(MemberStatus inactiveStatus) {
            // Given: 활성 멤버십을 가진 회원
            member = Member.create("M001", "김헥사", "test@example.com", "010-1234-5678");
            plan = MembershipPlan.basicMonthly();
            period = DateRange.fromTodayFor(30);
            member.assignMembership(plan, period);

            // When: 회원 상태를 비활성으로 변경
            if (inactiveStatus == MemberStatus.SUSPENDED) {
                member.suspend("테스트 정지");
            } else if (inactiveStatus == MemberStatus.WITHDRAWN) {
                member.withdraw();
            }

            // Then: 비활성 상태에서는 모든 예약 불가
            LocalDate tomorrow = LocalDate.now().plusDays(1);
            assertThat(member.canMakeReservation()).isFalse();
            assertThat(member.canReserve(ResourceType.GYM, tomorrow)).isFalse();
            assertThat(member.canReserveInAdvance(tomorrow)).isFalse();
            assertThat(member.canReserveSimultaneously(0)).isFalse();
        }
    }

    @DisplayName("Member 상태 관리 테스트")
    @Nested
    class StatusManagementTest {

        private Member member;

        @Test
        @DisplayName("회원을 정지할 수 있다")
        void suspendMember() {
            // Given: 활성 회원
            member = Member.create("M001", "김헥사", "test@example.com", "010-1234-5678");
            String suspensionReason = "규정 위반";

            // When: 회원 정지
            // suspend(): 회원을 정지 상태로 변경하고 정지 사유 기록
            member.suspend(suspensionReason);

            // Then: 정지 상태 및 정지 사유 확인
            assertThat(member.getStatus()).isEqualTo(MemberStatus.SUSPENDED);
            // getSuspensionReason(): 정지 사유 조회
            assertThat(member.getSuspensionReason()).isEqualTo(suspensionReason);
            // getLastStatusChangedAt(): 마지막 상태 변경 시간
            assertThat(member.getLastStatusChangedAt()).isNotNull();
        }

        @Test
        @DisplayName("탈퇴한 회원은 정지할 수 없다")
        void cannotSuspendWithdrawnMember() {
            // Given: 탈퇴한 회원
            member = Member.create("M001", "김헥사", "test@example.com", "010-1234-5678");
            member.withdraw();

            // When & Then: 탈퇴한 회원을 정지하려 할 때 예외 발생
            // 비즈니스 규칙: 탈퇴한 회원은 더 이상 상태 변경 불가
            assertThatThrownBy(() -> member.suspend("테스트 정지"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("탈퇴한 회원은 정지할 수 없습니다");
        }

        @Test
        @DisplayName("정지 사유 없이 정지하면 예외가 발생한다")
        void cannotSuspendWithoutReason() {
            // Given: 활성 회원
            member = Member.create("M001", "김헥사", "test@example.com", "010-1234-5678");

            // When & Then: 정지 사유 없이 정지 시도
            // 비즈니스 규칙: 정지 시 반드시 사유를 명시해야 함
            assertThatThrownBy(() -> member.suspend(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("정지 사유는 null이거나 빈 값일 수 없습니다");

            assertThatThrownBy(() -> member.suspend(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("정지 사유는 null이거나 빈 값일 수 없습니다");

            assertThatThrownBy(() -> member.suspend("  "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("정지 사유는 null이거나 빈 값일 수 없습니다");
        }

        @Test
        @DisplayName("정지된 회원을 활성화할 수 있다")
        void activateSuspendedMember() {
            // Given: 정지된 회원
            member = Member.create("M001", "김헥사", "test@example.com", "010-1234-5678");
            member.suspend("테스트 정지");
            assertThat(member.getSuspensionReason()).isNotNull();

            // When: 회원 활성화
            // activate(): 정지된 회원을 다시 활성 상태로 변경
            member.activate();

            // Then: 활성화 상태 및 정지 사유 초기화 확인
            assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
            // 활성화 시 정지 사유는 초기화됨
            assertThat(member.getSuspensionReason()).isNull();
            assertThat(member.getLastStatusChangedAt()).isNotNull();
        }

        @Test
        @DisplayName("탈퇴한 회원은 활성화할 수 없다")
        void cannotActivateWithdrawnMember() {
            // Given: 탈퇴한 회원
            member = Member.create("M001", "김헥사", "test@example.com", "010-1234-5678");
            member.withdraw();

            // When & Then: 탈퇴한 회원을 활성화하려 할 때 예외 발생
            // 비즈니스 규칙: 탈퇴는 최종 상태로 되돌릴 수 없음
            assertThatThrownBy(() -> member.activate())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("탈퇴한 회원은 활성화할 수 없습니다");
        }

        @Test
        @DisplayName("회원을 탈퇴시킬 수 있다")
        void withdrawMember() {
            // Given: 멤버십이 할당된 활성 회원
            member = Member.create("M001", "김헥사", "test@example.com", "010-1234-5678");

            // 멤버십 할당 (탈퇴 시 멤버십 정보 유지 확인용)
            MembershipPlan plan = MembershipPlan.basicMonthly();
            DateRange period = DateRange.fromTodayFor(30);
            member.assignMembership(plan, period);

            // When: 회원 탈퇴
            // withdraw(): 회원을 탈퇴 상태로 변경
            member.withdraw();

            // Then: 탈퇴 상태 확인 및 멤버십 정보 유지 확인
            assertThat(member.getStatus()).isEqualTo(MemberStatus.WITHDRAWN);
            assertThat(member.getLastStatusChangedAt()).isNotNull();
            // 멤버십 정보는 유지됨 (이력 관리용)
            assertThat(member.getCurrentPlan()).isEqualTo(plan);
            assertThat(member.getMembershipPeriod()).isEqualTo(period);
        }

        @Test
        @DisplayName("정지된 회원도 탈퇴할 수 있다")
        void withdrawSuspendedMember() {
            // Given: 정지된 회원
            member = Member.create("M001", "김헥사", "test@example.com", "010-1234-5678");
            member.suspend("테스트 정지");

            // When: 정지된 회원 탈퇴
            member.withdraw();

            // Then: 탈퇴 상태로 변경됨
            assertThat(member.getStatus()).isEqualTo(MemberStatus.WITHDRAWN);
        }
    }

    @DisplayName("Member 정보 조회 테스트")
    @Nested
    class InformationRetrievalTest {

        private Member member;

        @Test
        @DisplayName("회원 정보 요약을 조회할 수 있다")
        void getMemberSummary() {
            // Given: 멤버십이 할당된 회원
            member = Member.create("M001", "김헥사", "test@example.com", "010-1234-5678");
            MembershipPlan plan = MembershipPlan.basicMonthly();
            DateRange period = DateRange.fromTodayFor(30);
            member.assignMembership(plan, period);

            // When: 회원 정보 요약 조회
            // getSummary(): 회원의 주요 정보를 요약한 문자열 반환
            String summary = member.getSummary();

            // Then: 요약 정보에 주요 내용들이 포함되어 있는지 확인
            assertThat(summary).contains("회원 김헥사");
            assertThat(summary).contains("M001");
            assertThat(summary).contains("활성");
            assertThat(summary).contains("기본 월간권");
        }

        @Test
        @DisplayName("정지된 회원의 요약에는 정지 사유가 포함된다")
        void getSuspendedMemberSummary() {
            // Given: 정지된 회원
            member = Member.create("M001", "김헥사", "test@example.com", "010-1234-5678");
            member.suspend("규정 위반");

            // When: 정지된 회원의 요약 조회
            String summary = member.getSummary();

            // Then: 정지 상태와 정지 사유가 요약에 포함됨
            assertThat(summary).contains("정지");
            assertThat(summary).contains("정지사유: 규정 위반");
        }

        @Test
        @DisplayName("멤버십 남은 일수를 계산할 수 있다")
        void getRemainingMembershipDays() {
            member = Member.create("M001", "김헥사", "test@example.com", "010-1234-5678");
            MembershipPlan plan = MembershipPlan.basicMonthly();

            // 30일 멤버십
            DateRange period = DateRange.fromTodayFor(30);
            member.assignMembership(plan, period);
            // getRemainingMembershipDays(): 멤버십 남은 일수 계산
            assertThat(member.getRemainingMembershipDays()).isEqualTo(30);

            // 만료된 멤버십 (60일 전~30일 전)
            DateRange expiredPeriod = DateRange.of(LocalDate.now().minusDays(60), LocalDate.now().minusDays(30));
            member.assignMembership(plan, expiredPeriod);
            assertThat(member.getRemainingMembershipDays()).isEqualTo(0); // 만료시 0 반환

            // 멤버십 없음
            Member memberWithoutPlan = Member.create("M002", "김노플랜", "no@plan.com", "010-9999-9999");
            assertThat(memberWithoutPlan.getRemainingMembershipDays()).isEqualTo(0);
        }

        @Test
        @DisplayName("멤버십 만료 경고 여부를 확인할 수 있다")
        void isMembershipExpiryWarning() {
            // Given: 5일 후 만료되는 멤버십을 가진 회원
            member = Member.create("M001", "김헥사", "test@example.com", "010-1234-5678");
            MembershipPlan plan = MembershipPlan.basicMonthly();
            DateRange period = DateRange.of(LocalDate.now(), LocalDate.now().plusDays(5));
            member.assignMembership(plan, period);

            // When & Then: 다양한 경고 기간에 대한 만료 경고 여부 확인
            // isMembershipExpiryWarning(): 지정된 일수 내에 만료되는지 확인
            assertThat(member.isMembershipExpiryWarning(7)).isTrue(); // 7일 이내 경고
            assertThat(member.isMembershipExpiryWarning(3)).isFalse(); // 3일 이내 경고는 아직 아님
            assertThat(member.isMembershipExpiryWarning(10)).isTrue(); // 10일 이내 경고
        }
    }

    @DisplayName("Member 동등성 테스트")
    @Nested
    class EqualityTest {

        @Test
        @DisplayName("같은 memberId를 가진 회원들은 동등하다")
        void equalityWithSameMemberId() {
            // Given: 같은 memberId를 가진 서로 다른 속성의 회원들
            Member member1 = Member.create("M001", "김헥사", "hexpass1@example.com", "010-1111-1111");
            Member member2 = Member.create("M001", "박패스", "hexpass2@example.com", "010-2222-2222");

            // When & Then: memberId만 같으면 동등 (엔티티의 특성)
            // equals(): memberId를 기준으로 한 동등성 비교
            assertThat(member1).isEqualTo(member2);
            assertThat(member1.hashCode()).isEqualTo(member2.hashCode());
        }

        @Test
        @DisplayName("다른 memberId를 가진 회원들은 동등하지 않다")
        void inequalityWithDifferentMemberId() {
            // Given: 다른 memberId를 가진 회원들
            Member member1 = Member.create("M001", "김헥사", "test@example.com", "010-1234-5678");
            Member member2 = Member.create("M002", "김헥사", "test@example.com", "010-1234-5678");

            // When & Then: memberId가 다르면 비동등
            assertThat(member1).isNotEqualTo(member2);
        }

        @Test
        @DisplayName("null과는 동등하지 않다")
        void inequalityWithNull() {
            // Given: 회원 객체
            Member member = Member.create("M001", "김헥사", "test@example.com", "010-1234-5678");

            // When & Then: null과 비교
            assertThat(member).isNotEqualTo(null);
        }

        @Test
        @DisplayName("다른 타입 객체와는 동등하지 않다")
        void inequalityWithDifferentType() {
            // Given: Member 객체와 문자열
            Member member = Member.create("M001", "김헥사", "test@example.com", "010-1234-5678");
            String notMember = "M001";

            // When & Then: 타입이 다르면 비동등
            assertThat(member).isNotEqualTo(notMember);
        }
    }

    @DisplayName("Member toString 테스트")
    @Nested
    class ToStringTest {

        @Test
        @DisplayName("toString이 올바른 정보를 포함한다")
        void toStringContainsCorrectInfo() {
            // Given: 회원 객체
            Member member = Member.create("M001", "김헥사", "test@example.com", "010-1234-5678");

            // When: 문자열 변환
            String result = member.toString();

            // Then: 주요 정보들이 포함되어 있는지 확인
            // toString(): 디버깅과 로깅을 위한 문자열 표현
            assertThat(result).contains("M001");
            assertThat(result).contains("김헥사");
            assertThat(result).contains("test@example.com");
            assertThat(result).contains("ACTIVE");
        }
    }
}
```

## 주요 설계 원칙 및 특징

### 1. **도메인 엔티티 (Domain Entity)**
- `memberId`를 식별자로 하는 엔티티 객체
- 회원의 생명주기와 상태를 관리하는 핵심 도메인 객체

### 2. **복잡한 상태 관리**
- **상태 패턴**: ACTIVE, SUSPENDED, WITHDRAWN 상태와 상태 전이 규칙
- **상태 불변성**: 탈퇴는 최종 상태로 되돌릴 수 없음
- **상태 추적**: 상태 변경 시점과 사유를 기록

### 3. **멤버십 생명주기 관리**
- **할당/연장/변경**: 멤버십 플랜과 기간의 동적 관리
- **만료 처리**: 멤버십 만료 여부와 경고 시스템
- **권한 계산**: 멤버십 상태에 따른 서비스 이용 권한 동적 계산

### 4. **비즈니스 규칙 캡슐화**
- **예약 권한**: 멤버십, 리소스 타입, 날짜, 상태를 종합한 예약 가능 여부 판단
- **검증 로직**: 이메일 형식, 전화번호 형식 등의 데이터 유효성 검증
- **정책 적용**: 선예약 제한, 동시 예약 제한 등의 비즈니스 정책

### 5. **방어적 프로그래밍**
- **null 안전성**: 모든 필수 필드에 대한 null 체크
- **상태 검증**: 유효하지 않은 상태 전이 방지
- **데이터 무결성**: 과거 날짜 멤버십 할당 방지 등

### 사용되는 주요 Java 클래스와 기능

#### **정규식 (Regular Expression)**
```java
// 이메일과 전화번호 형식 검증
private static final Pattern EMAIL_PATTERN = Pattern.compile("...");
private static final Pattern PHONE_PATTERN = Pattern.compile("010-\\d{4}-\\d{4}");
```

#### **LocalDate**
```java
// 날짜 기반 멤버십 기간 관리
LocalDate today = LocalDate.now();
LocalDate endDate = today.plusDays(30);
```

#### **Enum 활용**
```java
// 회원 상태와 리소스 타입의 타입 안전성
MemberStatus status = MemberStatus.ACTIVE;
ResourceType resource = ResourceType.GYM;
```

#### **@EnumSource (JUnit 5)**
```java
// Enum의 모든 값 또는 특정 값들로 파라미터화된 테스트
@EnumSource(value = MemberStatus.class, names = {"SUSPENDED", "WITHDRAWN"})
```

### 왜 이런 구조로 설계했는가?

1. **회원 중심 설계**: 모든 비즈니스 로직이 회원을 중심으로 구성
2. **상태 기반 동작**: 회원 상태에 따라 사용 가능한 기능이 동적으로 결정
3. **복잡성 캡슐화**: 멤버십, 권한, 예약 등의 복잡한 로직을 회원 객체 내부로 캡슐화
4. **일관성 보장**: 회원 관련 모든 비즈니스 규칙을 한 곳에서 관리
5. **확장성**: 새로운 회원 상태나 권한을 쉽게 추가할 수 있는 구조

### 테스트에서 확인하는 핵심 비즈니스 로직

1. **데이터 검증**: 이메일, 전화번호 형식 등의 유효성 검사
2. **상태 관리**: 회원 상태 전이와 전이 규칙
3. **멤버십 관리**: 할당, 연장, 변경, 만료 처리
4. **권한 계산**: 복합적인 조건을 고려한 예약 권한 판단
5. **정보 조회**: 요약 정보, 남은 기간, 만료 경고 등의 편의 기능

### 실제 비즈니스에서의 활용

이러한 `Member` 엔티티는 실제 멤버십 관리 시스템에서:
- **고객 서비스**: 회원 상태 조회, 멤버십 연장/변경 처리
- **예약 시스템**: 예약 가능 여부 실시간 검증
- **마케팅**: 만료 임박 회원 대상 리마케팅
- **운영 관리**: 정지/복원 등 회원 상태 관리
  등의 용도로 활용됩니다.