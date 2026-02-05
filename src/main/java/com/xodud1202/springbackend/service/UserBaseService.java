package com.xodud1202.springbackend.service;

import com.xodud1202.springbackend.domain.admin.common.UserInfoVO;
import com.xodud1202.springbackend.entity.UserBaseEntity;
import com.xodud1202.springbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

// 사용자 기본 정보를 조회하는 서비스입니다.
@Service
@RequiredArgsConstructor
public class UserBaseService {

	private final UserRepository userRepository;

	/**
	 * 주어진 로그인 ID를 기반으로 사용자를 조회합니다.
	 * @param loginId 조회할 사용자의 로그인 ID
	 * @return 주어진 로그인 ID와 일치하는 {@code UserBaseEntity}를 포함하는 {@code Optional}.
	 *         사용자를 찾을 수 없는 경우 빈 {@code Optional}을 반환합니다.
	 */
	public Optional<UserBaseEntity> loadUserByLoginId(String loginId) {
		return userRepository.findByLoginId(loginId);
	}

	/**
	 * 사용자 번호로 사용자 정보를 조회합니다.
	 * @param usrNo 사용자 번호
	 * @return 사용자 정보 결과
	 */
	public Optional<UserInfoVO> getUserInfoByUsrNo(Long usrNo) {
		if (usrNo == null) {
			return Optional.empty();
		}
		return userRepository.findById(usrNo)
				.map(user -> {
					UserInfoVO info = new UserInfoVO();
					info.setUsrNo(user.getUsrNo());
					info.setLoginId(user.getLoginId());
					info.setUserNm(user.getUserNm());
					info.setUsrGradeCd(user.getUsrGradeCd());
					info.setUsrStatCd(user.getUsrStatCd());
					return info;
				});
	}
}
