package com.xodud1202.springbackend.controller.bo;

import com.xodud1202.springbackend.common.response.ApiMessageResponse;
import com.xodud1202.springbackend.config.properties.JwtProperties;
import com.xodud1202.springbackend.domain.LoginRequest;
import com.xodud1202.springbackend.domain.admin.common.AdminAuthResponse;
import com.xodud1202.springbackend.domain.admin.common.UserInfoVO;
import com.xodud1202.springbackend.entity.UserBaseEntity;
import com.xodud1202.springbackend.entity.UserRefreshTokenEntity;
import com.xodud1202.springbackend.repository.UserRepository;
import com.xodud1202.springbackend.security.AuthCookieFactory;
import com.xodud1202.springbackend.security.JwtTokenProvider;
import com.xodud1202.springbackend.service.UserBaseService;
import com.xodud1202.springbackend.service.UserRefreshTokenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Date;
import java.util.Optional;

// 백오피스 인증/토큰 관련 API를 처리하는 컨트롤러입니다.
@RestController
@RequiredArgsConstructor
@Slf4j
public class AdminAuthController {
	private static final String AUTH_FAILED_RESULT = "AUTH_FAILED";
	private static final String AUTH_FAILED_MESSAGE = "아이디 또는 비밀번호가 일치하지 않습니다.";
	
	private final AuthenticationManager authenticationManager;
	private final JwtTokenProvider tokenProvider;
	private final UserBaseService userBaseService;
	private final UserRefreshTokenService userRefreshTokenService;
	private final UserRepository userRepository;
	private final JwtProperties jwtProperties;
	private final AuthCookieFactory authCookieFactory;
	
	// 백오피스 로그인 처리와 토큰 발급을 수행합니다.
	@PostMapping("/api/backoffice/login")
	public ResponseEntity<AdminAuthResponse> authenticateUser(
			@Valid @RequestBody LoginRequest loginRequest,
			HttpServletRequest request,
			HttpServletResponse response
	) {
		try {
			// 먼저 사용자가 존재하는지 확인
			Optional<UserBaseEntity> existingUser = userBaseService.loadUserByLoginId(loginRequest.loginId());
			if (existingUser.isEmpty()) {
				// 계정 존재 여부가 외부 응답으로 노출되지 않도록 공통 인증 실패 응답을 반환합니다.
				log.warn("백오피스 로그인 실패 reason=user_not_found");
				return authenticationFailedResponse();
			}
			
			// 사용자가 존재하면 비밀번호 검증 시도
			Authentication authentication = authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(
							loginRequest.loginId(),
							loginRequest.pwd()
					)
			);
			
			SecurityContextHolder.getContext().setAuthentication(authentication);
			
			// Access Token 생성
			String accessToken = tokenProvider.generateAccessToken(authentication);
			
			// 사용자 정보 가져오기
			UserBaseEntity user = existingUser.get();
			
			// 로그인 유지를 선택한 경우 Refresh Token 생성
			if (loginRequest.rememberMe()) {
				String refreshToken = tokenProvider.generateRefreshToken(user.getUsername());
				Date refreshTokenExpiry = new Date(System.currentTimeMillis() + jwtProperties.refreshTokenExpiration().toMillis());
				// 신규 리프레시 토큰을 저장합니다.
				String refreshTokenHash = userRefreshTokenService.buildRefreshTokenHash(refreshToken);
				UserRefreshTokenEntity tokenEntity = new UserRefreshTokenEntity();
				tokenEntity.issue(
					user.getUsrNo(),
					refreshTokenHash,
					refreshTokenExpiry,
					new Date(),
					request.getRemoteAddr(),
					request.getHeader("User-Agent")
				);
				userRefreshTokenService.saveToken(tokenEntity);

				setRefreshTokenCookie(response, refreshToken);
			} else {
				// 로그인 유지 미선택 시 기존 쿠키 토큰만 폐기합니다.
				String existingRefreshToken = extractRefreshTokenFromCookies(request);
				if (StringUtils.hasText(existingRefreshToken)) {
					String refreshTokenHash = userRefreshTokenService.buildRefreshTokenHash(existingRefreshToken);
					userRefreshTokenService.revokeByHash(refreshTokenHash);
				}
				clearRefreshTokenCookie(response);
			}

			user.erasePassword();       // 비밀번호 정보는 제외
			// 반환할 사용자 정보를 포함한 성공 응답을 반환합니다.
			return ResponseEntity.ok(AdminAuthResponse.loginSuccess(accessToken, user));
		} catch (BadCredentialsException e) {
			// 비밀번호 불일치도 계정 미존재와 동일한 외부 응답으로 통합합니다.
			log.warn("백오피스 로그인 실패 reason=bad_credentials");
			return authenticationFailedResponse();
		} catch (Exception e) {
			// 기타 예외는 기존 계약과 동일한 결과코드/메시지로 반환합니다.
			return internalServerErrorResponse("ERROR", e.getMessage());
		}
	}
	
	// 액세스 토큰 만료 시 리프레시 토큰으로 재발급합니다.
	@GetMapping("/api/token/backoffice/access-token")
	public ResponseEntity<AdminAuthResponse> token(
			HttpServletRequest request,
			HttpServletResponse response
	) {
		// 액세스 토큰 검증 흐름: 토큰 유효, 만료, 부적합 처리
		String accessToken = extractAccessTokenFromHeader(request);
		// AccessToken이 있는 경우에만 유효성 검사를 수행합니다.
		if (StringUtils.hasText(accessToken)) {
			String validationResult = tokenProvider.validateCheckToken(accessToken);

			// 유효하고 타입이 AccessToken이면 그대로 반환합니다.
			if ("OK".equals(validationResult) && tokenProvider.isAccessToken(accessToken)) {
				Long usrNo = resolveUsrNoByAccessToken(accessToken);
				return ResponseEntity.ok(AdminAuthResponse.accessTokenSuccess(accessToken, usrNo));
			}

			// AccessToken이 만료/오류이거나 타입이 AccessToken이 아닌 경우 refreshToken 유효 시 재발급을 시도합니다.
		}

		String refreshToken = extractRefreshTokenFromCookies(request);
		if (!StringUtils.hasText(refreshToken)) {
			clearRefreshTokenCookie(response);
			return unauthorizedResponse("INVALID_REFRESH_TOKEN", "유효하지 않은 리프레시 토큰입니다.");
		}
		// 쿠키 토큰이 RefreshToken 타입인지 확인합니다.
		if (!tokenProvider.isRefreshToken(refreshToken)) {
			clearRefreshTokenCookie(response);
			return unauthorizedResponse("INVALID_REFRESH_TOKEN", "유효하지 않은 리프레시 토큰입니다.");
		}
		// refreshToken 해시를 조회합니다.
		String refreshTokenHash = userRefreshTokenService.buildRefreshTokenHash(refreshToken);
		Optional<UserRefreshTokenEntity> tokenInfo = userRefreshTokenService.findByHash(refreshTokenHash);
		if (tokenInfo.isEmpty()) {
			clearRefreshTokenCookie(response);
			return unauthorizedResponse("INVALID_REFRESH_TOKEN", "유효하지 않은 리프레시 토큰입니다.");
		}
		UserRefreshTokenEntity tokenEntity = tokenInfo.get();
		// 토큰 만료 및 폐기 여부를 확인합니다.
		if (!"N".equalsIgnoreCase(tokenEntity.getIsRevoked()) || tokenEntity.getExpiresAt() == null
				|| tokenEntity.getExpiresAt().before(new Date())) {
			clearRefreshTokenCookie(response);
			return unauthorizedResponse("INVALID_REFRESH_TOKEN", "유효하지 않은 리프레시 토큰입니다.");
		}
		// 사용자 정보를 조회합니다.
		Optional<UserBaseEntity> userOpt = userRepository.findById(tokenEntity.getUsrNo());
		if (userOpt.isEmpty()) {
			clearRefreshTokenCookie(response);
			return unauthorizedResponse("INVALID_REFRESH_TOKEN", "유효하지 않은 리프레시 토큰입니다.");
		}
		UserBaseEntity user = userOpt.get();
		String loginId = user.getLoginId();
		if (!StringUtils.hasText(loginId)) {
			clearRefreshTokenCookie(response);
			return unauthorizedResponse("INVALID_REFRESH_TOKEN", "리프레시 토큰의 사용자 정보를 확인할 수 없습니다.");
		}
		// 새로운 AccessToken과 RefreshToken을 발급합니다.
		String newAccessToken = tokenProvider.generateAccessTokenByLoginId(loginId);
		String rotatedRefreshToken = tokenProvider.generateRefreshToken(loginId);
		Date newRefreshExpiry = new Date(System.currentTimeMillis() + jwtProperties.refreshTokenExpiration().toMillis());
		String rotatedHash = userRefreshTokenService.buildRefreshTokenHash(rotatedRefreshToken);
		tokenEntity.rotate(rotatedHash, newRefreshExpiry, new Date());
		userRefreshTokenService.saveToken(tokenEntity);
		setRefreshTokenCookie(response, rotatedRefreshToken);
		userRefreshTokenService.touchLastUsed(tokenEntity.getTokenId());

		return ResponseEntity.ok(AdminAuthResponse.accessTokenSuccess(newAccessToken, user.getUsrNo()));
	}
	
	// 로그아웃 시 현재 세션의 리프레시 토큰만 폐기합니다.
	@PostMapping("/api/backoffice/logout")
	public ResponseEntity<AdminAuthResponse> logout(HttpServletRequest request, HttpServletResponse response) {
		String refreshToken = extractRefreshTokenFromCookies(request);
		if (StringUtils.hasText(refreshToken)) {
			String refreshTokenHash = userRefreshTokenService.buildRefreshTokenHash(refreshToken);
			userRefreshTokenService.revokeByHash(refreshTokenHash);
		}

		clearRefreshTokenCookie(response);
		return ResponseEntity.ok(AdminAuthResponse.logoutSuccess());
	}

	// 로그인 사용자 정보를 조회합니다.
	@GetMapping("/api/backoffice/user/info")
	public ResponseEntity<?> getLoginUserInfo(@RequestParam Long usrNo) {
		if (usrNo == null) {
			return ResponseEntity.badRequest().body(new ApiMessageResponse("사용자 정보를 확인해주세요."));
		}
		Optional<UserInfoVO> info = userBaseService.getUserInfoByUsrNo(usrNo);
		if (info.isEmpty()) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(info.get());
	}

	// refreshToken을 httpOnly 쿠키로 클라이언트에 전송합니다.
	private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
		response.addHeader(HttpHeaders.SET_COOKIE, authCookieFactory.createRefreshTokenCookie(refreshToken).toString());
	}

	// 쿠키를 비우기 위한 TTL 0 설정
	private void clearRefreshTokenCookie(HttpServletResponse response) {
		response.addHeader(HttpHeaders.SET_COOKIE, authCookieFactory.createExpiredRefreshTokenCookie().toString());
	}

	// Authorization 헤더에서 Bearer 토큰을 추출합니다.
	private String extractAccessTokenFromHeader(HttpServletRequest request) {
		String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
			return bearerToken.substring(7);
		}
		return null;
	}

	// AccessToken의 subject(loginId)로 사용자 번호를 조회합니다.
	private Long resolveUsrNoByAccessToken(String accessToken) {
		try {
			String loginId = tokenProvider.getUsernameFromJWT(accessToken);
			if (!StringUtils.hasText(loginId)) {
				return null;
			}
			Optional<UserBaseEntity> userOpt = userBaseService.loadUserByLoginId(loginId);
			return userOpt.map(UserBaseEntity::getUsrNo).orElse(null);
		} catch (Exception e) {
			return null;
		}
	}

	// 쿠키 목록에서 refreshToken 값을 찾습니다.
	private String extractRefreshTokenFromCookies(HttpServletRequest request) {
		if (request.getCookies() == null) {
			return null;
		}
		return Arrays.stream(request.getCookies())
				.filter(cookie -> "refreshToken".equals(cookie.getName()))
				.findFirst()
				.map(Cookie::getValue)
				.orElse(null);
	}

	// 인증 실패 공통 응답을 생성합니다.
	private ResponseEntity<AdminAuthResponse> unauthorizedResponse(String result, String resultMsg) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(AdminAuthResponse.failure(result, resultMsg));
	}

	// 로그인 인증 실패 공통 응답을 생성합니다.
	private ResponseEntity<AdminAuthResponse> authenticationFailedResponse() {
		return unauthorizedResponse(AUTH_FAILED_RESULT, AUTH_FAILED_MESSAGE);
	}

	// 서버 오류 공통 응답을 생성합니다.
	private ResponseEntity<AdminAuthResponse> internalServerErrorResponse(String result, String resultMsg) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(AdminAuthResponse.failure(result, resultMsg));
	}

}
