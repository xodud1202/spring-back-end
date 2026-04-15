package com.xodud1202.springbackend.service;

import static com.xodud1202.springbackend.common.util.CommonTextUtils.*;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.xodud1202.springbackend.common.mybatis.GeneratedLongKey;
import com.xodud1202.springbackend.config.properties.GoogleIdentityProperties;
import com.xodud1202.springbackend.domain.snippet.SnippetGoogleLoginRequest;
import com.xodud1202.springbackend.domain.snippet.SnippetGoogleLoginResponse;
import com.xodud1202.springbackend.domain.snippet.SnippetUserCreatePO;
import com.xodud1202.springbackend.domain.snippet.SnippetUserSessionVO;
import com.xodud1202.springbackend.mapper.SnippetMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Service
@RequiredArgsConstructor
// 스니펫 구글 로그인과 사용자 세션 복구를 처리합니다.
public class SnippetAuthService {
	private static final int GOOGLE_SUB_MAX_LENGTH = 100;
	private static final int EMAIL_MAX_LENGTH = 191;
	private static final int USER_NAME_MAX_LENGTH = 100;
	private static final int PROFILE_IMG_URL_MAX_LENGTH = 500;

	private final SnippetMapper snippetMapper;
	private final GoogleIdentityProperties googleIdentityProperties;

	@Transactional
	// 구글 ID 토큰을 검증하고 스니펫 사용자를 자동 가입 또는 로그인 처리합니다.
	public SnippetGoogleLoginResponse loginWithGoogle(SnippetGoogleLoginRequest request) {
		// 구글 ID 토큰을 검증하고 필수 사용자 정보를 정규화합니다.
		VerifiedSnippetGoogleUser verifiedUser = verifyGoogleCredential(request);

		// 기존 사용자 여부를 먼저 확인합니다.
		SnippetUserSessionVO existingUser = snippetMapper.getSnippetUserByGoogleSub(verifiedUser.googleSub());
		boolean isFirstLogin = existingUser == null || existingUser.snippetUserNo() == null;

		if (isFirstLogin) {
			// 첫 로그인 사용자는 자동 가입 후 감사 번호를 자신의 사용자 번호로 맞춥니다.
			registerSnippetUser(verifiedUser);
		} else {
			// 기존 사용자는 프로필과 마지막 로그인 시각을 갱신합니다.
			snippetMapper.updateSnippetUserLoginInfo(
				existingUser.snippetUserNo(),
				verifiedUser.email(),
				verifiedUser.userNm(),
				verifiedUser.profileImgUrl(),
				existingUser.snippetUserNo()
			);
		}

		// 최종 사용자 정보를 다시 읽어 로그인 응답을 구성합니다.
		SnippetUserSessionVO loginUser = snippetMapper.getSnippetUserByGoogleSub(verifiedUser.googleSub());
		if (loginUser == null || loginUser.snippetUserNo() == null) {
			throw new IllegalStateException("스니펫 사용자 로그인 처리에 실패했습니다.");
		}

		return SnippetGoogleLoginResponse.authenticated(
			isFirstLogin ? "Y" : "N",
			loginUser.snippetUserNo(),
			loginUser.userNm(),
			loginUser.email(),
			loginUser.profileImgUrl()
		);
	}

	// 세션 복구에 사용할 활성 사용자 정보를 조회합니다.
	public SnippetUserSessionVO findActiveSnippetUser(Long snippetUserNo) {
		if (snippetUserNo == null || snippetUserNo < 1L) {
			return null;
		}
		return snippetMapper.getSnippetUserByUserNo(snippetUserNo);
	}

	// 사용자 번호 기준으로 활성 사용자 정보를 강제 조회합니다.
	public SnippetUserSessionVO getRequiredSnippetUser(Long snippetUserNo) {
		SnippetUserSessionVO snippetUser = findActiveSnippetUser(snippetUserNo);
		if (snippetUser == null || snippetUser.snippetUserNo() == null) {
			throw new SecurityException("로그인이 필요합니다.");
		}
		return snippetUser;
	}

	// 신규 스니펫 사용자를 등록합니다.
	private void registerSnippetUser(VerifiedSnippetGoogleUser verifiedUser) {
		// 신규 사용자 등록 명령을 구성합니다.
		SnippetUserCreatePO createCommand = new SnippetUserCreatePO(
			verifiedUser.googleSub(),
			verifiedUser.email(),
			verifiedUser.userNm(),
			verifiedUser.profileImgUrl(),
			null,
			null
		);

		// 신규 사용자 등록과 생성키 수집을 수행합니다.
		GeneratedLongKey generatedKey = new GeneratedLongKey();
		snippetMapper.insertSnippetUser(createCommand, generatedKey);
		Long createdUserNo = generatedKey.getValue();
		if (createdUserNo == null) {
			throw new IllegalStateException("스니펫 사용자 등록에 실패했습니다.");
		}

		// 감사 번호를 생성된 사용자 번호 기준으로 갱신합니다.
		snippetMapper.updateSnippetUserAuditNo(createdUserNo, createdUserNo);
	}

	// 구글 ID 토큰을 검증하고 내부 사용자 정보로 변환합니다.
	private VerifiedSnippetGoogleUser verifyGoogleCredential(SnippetGoogleLoginRequest request) {
		try {
			// 요청 또는 서버 설정 기준으로 검증용 클라이언트 ID를 결정합니다.
			String resolvedClientId = resolveGoogleClientId(request.clientId());

			// 구글 검증기를 구성하고 서명/청중을 검증합니다.
			GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
				.setAudience(Collections.singletonList(resolvedClientId))
				.build();
			GoogleIdToken googleIdToken = verifier.verify(request.credential());
			if (googleIdToken == null) {
				throw new IllegalArgumentException("구글 로그인 검증에 실패했습니다.");
			}

			// 검증된 payload에서 화면과 저장에 필요한 항목을 추출합니다.
			GoogleIdToken.Payload payload = googleIdToken.getPayload();
			String googleSub = normalizeRequiredText(payload.getSubject(), "구글 사용자 식별값을 확인해주세요.");
			String email = normalizeRequiredText(payload.getEmail(), "구글 이메일 정보를 확인해주세요.");
			String userNm = resolveUserName(payload);
			String profileImgUrl = trimToNull(stringValue(payload.get("picture")));

			// 저장 컬럼 길이에 맞게 문자열 길이를 검증합니다.
			validateMaxLength(googleSub, GOOGLE_SUB_MAX_LENGTH, "구글 사용자 식별값 길이가 올바르지 않습니다.");
			validateMaxLength(email, EMAIL_MAX_LENGTH, "이메일 길이가 올바르지 않습니다.");
			validateMaxLength(userNm, USER_NAME_MAX_LENGTH, "사용자명 길이가 올바르지 않습니다.");
			if (profileImgUrl != null) {
				validateMaxLength(profileImgUrl, PROFILE_IMG_URL_MAX_LENGTH, "프로필 이미지 주소 길이가 올바르지 않습니다.");
			}

			return new VerifiedSnippetGoogleUser(googleSub, email, userNm, profileImgUrl);
		} catch (IllegalArgumentException exception) {
			throw exception;
		} catch (GeneralSecurityException | IOException exception) {
			throw new IllegalStateException("구글 로그인 검증 중 오류가 발생했습니다.", exception);
		}
	}

	// 구글 클라이언트 ID를 서버 설정값 우선으로 결정합니다.
	private String resolveGoogleClientId(String requestClientId) {
		String configuredClientId = trimToNull(googleIdentityProperties.clientId());
		if (configuredClientId != null) {
			return configuredClientId;
		}
		return normalizeRequiredText(requestClientId, "구글 클라이언트 ID 값을 확인해주세요.");
	}

	// 구글 payload에서 표시용 사용자명을 결정합니다.
	private String resolveUserName(GoogleIdToken.Payload payload) {
		String resolvedName = trimToNull(stringValue(payload.get("name")));
		if (resolvedName != null) {
			return resolvedName;
		}
		String resolvedEmail = trimToNull(payload.getEmail());
		if (resolvedEmail != null) {
			return resolvedEmail;
		}
		return normalizeRequiredText(payload.getSubject(), "구글 사용자명을 확인해주세요.");
	}

	// 객체 값을 문자열로 변환합니다.
	private String stringValue(Object value) {
		return value == null ? null : String.valueOf(value);
	}

	// 필수 문자열을 trim 처리하고 비어 있으면 예외를 반환합니다.
	private String normalizeRequiredText(String value, String errorMessage) {
		String normalizedValue = trimToNull(value);
		if (normalizedValue == null) {
			throw new IllegalArgumentException(errorMessage);
		}
		return normalizedValue;
	}

	// 문자열 길이 제한을 검증합니다.
	private void validateMaxLength(String value, int maxLength, String errorMessage) {
		if (value != null && value.length() > maxLength) {
			throw new IllegalArgumentException(errorMessage);
		}
	}

	// 검증이 끝난 구글 사용자 정보를 전달합니다.
	private record VerifiedSnippetGoogleUser(
		String googleSub,
		String email,
		String userNm,
		String profileImgUrl
	) {
	}
}
