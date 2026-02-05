package com.xodud1202.springbackend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.xodud1202.springbackend.entity.UserBaseEntity;

// 사용자 기본 정보를 조회하는 리포지토리입니다.
@Repository
public interface UserRepository extends JpaRepository<UserBaseEntity, Long> {
    /**
     * 로그인 ID로 사용자 정보를 조회합니다.
     * @param loginId 로그인 ID
     * @return 사용자 정보
     */
    Optional<UserBaseEntity> findByLoginId(String loginId);
}
