package com.hexapass.domain.port.outbound;

import com.hexapass.domain.model.Member;
import com.hexapass.domain.type.MemberStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 회원 정보 저장소 포트 (Outbound Port)
 * 헥사고날 아키텍처에서 도메인이 외부 저장소에 의존하기 위한 추상화
 * 구현체는 InMemory, JPA, MyBatis 등으로 교체 가능
 */
public interface MemberRepository {

    // =========================
    // 기본 CRUD 연산
    // =========================

    /**
     * 회원 ID로 회원 조회
     */
    Optional<Member> findById(String memberId);

    /**
     * 이메일로 회원 조회 (중복 가입 방지용)
     */
    Optional<Member> findByEmail(String email);

    /**
     * 전화번호로 회원 조회
     */
    Optional<Member> findByPhone(String phone);

    /**
     * 회원 저장 (생성/수정)
     */
    void save(Member member);

    /**
     * 회원 삭제 (물리적 삭제)
     */
    void delete(String memberId);

    /**
     * 회원 존재 여부 확인
     */
    boolean existsById(String memberId);

    /**
     * 이메일 중복 여부 확인
     */
    boolean existsByEmail(String email);

    // =========================
    // 조건별 조회
    // =========================

    /**
     * 모든 회원 조회
     */
    List<Member> findAll();

    /**
     * 상태별 회원 조회
     */
    List<Member> findByStatus(MemberStatus status);

    /**
     * 활성 회원 목록 조회
     */
    default List<Member> findActiveMembers() {
        return findByStatus(MemberStatus.ACTIVE);
    }

    /**
     * 정지된 회원 목록 조회
     */
    default List<Member> findSuspendedMembers() {
        return findByStatus(MemberStatus.SUSPENDED);
    }

    /**
     * 멤버십 만료 예정 회원 조회 (지정된 일수 내 만료)
     */
    List<Member> findMembersExpiringWithin(int days);

    /**
     * 멤버십이 만료된 회원 조회
     */
    List<Member> findMembersWithExpiredMembership();

    /**
     * 특정 멤버십 플랜을 사용하는 회원 조회
     */
    List<Member> findByMembershipPlan(String planId);

    // =========================
    // 통계 및 집계
    // =========================

    /**
     * 전체 회원 수
     */
    long countAll();

    /**
     * 상태별 회원 수
     */
    long countByStatus(MemberStatus status);

    /**
     * 특정 날짜 이후 가입한 회원 수
     */
    long countMembersJoinedAfter(LocalDate date);

    /**
     * 특정 기간 동안 가입한 회원 수
     */
    long countMembersJoinedBetween(LocalDate startDate, LocalDate endDate);

    // =========================
    // 배치 처리용 메서드들
    // =========================

    /**
     * 여러 회원 일괄 저장
     */
    void saveAll(List<Member> members);

    /**
     * 페이징 처리된 회원 목록 조회
     */
    List<Member> findAll(int page, int size);

    /**
     * 상태별 페이징 처리된 회원 목록 조회
     */
    List<Member> findByStatus(MemberStatus status, int page, int size);
}