package com.hexapass.adapter.persistence.memory;

import com.hexapass.domain.model.Member;
import com.hexapass.domain.port.outbound.MemberRepository;
import com.hexapass.domain.type.MemberStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 회원 저장소의 인메모리 구현체
 * 헥사고날 아키텍처에서 아웃바운드 어댑터 역할
 */
public class InMemoryMemberRepository implements MemberRepository {

    private final Map<String, Member> members = new ConcurrentHashMap<>();

    @Override
    public Optional<Member> findById(String memberId) {
        return Optional.ofNullable(members.get(memberId));
    }

    @Override
    public Optional<Member> findByEmail(String email) {
        return members.values().stream()
                .filter(member -> email.equals(member.getEmail()))
                .findFirst();
    }

    @Override
    public Optional<Member> findByPhone(String phone) {
        return members.values().stream()
                .filter(member -> phone.equals(member.getPhone()))
                .findFirst();
    }

    @Override
    public void save(Member member) {
        members.put(member.getMemberId(), member);
    }

    @Override
    public void delete(String memberId) {
        members.remove(memberId);
    }

    @Override
    public boolean existsById(String memberId) {
        return members.containsKey(memberId);
    }

    @Override
    public boolean existsByEmail(String email) {
        return members.values().stream()
                .anyMatch(member -> email.equals(member.getEmail()));
    }

    @Override
    public List<Member> findAll() {
        return new ArrayList<>(members.values());
    }

    @Override
    public List<Member> findByStatus(MemberStatus status) {
        return members.values().stream()
                .filter(member -> status.equals(member.getStatus()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Member> findMembersExpiringWithin(int days) {
        LocalDate cutoffDate = LocalDate.now().plusDays(days);

        return members.values().stream()
                .filter(member -> {
                    if (member.getMembershipPeriod() == null) {
                        return false;
                    }
                    LocalDate endDate = member.getMembershipPeriod().getEndDate();
                    return !endDate.isAfter(cutoffDate);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Member> findMembersWithExpiredMembership() {
        return members.values().stream()
                .filter(Member::isMembershipExpired)
                .collect(Collectors.toList());
    }

    @Override
    public List<Member> findByMembershipPlan(String planId) {
        return members.values().stream()
                .filter(member -> {
                    if (member.getCurrentPlan() == null) {
                        return false;
                    }
                    return planId.equals(member.getCurrentPlan().getPlanId());
                })
                .collect(Collectors.toList());
    }

    @Override
    public long countAll() {
        return members.size();
    }

    @Override
    public long countByStatus(MemberStatus status) {
        return members.values().stream()
                .filter(member -> status.equals(member.getStatus()))
                .count();
    }

    @Override
    public long countMembersJoinedAfter(LocalDate date) {
        return members.values().stream()
                .filter(member -> member.getCreatedAt().toLocalDate().isAfter(date))
                .count();
    }

    @Override
    public long countMembersJoinedBetween(LocalDate startDate, LocalDate endDate) {
        return members.values().stream()
                .filter(member -> {
                    LocalDate joinDate = member.getCreatedAt().toLocalDate();
                    return !joinDate.isBefore(startDate) && !joinDate.isAfter(endDate);
                })
                .count();
    }

    @Override
    public void saveAll(List<Member> memberList) {
        for (Member member : memberList) {
            save(member);
        }
    }

    @Override
    public List<Member> findAll(int page, int size) {
        List<Member> allMembers = findAll();
        int start = page * size;
        int end = Math.min(start + size, allMembers.size());

        if (start >= allMembers.size()) {
            return new ArrayList<>();
        }

        return allMembers.subList(start, end);
    }

    @Override
    public List<Member> findByStatus(MemberStatus status, int page, int size) {
        List<Member> statusMembers = findByStatus(status);
        int start = page * size;
        int end = Math.min(start + size, statusMembers.size());

        if (start >= statusMembers.size()) {
            return new ArrayList<>();
        }

        return statusMembers.subList(start, end);
    }

    // 개발/테스트용 유틸리티 메서드들
    public void clear() {
        members.clear();
    }

    public int size() {
        return members.size();
    }

    public boolean isEmpty() {
        return members.isEmpty();
    }
}