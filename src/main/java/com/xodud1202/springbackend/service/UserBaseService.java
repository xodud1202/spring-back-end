package com.xodud1202.springbackend.service;

import com.xodud1202.springbackend.domain.UserBase;
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
	 * Retrieves a user by their login ID.
	 * @param loginId the login ID of the user to be retrieved
	 * @return an {@code Optional} containing the {@code UserBase} if found, or an empty {@code Optional} if no user exists with the provided login ID
	 */
	public Optional<UserBase> loadUserByLoginId(String loginId) {
		return userRepository.findByLoginId(loginId);
	}
	
	/**
	 * Retrieves a {@code UserBase} entity based on the provided login ID, refresh token, and expiration check.
	 * The method performs a query to find a user whose login ID matches, refresh token matches,
	 * and whose refresh token has not expired.
	 * @param user the {@code UserBase} object containing the login ID and refresh token to be used for the query
	 * @return an {@code Optional} containing the {@code UserBase} if a match is found, or an empty {@code Optional} if no match exists
	 */
	public Optional<UserBase> findUserBaseByLoginIdAndRefreshTokenAndExpiredCheck(UserBase user) {
		String ql = "SELECT u FROM UserBase u WHERE u.loginId = :loginId AND u.refreshToken = :refreshToken AND u.refreshTokenExpiry > CURRENT_TIMESTAMP";
		UserBase result = em.createQuery(ql, UserBase.class)
				.setParameter("loginId", user.getLoginId())
				.setParameter("refreshToken", user.getRefreshToken())
				.getResultStream().findFirst().orElse(null);
		return Optional.ofNullable(result);
	}
}
