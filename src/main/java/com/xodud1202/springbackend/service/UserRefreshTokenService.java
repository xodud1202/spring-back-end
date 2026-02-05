package com.xodud1202.springbackend.service;

import com.xodud1202.springbackend.entity.UserRefreshTokenEntity;
import com.xodud1202.springbackend.repository.UserRefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Optional;

@Service
@RequiredArgsConstructor
// 사용자 리프레시 토큰 비즈니스 로직을 처리합니다.
public class UserRefreshTokenService {
	private final UserRefreshTokenRepository userRefreshTokenRepository;

	// 리프레시 토큰 해시를 생성합니다.
	public String buildRefreshTokenHash(String refreshToken) {
		if (refreshToken == null) {
			return null;
		}
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(refreshToken.getBytes(StandardCharsets.UTF_8));
			StringBuilder builder = new StringBuilder();
			for (byte b : hash) {
				builder.append(String.format("%02x", b));
			}
			return builder.toString();
		} catch (NoSuchAlgorithmException e) {
			return null;
		}
	}

	// 토큰 해시로 토큰을 조회합니다.
	public Optional<UserRefreshTokenEntity> findByHash(String refreshTokenHash) {
		if (refreshTokenHash == null || refreshTokenHash.isBlank()) {
			return Optional.empty();
		}
		return userRefreshTokenRepository.findByRefreshTokenHash(refreshTokenHash);
	}

	// 토큰 정보를 저장합니다.
	public UserRefreshTokenEntity saveToken(UserRefreshTokenEntity token) {
		return userRefreshTokenRepository.save(token);
	}

	// 사용 이력을 갱신합니다.
	public void touchLastUsed(Long tokenId) {
		if (tokenId == null) {
			return;
		}
		userRefreshTokenRepository.touchLastUsed(tokenId, new Date());
	}

	// 사용자 기준으로 토큰을 폐기 처리합니다.
	public void revokeByUsrNo(Long usrNo) {
		if (usrNo == null) {
			return;
		}
		userRefreshTokenRepository.revokeByUsrNo(usrNo);
	}

	// 리프레시 토큰 해시 기준으로 토큰을 폐기 처리합니다.
	public void revokeByHash(String refreshTokenHash) {
		if (refreshTokenHash == null || refreshTokenHash.isBlank()) {
			return;
		}
		userRefreshTokenRepository.revokeByHash(refreshTokenHash);
	}
}
