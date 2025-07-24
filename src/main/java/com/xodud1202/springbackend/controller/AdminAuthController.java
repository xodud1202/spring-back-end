package com.xodud1202.springbackend.controller;

import ch.qos.logback.core.util.StringUtil;
import com.xodud1202.springbackend.domain.LoginRequest;
import com.xodud1202.springbackend.domain.RefreshTokenRequest;
import com.xodud1202.springbackend.domain.UserBase;
import com.xodud1202.springbackend.repository.UserRepository;
import com.xodud1202.springbackend.security.JwtTokenProvider;
import com.xodud1202.springbackend.service.CustomUserDetailService;
import com.xodud1202.springbackend.service.UserBaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

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
	
	@PostMapping("/backoffice/login")
	public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
		try {
			// 먼저 사용자가 존재하는지 확인
			Optional<UserBase> existingUser = userBaseService.loadUserByLoginId(loginRequest.getLoginId());
			if (existingUser.isEmpty()) {
				// 사용자가 존재하지 않는 경우
				Map<String, String> response = new HashMap<>();
				response.put("result", "NOT_FOUND_ID");
				response.put("resultMsg", "계정정보가 존재하지 않습니다.");
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
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
			UserBase user = existingUser.get();
			user.setJwtToken(accessToken);
			
			Map<String, Object> response = new HashMap<>();
			response.put("result", "OK");
			response.put("accessToken", accessToken);
			
			// 로그인 유지를 선택한 경우 Refresh Token 생성
			if (loginRequest.isRememberMe()) {
				String refreshToken = tokenProvider.generateRefreshToken(user.getUsername());
				Date refreshTokenExpiry = new Date(System.currentTimeMillis() + jwtRefreshTokenExpirationInMs); // refresh token 만료일정

				// 사용자 정보에 Refresh Token 저장 (메모리 객체에만 설정)
				user.setRefreshToken(refreshToken);
				user.setRefreshTokenExpiry(refreshTokenExpiry);

				// DB에는 필요한 필드만 업데이트
				userRepository.updateRefreshToken(
					user.getUsrNo(),
					user.getRefreshToken(),
					user.getRefreshTokenExpiry(),
					new Date(), // AccessDt (마지막 로그인 일시 현재시간 등록)
					user.getUsrNo(), // 현재 로그인한 사용자의 usrNo를 udtNo로 설정
					new Date()       // 현재 시간을 udtDt로 설정
				);

				response.put("refreshToken", refreshToken);
			} else {
				// USER_BASE의 REFRESH_TOKEN과 만료일시 초기화
				userRepository.clearRefreshToken(user.getUsrNo());
			}

			user.setPwd(null);       // 비밀번호 정보는 제외
			response.put("userInfo", user);
			
			return ResponseEntity.ok(response);
		} catch (BadCredentialsException e) {
			// 비밀번호가 일치하지 않는 경우
			Map<String, String> response = new HashMap<>();
			response.put("result", "NOT_CORRECT_PW");
			response.put("resultMsg", "비밀번호가 일치하지 않습니다.");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
		} catch (Exception e) {
			// 기타 예외 처리
			Map<String, String> response = new HashMap<>();
			response.put("result", "ERROR");
			response.put("resultMsg", e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}
	
	@GetMapping("/token/backoffice/access-token")
	public ResponseEntity<?> token(RefreshTokenRequest request) {
		// AccessToken 사용 가능 여부 확인
		Map<String, String> response = new HashMap<>();
		String accessToken = request.getAccessToken();
		String accessTokenValidResult = tokenProvider.validateCheckToken(accessToken);
		if (!"OK".equals(accessTokenValidResult) && !"EXPIRED".equals(accessTokenValidResult) ) {
			response.put("result", "INVALID_TOKEN");
			response.put("resultMsg", "유효하지 않은 Access Token 토큰입니다.");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
		} else if ("EXPIRED".equals(accessTokenValidResult) && !StringUtil.isNullOrEmpty(request.getRefreshToken())) {
			// accessToken이 만료되었으나 refreshToken이 있을 경우
			UserBase user = new UserBase();
			user.setRefreshToken(request.getRefreshToken());
			user.setLoginId(request.getLoginId());
			
			// refreshToken이 만료 전인지 확인.
			Optional<UserBase> chkUserInfo = userBaseService.findUserBaseByLoginIdAndRefreshTokenAndExpiredCheck(user);
			if (chkUserInfo.isEmpty()) {
				response.put("result", "EXPIRED_TOKEN");
				response.put("resultMsg", "유효하지 않은 Access Token 토큰입니다.");
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
			}
		}
		
		// accessTokenValidResult OK이거나, accessToken 은 만료되었으나 refreshToken은 만료가 아닐 경우
		// 신규 accessToken을 생성하여 30분짜리 token을 재생성한다.
		response.put("result", "OK");
		response.put("resultMsg", "OK");
		response.put("accessToken", tokenProvider.generateAccessTokenByLoginId(request.getLoginId()));
		
		return ResponseEntity.ok(response);
	}
	
	@PostMapping("/token/backoffice/refresh-token")
	public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest request) {
		// Refresh Token 검증
		String refreshToken = request.getRefreshToken();
		if (!tokenProvider.validateToken(refreshToken)) {
			Map<String, String> response = new HashMap<>();
			response.put("result", "INVALID_TOKEN");
			response.put("resultMsg", "유효하지 않은 리프레시 토큰입니다.");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
		}
		
		// 토큰에서 사용자아이디 추출
		String username = tokenProvider.getUsernameFromJWT(refreshToken);
		
		// 사용자 정보 조회
		Optional<UserBase> userOpt = userBaseService.loadUserByLoginId(username);
		if (userOpt.isEmpty()) {
			Map<String, String> response = new HashMap<>();
			response.put("result", "USER_NOT_FOUND");
			response.put("resultMsg", "사용자를 찾을 수 없습니다.");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
		}
		
		UserBase user = userOpt.get();
		
		// DB에 저장된 Refresh Token과 비교
		if (!refreshToken.equals(user.getRefreshToken())) {
			Map<String, String> response = new HashMap<>();
			response.put("result", "TOKEN_MISMATCH");
			response.put("resultMsg", "토큰이 일치하지 않습니다.");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
		}
		
		// Refresh Token 만료 시간 확인
		if (user.getRefreshTokenExpiry().before(new Date())) {
			Map<String, String> response = new HashMap<>();
			response.put("result", "TOKEN_EXPIRED");
			response.put("resultMsg", "리프레시 토큰이 만료되었습니다.");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
		}
		
		// 새 Access Token 생성
		Authentication authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
		String newAccessToken = tokenProvider.generateAccessToken(authentication);
		
		Map<String, Object> response = new HashMap<>();
		response.put("result", "OK");
		response.put("accessToken", newAccessToken);
		
		return ResponseEntity.ok(response);
	}
}