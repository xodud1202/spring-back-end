package com.xodud1202.springbackend.repository;

import com.xodud1202.springbackend.entity.UserRefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Optional;

@Repository
// 사용자 리프레시 토큰 저장소를 정의합니다.
public interface UserRefreshTokenRepository extends JpaRepository<UserRefreshTokenEntity, Long> {
	// 리프레시 토큰 해시로 토큰 정보를 조회합니다.
	Optional<UserRefreshTokenEntity> findByRefreshTokenHash(String refreshTokenHash);

	// 사용자 기준으로 리프레시 토큰을 폐기 처리합니다.
	@Modifying
	@Transactional
	@Query("UPDATE UserRefreshTokenEntity t SET t.isRevoked = 'Y', t.udtDt = CURRENT_TIMESTAMP WHERE t.usrNo = :usrNo")
	int revokeByUsrNo(@Param("usrNo") Long usrNo);

	// 리프레시 토큰 해시 기준으로 토큰을 폐기 처리합니다.
	@Modifying
	@Transactional
	@Query("UPDATE UserRefreshTokenEntity t SET t.isRevoked = 'Y', t.udtDt = CURRENT_TIMESTAMP WHERE t.refreshTokenHash = :refreshTokenHash")
	int revokeByHash(@Param("refreshTokenHash") String refreshTokenHash);

	// 토큰 사용 시 마지막 사용일시를 갱신합니다.
	@Modifying
	@Transactional
	@Query("UPDATE UserRefreshTokenEntity t SET t.lastUsedAt = :lastUsedAt, t.udtDt = CURRENT_TIMESTAMP WHERE t.tokenId = :tokenId")
	int touchLastUsed(@Param("tokenId") Long tokenId, @Param("lastUsedAt") Date lastUsedAt);
}
