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

@DisplayName("Member 엔티티 테스트")
class MemberTest {

    @DisplayName("Member 생성 테스트")
    @Nested
    class CreationTest {

        @Test
        @DisplayName("유효한 정보로 회원을 생성할 수 있다")
        void createValidMember() {
            // Given
            String memberId = "M001";
            String name = "김헥사";
            String email = "hexpass@example.com";
            String phone = "010-1234-5678";

            // When
            Member member = Member.create(memberId, name, email, phone);

            // Then
            assertThat(member.getMemberId()).isEqualTo(memberId);
            assertThat(member.getName()).isEqualTo(name);
            assertThat(member.getEmail()).isEqualTo(email);
            assertThat(member.getPhone()).isEqualTo(phone);
            assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE); // 기본값
            assertThat(member.getCreatedAt()).isNotNull();
            assertThat(member.getLastStatusChangedAt()).isNotNull();
            assertThat(member.getCurrentPlan()).isNull(); // 초기에는 플랜 없음
            assertThat(member.getMembershipPeriod()).isNull();
        }

        @ParameterizedTest
        @DisplayName("필수 필드가 null이거나 빈 값이면 예외가 발생한다")
        @MethodSource("provideInvalidStringFields")
        void createWithInvalidStringFields(String memberId, String name, String email, String phone) {
            // When & Then
            assertThatThrownBy(() -> Member.create(memberId, name, email, phone))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("null이거나 빈 값일 수 없습니다");
        }

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
            // When & Then
            assertThatThrownBy(() -> Member.create("M001", "Valid Name", invalidEmail, "010-1234-5678"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("유효하지 않은 이메일 형식입니다");
        }

        @ParameterizedTest
        @DisplayName("잘못된 전화번호 형식으로 생성하면 예외가 발생한다")
        @ValueSource(strings = {"010-123-4567", "010-12345678", "02-1234-5678", "010 1234 5678", "010-1234-567"})
        void createWithInvalidPhoneFormat(String invalidPhone) {
            // When & Then
            assertThatThrownBy(() -> Member.create("M001", "Valid Name", "valid@email.com", invalidPhone))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("전화번호는 010-XXXX-XXXX 형식이어야 합니다");
        }

        @Test
        @DisplayName("유효한 이메일 형식들을 올바르게 처리한다")
        void acceptValidEmailFormats() {
            // Given
            String[] validEmails = {
                    "test@example.com",
                    "user.name@domain.co.kr",
                    "test123@test-domain.com",
                    "user+tag@example.org"
            };

            // When & Then
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
            // Given
            member = Member.create("M001", "김헥사", "test@example.com", "010-1234-5678");
            plan = MembershipPlan.basicMonthly();
            period = DateRange.fromTodayFor(30);

            // When
            member.assignMembership(plan, period);

            // Then
            assertThat(member.getCurrentPlan()).isEqualTo(plan);
            assertThat(member.getMembershipPeriod()).isEqualTo(period);
            assertThat(member.hasMembershipActive()).isTrue();
        }

        @Test
        @DisplayName("null 멤버십 플랜으로 할당하면 예외가 발생한다")
        void assignNullMembershipPlan() {
            // Given
            member = Member.create("M001", "김헥사", "test@example.com", "010-1234-5678");
            period = DateRange.fromTodayFor(30);

            // When & Then
            assertThatThrownBy(() -> member.assignMembership(null, period))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("멤버십 플랜은 null일 수 없습니다");
        }

        @Test
        @DisplayName("null 멤버십 기간으로 할당하면 예외가 발생한다")
        void assignNullMembershipPeriod() {
            // Given
            member = Member.create("M001", "김헥사", "test@example.com", "010-1234-5678");
            plan = MembershipPlan.basicMonthly();

            // When & Then
            assertThatThrownBy(() -> member.assignMembership(plan, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("멤버십 기간은 null일 수 없습니다");
        }

        @Test
        @DisplayName("과거 종료일의 멤버십 기간으로 할당하면 예외가 발생한다")
        void assignPastEndDateMembership() {
            // Given
            member = Member.create("M001", "김헥사", "test@example.com", "010-1234-5678");
            plan = MembershipPlan.basicMonthly();
            DateRange pastPeriod = DateRange.of(LocalDate.now().minusDays(60), LocalDate.now().minusDays(30));

            // When & Then
            assertThatThrownBy(() -> member.assignMembership(plan, pastPeriod))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("멤버십 종료일은 현재 날짜 이후여야 합니다");
        }

        @Test
        @DisplayName("비활성화된 플랜으로 할당하면 예외가 발생한다")
        void assignInactivePlan() {
            // Given
            member = Member.create("M001", "김헥사", "test@example.com", "010-1234-5678");
            plan = MembershipPlan.basicMonthly();
            plan.deactivate();
            period = DateRange.fromTodayFor(30);

            // When & Then
            assertThatThrownBy(() -> member.assignMembership(plan, period))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("비활성화된 플랜은 할당할 수 없습니다");
        }

        @Test
        @DisplayName("멤버십을 연장할 수 있다")
        void extendMembership() {
            // Given
            member = Member.create("M001", "김헥사", "test@example.com", "010-1234-5678");
            plan = MembershipPlan.basicMonthly();
            period = DateRange.fromTodayFor(30);
            member.assignMembership(plan, period);

            // When
            member.extendMembership(15); // 15일 연장

            // Then
            DateRange extendedPeriod = member.getMembershipPeriod();
            assertThat(extendedPeriod.getDays()).isEqualTo(45L); // 30 + 15
            assertThat(extendedPeriod.getEndDate()).isEqualTo(period.getEndDate().plusDays(15));
        }

        @Test
        @DisplayName("멤버십 없이 연장하려 하면 예외가 발생한다")
        void extendMembershipWithoutAssignment() {
            // Given
            member = Member.create("M001", "김헥사", "test@example.com", "010-1234-5678");

            // When & Then
            assertThatThrownBy(() -> member.extendMembership(15))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("멤버십이 할당되지 않은 상태에서는 연장할 수 없습니다");
        }

        @ParameterizedTest
        @DisplayName("0 이하의 일수로 연장하면 예외가 발생한다")
        @ValueSource(ints = {0, -1, -15})
        void extendMembershipWithInvalidDays(int invalidDays) {
            // Given
            member = Member.create("M001", "김헥사", "test@example.com", "010-1234-5678");
            plan = MembershipPlan.basicMonthly();
            period = DateRange.fromTodayFor(30);
            member.assignMembership(plan, period);

            // When & Then
            assertThatThrownBy(() -> member.extendMembership(invalidDays))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("연장 일수는 0보다 커야 합니다");
        }

        @Test
        @DisplayName("멤버십 플랜을 변경할 수 있다")
        void changeMembershipPlan() {
            // Given
            member = Member.create("M001", "김헥사", "test@example.com", "010-1234-5678");
            MembershipPlan basicPlan = MembershipPlan.basicMonthly();
            MembershipPlan premiumPlan = MembershipPlan.premiumMonthly();
            period = DateRange.fromTodayFor(30);
            member.assignMembership(basicPlan, period);

            // When
            member.changePlan(premiumPlan);

            // Then
            assertThat(member.getCurrentPlan()).isEqualTo(premiumPlan);
            assertThat(member.getMembershipPeriod()).isEqualTo(period); // 기간은 그대로
        }

        @Test
        @DisplayName("멤버십 기간 없이 플랜 변경하려 하면 예외가 발생한다")
        void changePlanWithoutMembershipPeriod() {
            // Given
            member = Member.create("M001", "김헥사", "test@example.com", "010-1234-5678");
            MembershipPlan newPlan = MembershipPlan.premiumMonthly();

            // When & Then
            assertThatThrownBy(() -> member.changePlan(newPlan))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("멤버십 기간이 설정되지 않은 상태에서는 플랜을 변경할 수 없습니다");
        }

        @Test
        @DisplayName("멤버십 만료 여부를 올바르게 판단한다")
        void checkMembershipExpiry() {
            member = Member.create("M001", "김헥사", "test@example.com", "010-1234-5678");
            plan = MembershipPlan.basicMonthly();

            // 유효한 멤버십
            DateRange validPeriod = DateRange.fromTodayFor(30);
            member.assignMembership(plan, validPeriod);
            assertThat(member.isMembershipExpired()).isFalse();

            // 만료된 멤버십
            DateRange expiredPeriod = DateRange.of(LocalDate.now().minusDays(60), LocalDate.now().minusDays(30));
            member.assignMembership(plan, expiredPeriod);
            assertThat(member.isMembershipExpired()).isTrue();

            // 멤버십 없음
            Member memberWithoutPlan = Member.create("M002", "김노플랜", "no@plan.com", "010-9999-9999");
            assertThat(memberWithoutPlan.isMembershipExpired()).isTrue();
        }

        @Test
        @DisplayName("멤버십 활성 여부를 종합적으로 판단한다")
        void checkMembershipActiveStatus() {
            member = Member.create("M001", "김헥사", "test@example.com", "010-1234-5678");
            plan = MembershipPlan.basicMonthly();
            period = DateRange.fromTodayFor(30);

            // 정상 활성 상태
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
            // Given
            member = Member.create("M001", "김헥사", "test@example.com", "010-1234-5678");
            plan = MembershipPlan.basicMonthly(); // GYM, STUDY_ROOM 이용 가능
            period = DateRange.fromTodayFor(30);
            member.assignMembership(plan, period);

            LocalDate tomorrow = LocalDate.now().plusDays(1);

            // When & Then
            assertThat(member.canMakeReservation()).isTrue();
            assertThat(member.canReserve(ResourceType.GYM, tomorrow)).isTrue();
            assertThat(member.canReserve(ResourceType.STUDY_ROOM, tomorrow)).isTrue();
        }

        @Test
        @DisplayName("권한 없는 리소스는 예약할 수 없다")
        void cannotReserveUnauthorizedResource() {
            // Given
            member = Member.create("M001", "김헥사", "test@example.com", "010-1234-5678");
            plan = MembershipPlan.basicMonthly(); // GYM, STUDY_ROOM만 이용 가능
            period = DateRange.fromTodayFor(30);
            member.assignMembership(plan, period);

            LocalDate tomorrow = LocalDate.now().plusDays(1);

            // When & Then
            assertThat(member.canReserve(ResourceType.POOL, tomorrow)).isFalse(); // 권한 없음
            assertThat(member.canReserve(ResourceType.SAUNA, tomorrow)).isFalse(); // 권한 없음
            assertThat(member.canReserve(ResourceType.TENNIS_COURT, tomorrow)).isFalse(); // 권한 없음
        }

        @Test
        @DisplayName("멤버십 기간 외의 날짜는 예약할 수 없다")
        void cannotReserveOutsideMembershipPeriod() {
            // Given
            member = Member.create("M001", "김헥사", "test@example.com", "010-1234-5678");
            plan = MembershipPlan.basicMonthly();
            period = DateRange.fromTodayFor(30); // 30일간 유효
            member.assignMembership(plan, period);

            // When & Then
            LocalDate withinPeriod = LocalDate.now().plusDays(15); // 기간 내
            LocalDate outsidePeriod = LocalDate.now().plusDays(35); // 기간 외

            assertThat(member.canReserve(ResourceType.GYM, withinPeriod)).isTrue();
            assertThat(member.canReserve(ResourceType.GYM, outsidePeriod)).isFalse();
        }

        @Test
        @DisplayName("선예약 가능 여부를 확인할 수 있다")
        void checkAdvanceReservation() {
            // Given
            member = Member.create("M001", "김헥사", "test@example.com", "010-1234-5678");
            plan = MembershipPlan.basicMonthly(); // 30일 선예약 가능
            period = DateRange.fromTodayFor(60);
            member.assignMembership(plan, period);

            // When & Then
            LocalDate within30Days = LocalDate.now().plusDays(25);
            LocalDate beyond30Days = LocalDate.now().plusDays(35);

            assertThat(member.canReserveInAdvance(within30Days)).isTrue();
            assertThat(member.canReserveInAdvance(beyond30Days)).isFalse();
        }

        @Test
        @DisplayName("오늘이나 과거 날짜는 선예약 제한 없이 예약 가능하다")
        void noAdvanceLimitForTodayOrPast() {
            // Given
            member = Member.create("M001", "김헥사", "test@example.com", "010-1234-5678");
            plan = MembershipPlan.basicMonthly();
            period = DateRange.fromTodayFor(30);
            member.assignMembership(plan, period);

            // When & Then
            LocalDate today = LocalDate.now();
            LocalDate yesterday = LocalDate.now().minusDays(1);

            assertThat(member.canReserveInAdvance(today)).isTrue();
            assertThat(member.canReserveInAdvance(yesterday)).isTrue();
        }

        @Test
        @DisplayName("동시 예약 가능 여부를 확인할 수 있다")
        void checkSimultaneousReservation() {
            // Given
            member = Member.create("M001", "김헥사", "test@example.com", "010-1234-5678");
            plan = MembershipPlan.basicMonthly(); // 최대 3개 동시 예약
            period = DateRange.fromTodayFor(30);
            member.assignMembership(plan, period);

            // When & Then
            assertThat(member.canReserveSimultaneously(0)).isTrue(); // 0개 → 1개
            assertThat(member.canReserveSimultaneously(2)).isTrue(); // 2개 → 3개
            assertThat(member.canReserveSimultaneously(3)).isFalse(); // 3개 → 4개 (초과)
            assertThat(member.canReserveSimultaneously(5)).isFalse(); // 5개 → 6개 (초과)
        }

        @ParameterizedTest
        @DisplayName("비활성 상태에서는 예약할 수 없다")
        @EnumSource(value = MemberStatus.class, names = {"SUSPENDED", "WITHDRAWN"})
        void cannotReserveWhenInactive(MemberStatus inactiveStatus) {
            // Given
            member = Member.create("M001", "김헥사", "test@example.com", "010-1234-5678");
            plan = MembershipPlan.basicMonthly();
            period = DateRange.fromTodayFor(30);
            member.assignMembership(plan, period);

            // When
            if (inactiveStatus == MemberStatus.SUSPENDED) {
                member.suspend("테스트 정지");
            } else if (inactiveStatus == MemberStatus.WITHDRAWN) {
                member.withdraw();
            }

            // Then
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
            // Given
            member = Member.create("M001", "김헥사", "test@example.com", "010-1234-5678");
            String suspensionReason = "규정 위반";

            // When
            member.suspend(suspensionReason);

            // Then
            assertThat(member.getStatus()).isEqualTo(MemberStatus.SUSPENDED);
            assertThat(member.getSuspensionReason()).isEqualTo(suspensionReason);
            assertThat(member.getLastStatusChangedAt()).isNotNull();
        }

        @Test
        @DisplayName("탈퇴한 회원은 정지할 수 없다")
        void cannotSuspendWithdrawnMember() {
            // Given
            member = Member.create("M001", "김헥사", "test@example.com", "010-1234-5678");
            member.withdraw();

            // When & Then
            assertThatThrownBy(() -> member.suspend("테스트 정지"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("탈퇴한 회원은 정지할 수 없습니다");
        }

        @Test
        @DisplayName("정지 사유 없이 정지하면 예외가 발생한다")
        void cannotSuspendWithoutReason() {
            // Given
            member = Member.create("M001", "김헥사", "test@example.com", "010-1234-5678");

            // When & Then
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
            // Given
            member = Member.create("M001", "김헥사", "test@example.com", "010-1234-5678");
            member.suspend("테스트 정지");
            assertThat(member.getSuspensionReason()).isNotNull();

            // When
            member.activate();

            // Then
            assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
            assertThat(member.getSuspensionReason()).isNull(); // 정지 사유 초기화
            assertThat(member.getLastStatusChangedAt()).isNotNull();
        }

        @Test
        @DisplayName("탈퇴한 회원은 활성화할 수 없다")
        void cannotActivateWithdrawnMember() {
            // Given
            member = Member.create("M001", "김헥사", "test@example.com", "010-1234-5678");
            member.withdraw();

            // When & Then
            assertThatThrownBy(() -> member.activate())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("탈퇴한 회원은 활성화할 수 없습니다");
        }

        @Test
        @DisplayName("회원을 탈퇴시킬 수 있다")
        void withdrawMember() {
            // Given
            member = Member.create("M001", "김헥사", "test@example.com", "010-1234-5678");

            // 멤버십 할당 (탈퇴 시 멤버십 정보 유지 확인용)
            MembershipPlan plan = MembershipPlan.basicMonthly();
            DateRange period = DateRange.fromTodayFor(30);
            member.assignMembership(plan, period);

            // When
            member.withdraw();

            // Then
            assertThat(member.getStatus()).isEqualTo(MemberStatus.WITHDRAWN);
            assertThat(member.getLastStatusChangedAt()).isNotNull();
            // 멤버십 정보는 유지됨 (이력 관리용)
            assertThat(member.getCurrentPlan()).isEqualTo(plan);
            assertThat(member.getMembershipPeriod()).isEqualTo(period);
        }

        @Test
        @DisplayName("정지된 회원도 탈퇴할 수 있다")
        void withdrawSuspendedMember() {
            // Given
            member = Member.create("M001", "김헥사", "test@example.com", "010-1234-5678");
            member.suspend("테스트 정지");

            // When
            member.withdraw();

            // Then
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
            // Given
            member = Member.create("M001", "김헥사", "test@example.com", "010-1234-5678");
            MembershipPlan plan = MembershipPlan.basicMonthly();
            DateRange period = DateRange.fromTodayFor(30);
            member.assignMembership(plan, period);

            // When
            String summary = member.getSummary();

            // Then
            assertThat(summary).contains("회원 김헥사");
            assertThat(summary).contains("M001");
            assertThat(summary).contains("활성");
            assertThat(summary).contains("기본 월간권");
        }

        @Test
        @DisplayName("정지된 회원의 요약에는 정지 사유가 포함된다")
        void getSuspendedMemberSummary() {
            // Given
            member = Member.create("M001", "김헥사", "test@example.com", "010-1234-5678");
            member.suspend("규정 위반");

            // When
            String summary = member.getSummary();

            // Then
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
            assertThat(member.getRemainingMembershipDays()).isEqualTo(30);

            // 만료된 멤버십
            DateRange expiredPeriod = DateRange.of(LocalDate.now().minusDays(60), LocalDate.now().minusDays(30));
            member.assignMembership(plan, expiredPeriod);
            assertThat(member.getRemainingMembershipDays()).isEqualTo(0);

            // 멤버십 없음
            Member memberWithoutPlan = Member.create("M002", "김노플랜", "no@plan.com", "010-9999-9999");
            assertThat(memberWithoutPlan.getRemainingMembershipDays()).isEqualTo(0);
        }

        @Test
        @DisplayName("멤버십 만료 경고 여부를 확인할 수 있다")
        void isMembershipExpiryWarning() {
            // Given - 5일 후 만료
            member = Member.create("M001", "김헥사", "test@example.com", "010-1234-5678");
            MembershipPlan plan = MembershipPlan.basicMonthly();
            DateRange period = DateRange.of(LocalDate.now(), LocalDate.now().plusDays(5));
            member.assignMembership(plan, period);

            // When & Then
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
            // Given
            Member member1 = Member.create("M001", "김헥사", "hexpass1@example.com", "010-1111-1111");
            Member member2 = Member.create("M001", "박패스", "hexpass2@example.com", "010-2222-2222");

            // When & Then
            assertThat(member1).isEqualTo(member2);
            assertThat(member1.hashCode()).isEqualTo(member2.hashCode());
        }

        @Test
        @DisplayName("다른 memberId를 가진 회원들은 동등하지 않다")
        void inequalityWithDifferentMemberId() {
            // Given
            Member member1 = Member.create("M001", "김헥사", "test@example.com", "010-1234-5678");
            Member member2 = Member.create("M002", "김헥사", "test@example.com", "010-1234-5678");

            // When & Then
            assertThat(member1).isNotEqualTo(member2);
        }

        @Test
        @DisplayName("null과는 동등하지 않다")
        void inequalityWithNull() {
            // Given
            Member member = Member.create("M001", "김헥사", "test@example.com", "010-1234-5678");

            // When & Then
            assertThat(member).isNotEqualTo(null);
        }

        @Test
        @DisplayName("다른 타입 객체와는 동등하지 않다")
        void inequalityWithDifferentType() {
            // Given
            Member member = Member.create("M001", "김헥사", "test@example.com", "010-1234-5678");
            String notMember = "M001";

            // When & Then
            assertThat(member).isNotEqualTo(notMember);
        }
    }

    @DisplayName("Member toString 테스트")
    @Nested
    class ToStringTest {

        @Test
        @DisplayName("toString이 올바른 정보를 포함한다")
        void toStringContainsCorrectInfo() {
            // Given
            Member member = Member.create("M001", "김헥사", "test@example.com", "010-1234-5678");

            // When
            String result = member.toString();

            // Then
            assertThat(result).contains("M001");
            assertThat(result).contains("김헥사");
            assertThat(result).contains("test@example.com");
            assertThat(result).contains("ACTIVE");
        }
    }
}