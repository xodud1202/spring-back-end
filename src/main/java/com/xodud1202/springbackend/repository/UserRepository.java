package com.xodud1202.springbackend.repository;

import java.util.Date;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.xodud1202.springbackend.entity.UserBaseEntity;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface UserRepository extends JpaRepository<UserBaseEntity, Long> {
    /**
     * Finds a user by their login ID.
     * @param loginId the login ID of the user to be retrieved
     * @return an {@code Optional} containing the {@code UserBase} if a user with the given login ID exists;
     *         otherwise, an empty {@code Optional}
     */
    Optional<UserBaseEntity> findByLoginId(String loginId);

    @Modifying
    @Transactional
    @Query("UPDATE UserBaseEntity u SET u.refreshToken = :refreshToken, u.refreshTokenExpiry = :refreshTokenExpiry, u.udtNo = :udtNo, u.udtDt = :udtDt, u.accessDt = :accessDt WHERE u.usrNo = :usrNo")
    void updateRefreshToken(@Param("usrNo") Long usrNo, @Param("refreshToken") String refreshToken, @Param("refreshTokenExpiry") Date refreshTokenExpiry, @Param("accessDt") Date accessDt, @Param("udtNo") Long udtNo, @Param("udtDt") Date udtDt);
    
    @Modifying
    @Transactional
    @Query("UPDATE UserBaseEntity u SET u.refreshToken = NULL, u.refreshTokenExpiry = NULL, u.udtNo = :usrNo, u.udtDt = CURRENT_TIMESTAMP WHERE u.usrNo = :usrNo")
    void clearRefreshToken(@Param("usrNo") Long usrNo);
}
