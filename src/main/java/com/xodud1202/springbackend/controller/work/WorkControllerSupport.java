package com.xodud1202.springbackend.controller.work;

import com.xodud1202.springbackend.common.work.WorkSessionPolicy;
import com.xodud1202.springbackend.entity.UserBaseEntity;
import com.xodud1202.springbackend.security.SignedLoginTokenService;
import com.xodud1202.springbackend.service.UserBaseService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;

// 업무관리 계열 컨트롤러의 로그인 세션 확인 기능을 제공합니다.
abstract class WorkControllerSupport {
	@Autowired
	private SignedLoginTokenService signedLoginTokenService;

	@Autowired
	private UserBaseService workSessionUserBaseService;

	// 요청에서 업무관리 사용자번호를 필수로 확인하고 없으면 인증 오류를 발생시킵니다.
	protected Long resolveRequiredWorkUserNo(HttpServletRequest request) {
		Long sessionWorkUserNo = resolveWorkUserNoFromSession(request);
		if (sessionWorkUserNo != null) {
			return sessionWorkUserNo;
		}

		Long cookieWorkUserNo = WorkSessionPolicy.resolveWorkUserNoFromRequest(request, signedLoginTokenService);
		if (cookieWorkUserNo == null) {
			throw new SecurityException("로그인이 필요합니다.");
		}

		UserBaseEntity user = workSessionUserBaseService.getUserEntityByUsrNo(cookieWorkUserNo).orElse(null);
		if (user == null || user.getUsrNo() == null) {
			throw new SecurityException("로그인이 필요합니다.");
		}

		WorkSessionPolicy.applyAuthenticatedSession(request.getSession(true), user.getUsrNo());
		return user.getUsrNo();
	}

	// 기존 HTTP 세션에 저장된 업무관리 사용자번호를 읽습니다.
	private Long resolveWorkUserNoFromSession(HttpServletRequest request) {
		HttpSession session = request == null ? null : request.getSession(false);
		if (session == null) {
			return null;
		}
		return WorkSessionPolicy.resolveWorkUserNo(session.getAttribute(WorkSessionPolicy.SESSION_ATTR_WORK_USER_NO));
	}
}
