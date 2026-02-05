package com.xodud1202.springbackend.controller;

import com.xodud1202.springbackend.domain.LoginRequest;
import com.xodud1202.springbackend.domain.admin.common.UserInfoVO;
import com.xodud1202.springbackend.entity.UserBaseEntity;
import com.xodud1202.springbackend.entity.UserRefreshTokenEntity;
import com.xodud1202.springbackend.repository.UserRepository;
import com.xodud1202.springbackend.security.JwtTokenProvider;
import com.xodud1202.springbackend.service.UserBaseService;
import com.xodud1202.springbackend.service.UserRefreshTokenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
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

import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

// 백오피스 인증/토큰 관련 API를 처리하는 컨트롤러입니다.
@RestController
@RequiredArgsConstructor
public class AdminAuthController {
	
	private final AuthenticationManager authenticationManager;
	private final JwtTokenProvider tokenProvider;
	private final UserBaseService userBaseService;
	private final UserRefreshTokenService userRefreshTokenService;
	private final UserRepository userRepository;

	@Value("${jwt.refresh-token-expiration}")
    private long jwtRefreshTokenExpirationInMs;
	
	@Value("${jwt.cookie-secure:false}")
	private boolean jwtCookieSecure;
	
	// 백오피스 로그인 처리와 토큰 발급을 수행합니다.
	@PostMapping("/api/backoffice/login")
	public ResponseEntity<?> authenticateUser(
			@RequestBody LoginRequest loginRequest,
			HttpServletRequest request,
			HttpServletResponse response
	) {
		try {
			// 먼저 사용자가 존재하는지 확인
			Optional<UserBaseEntity> existingUser = userBaseService.loadUserByLoginId(loginRequest.getLoginId());
			if (existingUser.isEmpty()) {
				// 사용자가 존재하지 않는 경우
				Map<String, String> payload = new HashMap<>();
				payload.put("result", "NOT_FOUND_ID");
				payload.put("resultMsg", "계정정보가 존재하지 않습니다.");
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(payload);
			}
			
			// 사용자가 존재하면 비밀번호 검증 시도
			Authentication authentication = authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(
							loginRequest.getLoginId(),
							loginRequest.getPwd()
					)
			);
			
			SecurityContextHolder.getContext().setAuthentication(authentication);
			
			// Access Token 생성
			String accessToken = tokenProvider.generateAccessToken(authentication);
			
			// 사용자 정보 가져오기
			UserBaseEntity user = existingUser.get();
			// accessToken은 응답으로만 전달합니다.
			
			// 로그인 결과를 담을 응답 맵
			Map<String, Object> payload = new HashMap<>();
			payload.put("result", "OK");
			payload.put("accessToken", accessToken);
			
			// 로그인 유지를 선택한 경우 Refresh Token 생성
			if (loginRequest.isRememberMe()) {
				String refreshToken = tokenProvider.generateRefreshToken(user.getUsername());
				Date refreshTokenExpiry = new Date(System.currentTimeMillis() + jwtRefreshTokenExpirationInMs);
				// 신규 리프레시 토큰을 저장합니다.
				String refreshTokenHash = userRefreshTokenService.buildRefreshTokenHash(refreshToken);
				UserRefreshTokenEntity tokenEntity = new UserRefreshTokenEntity();
				tokenEntity.setUsrNo(user.getUsrNo());
				tokenEntity.setRefreshTokenHash(refreshTokenHash);
				tokenEntity.setExpiresAt(refreshTokenExpiry);
				tokenEntity.setLastUsedAt(new Date());
				tokenEntity.setClientIp(request.getRemoteAddr());
				tokenEntity.setUserAgent(request.getHeader("User-Agent"));
				tokenEntity.setIsRevoked("N");
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

			user.setPwd(null);       // 비밀번호 정보는 제외
			// 반환할 사용자 정보를 설정
			payload.put("userInfo", user);
			
			return ResponseEntity.ok(payload);
		} catch (BadCredentialsException e) {
			// 비밀번호가 일치하지 않는 경우
			Map<String, String> payload = new HashMap<>();
			payload.put("result", "NOT_CORRECT_PW");
			payload.put("resultMsg", "비밀번호가 일치하지 않습니다.");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(payload);
		} catch (Exception e) {
			// 기타 예외 처리
			Map<String, String> payload = new HashMap<>();
			payload.put("result", "ERROR");
			payload.put("resultMsg", e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(payload);
		}
	}
	
	// 액세스 토큰 만료 시 리프레시 토큰으로 재발급합니다.
	@GetMapping("/api/token/backoffice/access-token")
	public ResponseEntity<?> token(
			HttpServletRequest request,
			HttpServletResponse response,
			@RequestParam(value = "loginId", required = false) String loginIdParam,
			@RequestParam(value = "usrNo", required = false) Long usrNoParam
	) {
		// 액세스 토큰 검증 흐름: 토큰 유효, 만료, 부적합 처리
		Map<String, String> result = new HashMap<>();
		String accessToken = extractAccessTokenFromHeader(request);
		String validationResult = tokenProvider.validateCheckToken(accessToken);

		if ("OK".equals(validationResult) && StringUtils.hasText(accessToken)) {
			result.put("result", "OK");
			result.put("resultMsg", "OK");
			result.put("accessToken", accessToken);
			return ResponseEntity.ok(result);
		}

		if (!"EXPIRED".equals(validationResult)) {
			clearRefreshTokenCookie(response);
			result.put("result", "INVALID_TOKEN");
			result.put("resultMsg", "유효하지 않은 Access Token 토큰입니다.");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
		}

		String refreshToken = extractRefreshTokenFromCookies(request);
		if (!StringUtils.hasText(refreshToken)) {
			clearRefreshTokenCookie(response);
			result.put("result", "INVALID_REFRESH_TOKEN");
			result.put("resultMsg", "유효하지 않은 리프레시 토큰입니다.");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
		}
		// refreshToken 해시를 조회합니다.
		String refreshTokenHash = userRefreshTokenService.buildRefreshTokenHash(refreshToken);
		Optional<UserRefreshTokenEntity> tokenInfo = userRefreshTokenService.findByHash(refreshTokenHash);
		if (tokenInfo.isEmpty()) {
			clearRefreshTokenCookie(response);
			result.put("result", "INVALID_REFRESH_TOKEN");
			result.put("resultMsg", "유효하지 않은 리프레시 토큰입니다.");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
		}
		UserRefreshTokenEntity tokenEntity = tokenInfo.get();
		// 토큰 만료 및 폐기 여부를 확인합니다.
		if (!"N".equalsIgnoreCase(tokenEntity.getIsRevoked()) || tokenEntity.getExpiresAt() == null
				|| tokenEntity.getExpiresAt().before(new Date())) {
			clearRefreshTokenCookie(response);
			result.put("result", "INVALID_REFRESH_TOKEN");
			result.put("resultMsg", "유효하지 않은 리프레시 토큰입니다.");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
		}
		// usrNo 파라미터가 있으면 토큰 소유자를 검증합니다.
		if (usrNoParam != null && !usrNoParam.equals(tokenEntity.getUsrNo())) {
			clearRefreshTokenCookie(response);
			result.put("result", "INVALID_REFRESH_TOKEN");
			result.put("resultMsg", "유효하지 않은 리프레시 토큰입니다.");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
		}
		// 사용자 정보를 조회합니다.
		Optional<UserBaseEntity> userOpt = userRepository.findById(tokenEntity.getUsrNo());
		if (userOpt.isEmpty()) {
			clearRefreshTokenCookie(response);
			result.put("result", "INVALID_REFRESH_TOKEN");
			result.put("resultMsg", "유효하지 않은 리프레시 토큰입니다.");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
		}
		UserBaseEntity user = userOpt.get();
		String loginId = StringUtils.hasText(loginIdParam) ? loginIdParam : user.getLoginId();
		if (!StringUtils.hasText(loginId)) {
			clearRefreshTokenCookie(response);
			result.put("result", "INVALID_REFRESH_TOKEN");
			result.put("resultMsg", "리프레시 토큰의 사용자 정보를 확인할 수 없습니다.");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
		}
		// 새로운 AccessToken과 RefreshToken을 발급합니다.
		String newAccessToken = tokenProvider.generateAccessTokenByLoginId(loginId);
		String rotatedRefreshToken = tokenProvider.generateRefreshToken(loginId);
		Date newRefreshExpiry = new Date(System.currentTimeMillis() + jwtRefreshTokenExpirationInMs);
		String rotatedHash = userRefreshTokenService.buildRefreshTokenHash(rotatedRefreshToken);
		tokenEntity.setRefreshTokenHash(rotatedHash);
		tokenEntity.setExpiresAt(newRefreshExpiry);
		tokenEntity.setLastUsedAt(new Date());
		userRefreshTokenService.saveToken(tokenEntity);
		setRefreshTokenCookie(response, rotatedRefreshToken);
		userRefreshTokenService.touchLastUsed(tokenEntity.getTokenId());

		result.put("result", "OK");
		result.put("resultMsg", "OK");
		result.put("accessToken", newAccessToken);
		return ResponseEntity.ok(result);
	}
	
	// 로그아웃 시 현재 세션의 리프레시 토큰만 폐기합니다.
	@PostMapping("/api/backoffice/logout")
	public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
		String refreshToken = extractRefreshTokenFromCookies(request);
		if (StringUtils.hasText(refreshToken)) {
			String refreshTokenHash = userRefreshTokenService.buildRefreshTokenHash(refreshToken);
			userRefreshTokenService.revokeByHash(refreshTokenHash);
		}

		clearRefreshTokenCookie(response);
		Map<String, String> result = new HashMap<>();
		result.put("result", "OK");
		result.put("resultMsg", "로그아웃 처리되었습니다.");
		return ResponseEntity.ok(result);
	}

	// 로그인 사용자 정보를 조회합니다.
	@GetMapping("/api/backoffice/user/info")
	public ResponseEntity<?> getLoginUserInfo(@RequestParam Long usrNo) {
		if (usrNo == null) {
			return ResponseEntity.badRequest().body(Map.of("message", "사용자 정보를 확인해주세요."));
		}
		Optional<UserInfoVO> info = userBaseService.getUserInfoByUsrNo(usrNo);
		if (info.isEmpty()) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(info.get());
	}

	// refreshToken을 httpOnly 쿠키로 클라이언트에 전송합니다.
	private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
		ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
				.httpOnly(true)
				.secure(jwtCookieSecure)
				.path("/")
				.maxAge(Duration.ofMillis(jwtRefreshTokenExpirationInMs))
				.sameSite("Strict")
				.build();
		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
	}

	// 쿠키를 비우기 위한 TTL 0 설정
	private void clearRefreshTokenCookie(HttpServletResponse response) {
		ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
				.httpOnly(true)
				.secure(jwtCookieSecure)
				.path("/")
				.maxAge(Duration.ZERO)
				.sameSite("Strict")
				.build();
		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
	}

	// Authorization 헤더에서 Bearer 토큰을 추출합니다.
	private String extractAccessTokenFromHeader(HttpServletRequest request) {
		String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
			return bearerToken.substring(7);
		}
		return null;
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

}
