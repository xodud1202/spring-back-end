package com.xodud1202.springbackend.service;

import com.xodud1202.springbackend.domain.admin.user.UserManagePO;
import com.xodud1202.springbackend.domain.admin.user.UserManageVO;
import com.xodud1202.springbackend.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

// 관리자 사용자 관리 기능을 처리하는 서비스입니다.
@Service
@RequiredArgsConstructor
public class AdminUserService {
	private final UserMapper userMapper;
	private final PasswordEncoder passwordEncoder;

	// 사용자 아이디 최소 길이 제한입니다.
	private static final int LOGIN_ID_MIN_LENGTH = 5;
	// 비밀번호 최소 길이 제한입니다.
	private static final int PASSWORD_MIN_LENGTH = 6;
	// 이름 최소 길이 제한입니다.
	private static final int USER_NAME_MIN_LENGTH = 2;

	// 관리자 사용자 목록을 조회합니다.
	public List<UserManageVO> getAdminUserList(String searchGb, String searchValue, String usrStatCd, String usrGradeCd) {
		String resolvedSearchGb = trimToNull(searchGb);
		String resolvedSearchValue = trimToNull(searchValue);
		String resolvedUsrStatCd = trimToNull(usrStatCd);
		String resolvedUsrGradeCd = trimToNull(usrGradeCd);
		return userMapper.getAdminUserList(resolvedSearchGb, resolvedSearchValue, resolvedUsrStatCd, resolvedUsrGradeCd);
	}

	// 관리자 사용자 등록 요청을 검증합니다.
	public String validateCreateAdminUser(UserManagePO param) {
		// 요청 데이터 필수값을 검증합니다.
		if (param == null) {
			return "요청 데이터를 확인해주세요.";
		}
		if (isBlank(param.getLoginId())) {
			return "ID를 입력해주세요.";
		}
		if (isBlank(param.getPwd())) {
			return "비밀번호를 입력해주세요.";
		}
		if (isBlank(param.getUserNm())) {
			return "이름을 입력해주세요.";
		}
		if (isBlank(param.getUsrStatCd())) {
			return "상태를 선택해주세요.";
		}
		if (isBlank(param.getUsrGradeCd())) {
			return "등급을 선택해주세요.";
		}
		if (isBlank(param.getHPhoneNo())) {
			return "휴대폰번호를 입력해주세요.";
		}
		if (isBlank(param.getEmail())) {
			return "이메일을 입력해주세요.";
		}
		if (param.getRegNo() == null) {
			return "등록자 정보를 확인해주세요.";
		}
		// 입력 형식을 검증합니다.
		String formatMessage = validateUserFormat(param);
		if (formatMessage != null) {
			return formatMessage;
		}
		// 로그인 아이디 중복 여부를 검증합니다.
		if (userMapper.countAdminUserByLoginId(param.getLoginId().trim()) > 0) {
			return "이미 등록된 ID입니다.";
		}
		return null;
	}

	// 관리자 사용자 수정 요청을 검증합니다.
	public String validateUpdateAdminUser(UserManagePO param) {
		// 요청 데이터 필수값을 검증합니다.
		if (param == null) {
			return "요청 데이터를 확인해주세요.";
		}
		if (param.getUsrNo() == null) {
			return "수정 대상을 확인해주세요.";
		}
		if (isBlank(param.getPwd())) {
			return "비밀번호를 입력해주세요.";
		}
		if (isBlank(param.getUserNm())) {
			return "이름을 입력해주세요.";
		}
		if (isBlank(param.getUsrStatCd())) {
			return "상태를 선택해주세요.";
		}
		if (isBlank(param.getUsrGradeCd())) {
			return "등급을 선택해주세요.";
		}
		if (isBlank(param.getHPhoneNo())) {
			return "휴대폰번호를 입력해주세요.";
		}
		if (isBlank(param.getEmail())) {
			return "이메일을 입력해주세요.";
		}
		if (param.getUdtNo() == null) {
			return "수정자 정보를 확인해주세요.";
		}
		// 수정 대상 존재 여부를 검증합니다.
		UserManageVO current = userMapper.getAdminUserDetail(param.getUsrNo());
		if (current == null) {
			return "수정 대상을 확인해주세요.";
		}
		// 입력 형식을 검증합니다.
		String formatMessage = validateUserFormat(param);
		if (formatMessage != null) {
			return formatMessage;
		}
		return null;
	}

	// 관리자 사용자를 등록합니다.
	public int createAdminUser(UserManagePO param) {
		// 등록 입력값 공백을 정리합니다.
		normalizeUserParam(param);
		// 수정자 정보가 없으면 등록자로 설정합니다.
		if (param.getUdtNo() == null) {
			param.setUdtNo(param.getRegNo());
		}
		// 비밀번호를 로그인 정책과 동일하게 BCrypt로 암호화합니다.
		param.setPwd(passwordEncoder.encode(param.getPwd()));
		return userMapper.insertAdminUser(param);
	}

	// 관리자 사용자를 수정합니다.
	public int updateAdminUser(UserManagePO param) {
		// 수정 입력값 공백을 정리합니다.
		normalizeUserParam(param);
		// 비밀번호를 로그인 정책과 동일하게 BCrypt로 암호화합니다.
		param.setPwd(passwordEncoder.encode(param.getPwd()));
		return userMapper.updateAdminUser(param);
	}

	// 사용자 입력 형식을 검증합니다.
	private String validateUserFormat(UserManagePO param) {
		String loginId = trimToNull(param.getLoginId());
		String pwd = trimToNull(param.getPwd());
		String userNm = trimToNull(param.getUserNm());
		String hPhoneNo = trimToNull(param.getHPhoneNo());
		String email = trimToNull(param.getEmail());

		// 로그인 아이디 길이를 검증합니다.
		if (loginId != null && loginId.length() < LOGIN_ID_MIN_LENGTH) {
			return "ID는 최소 5자 이상 입력해주세요.";
		}
		// 비밀번호 길이를 검증합니다.
		if (pwd != null && pwd.length() < PASSWORD_MIN_LENGTH) {
			return "비밀번호는 최소 6자 이상 입력해주세요.";
		}
		// 이름 길이를 검증합니다.
		if (userNm != null && userNm.length() < USER_NAME_MIN_LENGTH) {
			return "이름은 최소 2자 이상 입력해주세요.";
		}
		// 휴대폰번호 형식을 검증합니다.
		if (hPhoneNo != null && !hPhoneNo.matches("^01\\d-\\d{3,4}-\\d{4}$")) {
			return "휴대폰번호 형식을 확인해주세요.";
		}
		// 이메일 형식을 검증합니다.
		if (email != null && !email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
			return "이메일 형식을 확인해주세요.";
		}
		return null;
	}

	// 사용자 등록/수정 입력값의 공백을 정리합니다.
	private void normalizeUserParam(UserManagePO param) {
		param.setLoginId(trimToNull(param.getLoginId()));
		param.setPwd(trimToNull(param.getPwd()));
		param.setUserNm(trimToNull(param.getUserNm()));
		param.setUsrGradeCd(trimToNull(param.getUsrGradeCd()));
		param.setUsrStatCd(trimToNull(param.getUsrStatCd()));
		param.setHPhoneNo(trimToNull(param.getHPhoneNo()));
		param.setEmail(trimToNull(param.getEmail()));
	}

	// 문자열 공백 여부를 확인합니다.
	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

	// 문자열 양끝 공백을 제거하고 빈 값은 null로 변환합니다.
	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}

