package com.xodud1202.springbackend.controller;

import com.xodud1202.springbackend.domain.LoginRequest;
import com.xodud1202.springbackend.domain.RefreshTokenRequest;
import com.xodud1202.springbackend.entity.UserBaseEntity;
import com.xodud1202.springbackend.repository.UserRepository;
import com.xodud1202.springbackend.security.JwtTokenProvider;
import com.xodud1202.springbackend.service.CustomUserDetailService;
import com.xodud1202.springbackend.service.UserBaseService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@RestController
@RequiredArgsConstructor
public class AdminAuthController {
	
	private final AuthenticationManager authenticationManager;
	private final JwtTokenProvider tokenProvider;
	private final CustomUserDetailService userDetailService;
	private final UserBaseService userBaseService;
	private final UserRepository userRepository;

	@Value("${jwt.refresh-token-expiration}")
    private long jwtRefreshTokenExpirationInMs;
	
	@Value("${jwt.cookie-secure:false}")
	private boolean jwtCookieSecure;
	
	@PostMapping("/api/backoffice/login")
	public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest, HttpServletResponse response) {
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
			user.setJwtToken(accessToken);
			
			// 로그인 결과를 담을 응답 맵
			Map<String, Object> payload = new HashMap<>();
			payload.put("result", "OK");
			payload.put("accessToken", accessToken);
			
			// 로그인 유지를 선택한 경우 Refresh Token 생성
			if (loginRequest.isRememberMe()) {
				String refreshToken = tokenProvider.generateRefreshToken(user.getUsername());
				Date refreshTokenExpiry = new Date(System.currentTimeMillis() + jwtRefreshTokenExpirationInMs);

				user.setRefreshToken(refreshToken);
				user.setRefreshTokenExpiry(refreshTokenExpiry);

				userRepository.updateRefreshToken(
					user.getUsrNo(),
					user.getRefreshToken(),
					user.getRefreshTokenExpiry(),
					new Date(),
					user.getUsrNo(),
					new Date()
				);

				setRefreshTokenCookie(response, refreshToken);
			} else {
				userRepository.clearRefreshToken(user.getUsrNo());
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
	
	@GetMapping("/api/token/backoffice/access-token")
	public ResponseEntity<?> token(
			HttpServletRequest request,
			HttpServletResponse response,
			@RequestParam(value = "loginId", required = false) String loginIdParam
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

		String loginId = StringUtils.hasText(loginIdParam) ? loginIdParam : resolveLoginIdFromToken(refreshToken);
		if (!StringUtils.hasText(loginId)) {
			clearRefreshTokenCookie(response);
			result.put("result", "INVALID_REFRESH_TOKEN");
			result.put("resultMsg", "리프레시 토큰의 사용자 정보를 확인할 수 없습니다.");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
		}

		UserBaseEntity candidate = new UserBaseEntity();
		candidate.setLoginId(loginId);
		candidate.setRefreshToken(refreshToken);

		Optional<UserBaseEntity> chkUserInfo = userBaseService.findUserBaseByLoginIdAndRefreshTokenAndExpiredCheck(candidate);
		if (chkUserInfo.isEmpty()) {
			clearRefreshTokenCookie(response);
			result.put("result", "EXPIRED_TOKEN");
			result.put("resultMsg", "유효하지 않은 Access Token 토큰입니다.");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
		}

		UserBaseEntity user = chkUserInfo.get();
		String newAccessToken = tokenProvider.generateAccessTokenByLoginId(loginId);

		String rotatedRefreshToken = tokenProvider.generateRefreshToken(loginId);
		Date newRefreshExpiry = new Date(System.currentTimeMillis() + jwtRefreshTokenExpirationInMs);
		user.setRefreshToken(rotatedRefreshToken);
		user.setRefreshTokenExpiry(newRefreshExpiry);
		userRepository.updateRefreshToken(
			user.getUsrNo(),
			user.getRefreshToken(),
			user.getRefreshTokenExpiry(),
			new Date(),
			user.getUsrNo(),
			new Date()
		);
		setRefreshTokenCookie(response, rotatedRefreshToken);

		result.put("result", "OK");
		result.put("resultMsg", "OK");
		result.put("accessToken", newAccessToken);
		return ResponseEntity.ok(result);
	}
	
	@PostMapping("/token/backoffice/refresh-token")
	public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest request, HttpServletResponse response) {
		// refreshToken을 통해 accessToken을 새로 발급
		// Refresh Token 검증
		String refreshToken = request.getRefreshToken();
		if (!tokenProvider.validateToken(refreshToken)) {
			Map<String, String> payload = new HashMap<>();
			payload.put("result", "INVALID_TOKEN");
			payload.put("resultMsg", "유효하지 않은 리프레시 토큰입니다.");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(payload);
		}
		
		// 토큰에서 사용자아이디 추출
		String username = tokenProvider.getUsernameFromJWT(refreshToken);
		
		// 사용자 정보 조회
		Optional<UserBaseEntity> userOpt = userBaseService.loadUserByLoginId(username);
		if (userOpt.isEmpty()) {
			Map<String, String> payload = new HashMap<>();
			payload.put("result", "USER_NOT_FOUND");
			payload.put("resultMsg", "사용자를 찾을 수 없습니다.");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(payload);
		}
		
		UserBaseEntity user = userOpt.get();
		
		// DB에 저장된 Refresh Token과 비교
		if (!refreshToken.equals(user.getRefreshToken())) {
			Map<String, String> payload = new HashMap<>();
			payload.put("result", "TOKEN_MISMATCH");
			payload.put("resultMsg", "토큰이 일치하지 않습니다.");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(payload);
		}
		
		// Refresh Token 만료 시간 확인
		if (user.getRefreshTokenExpiry().before(new Date())) {
			Map<String, String> payload = new HashMap<>();
			payload.put("result", "TOKEN_EXPIRED");
			payload.put("resultMsg", "리프레시 토큰이 만료되었습니다.");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(payload);
		}
		
		// 새 Access Token 생성
		Authentication authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
		String newAccessToken = tokenProvider.generateAccessToken(authentication);
		
		String rotatedRefreshToken = tokenProvider.generateRefreshToken(username);
		Date newRefreshExpiry = new Date(System.currentTimeMillis() + jwtRefreshTokenExpirationInMs);
		user.setRefreshToken(rotatedRefreshToken);
		user.setRefreshTokenExpiry(newRefreshExpiry);
		userRepository.updateRefreshToken(
			user.getUsrNo(),
			user.getRefreshToken(),
			user.getRefreshTokenExpiry(),
			new Date(),
			user.getUsrNo(),
			new Date()
		);
		setRefreshTokenCookie(response, rotatedRefreshToken);
		
		Map<String, Object> result = new HashMap<>();
		result.put("result", "OK");
		result.put("accessToken", newAccessToken);
		
		return ResponseEntity.ok(result);
	}

	@PostMapping("/api/backoffice/logout")
	public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
		String refreshToken = extractRefreshTokenFromCookies(request);
		if (StringUtils.hasText(refreshToken)) {
			String loginId = resolveLoginIdFromToken(refreshToken);
			if (StringUtils.hasText(loginId)) {
				userBaseService.loadUserByLoginId(loginId)
						.ifPresent(user -> userRepository.clearRefreshToken(user.getUsrNo()));
			}
		}

		clearRefreshTokenCookie(response);
		Map<String, String> result = new HashMap<>();
		result.put("result", "OK");
		result.put("resultMsg", "로그아웃 처리되었습니다.");
		return ResponseEntity.ok(result);
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

	// 토큰에서 loginId를 파싱합니다.
	private String resolveLoginIdFromToken(String refreshToken) {
		try {
			return tokenProvider.getUsernameFromJWT(refreshToken);
		} catch (Exception ex) {
			log.warn("refresh token parsing failed", ex);
			return null;
		}
	}
}
