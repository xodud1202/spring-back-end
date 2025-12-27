package com.xodud1202.springbackend.service;

import com.xodud1202.springbackend.entity.UserBaseEntity;
import com.xodud1202.springbackend.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserBaseService {
	
	@PersistenceContext
	private EntityManager em;
	
	private final UserRepository userRepository;

	/**
	 * 주어진 로그인 ID를 기반으로 사용자를 조회합니다.
	 * @param loginId 조회할 사용자의 로그인 ID
	 * @return 주어진 로그인 ID와 일치하는 {@code UserBaseEntity}를 포함하는 {@code Optional}.
	 *         사용자를 찾을 수 없는 경우 빈 {@code Optional}을 반환합니다.
	 */
	public Optional<UserBaseEntity> loadUserByLoginId(String loginId) {
		return userRepository.findByLoginId(loginId);
	}
	
	/**
	 * Retrieves a {@code UserBase} entity based on the provided login ID, refresh token, and expiration check.
	 * The method performs a query to find a user whose login ID matches, refresh token matches,
	 * and whose refresh token has not expired.
	 * @param user the {@code UserBase} object containing the login ID and refresh token to be used for the query
	 * @return an {@code Optional} containing the {@code UserBase} if a match is found, or an empty {@code Optional} if no match exists
	 */
	public Optional<UserBaseEntity> findUserBaseByLoginIdAndRefreshTokenAndExpiredCheck(UserBaseEntity user) {
		String ql = "SELECT u FROM UserBaseEntity u WHERE u.loginId = :loginId AND u.refreshToken = :refreshToken AND u.refreshTokenExpiry > CURRENT_TIMESTAMP";
		UserBaseEntity result = em.createQuery(ql, UserBaseEntity.class)
				.setParameter("loginId", user.getLoginId())
				.setParameter("refreshToken", user.getRefreshToken())
				.getResultStream().findFirst().orElse(null);
		return Optional.ofNullable(result);
	}
}
