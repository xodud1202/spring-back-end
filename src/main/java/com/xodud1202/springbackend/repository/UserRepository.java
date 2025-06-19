package com.xodud1202.springbackend.repository;

import java.util.Date;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.xodud1202.springbackend.domain.UserBase;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface UserRepository extends JpaRepository<UserBase, Long> {
    Optional<UserBase> findByLoginId(String loginId);

    @Modifying
    @Transactional
    @Query("UPDATE UserBase u SET u.refreshToken = :refreshToken, u.refreshTokenExpiry = :refreshTokenExpiry, u.updNo = :updNo, u.updDt = :updDt, u.accessDt = :accessDt WHERE u.usrNo = :usrNo")
    void updateRefreshToken(@Param("usrNo") Long usrNo, @Param("refreshToken") String refreshToken, @Param("refreshTokenExpiry") Date refreshTokenExpiry, @Param("accessDt") Date accessDt, @Param("updNo") Long updNo, @Param("updDt") Date updDt);
}
