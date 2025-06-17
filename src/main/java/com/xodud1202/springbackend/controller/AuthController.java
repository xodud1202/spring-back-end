package com.xodud1202.springbackend.controller;

import com.xodud1202.springbackend.domain.JwtAuthenticationResponse;
import com.xodud1202.springbackend.domain.UserBase;
import com.xodud1202.springbackend.security.JwtTokenProvider;
import com.xodud1202.springbackend.service.CustomUserDetailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequiredArgsConstructor
public class AuthController {

	private final AuthenticationManager authenticationManager;
	private final JwtTokenProvider tokenProvider;
	private final CustomUserDetailService userDetailService;

	@PostMapping("/backoffice/login")
	public ResponseEntity<?> authenticateUser(@RequestBody UserBase userBase) {
		try {
			// 먼저 사용자가 존재하는지 확인
			Optional<UserBase> existingUser = userDetailService.loadUserByLoginId(userBase.getUsername());
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
							userBase.getUsername(),
							userBase.getPassword()
					)
			);

			SecurityContextHolder.getContext().setAuthentication(authentication);
			String jwt = tokenProvider.generateToken(authentication);

			UserBase user = existingUser.get(); // existingUser에서 UserBase 객체 가져오기
			user.setPwd(null);       // 비밀번호 정보는 제외
			user.setJwtToken(jwt);

			Map<String, Object> response = new HashMap<>();
			response.put("result", "OK");
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
}