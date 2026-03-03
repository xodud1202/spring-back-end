package com.xodud1202.springbackend.service;

import com.xodud1202.springbackend.domain.admin.common.AdminMenuLnb;
import com.xodud1202.springbackend.domain.admin.common.CommonCodeManagePO;
import com.xodud1202.springbackend.domain.admin.common.MenuBase;
import com.xodud1202.springbackend.domain.admin.common.MenuManageSavePO;
import com.xodud1202.springbackend.domain.admin.common.MenuManageVO;
import com.xodud1202.springbackend.domain.common.CommonCodeVO;
import com.xodud1202.springbackend.mapper.CommonMapper;
import com.xodud1202.springbackend.repository.MenuBaseRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminCommonService {
	
	private final MenuBaseRepository menuBaseRepository;
	private final CommonMapper commonMapper;
	
	/**
	 * MenuBase 객체를 AdminMenuLnb 객체로 변환합니다.
	 * @param menuBase 변환할 MenuBase 객체
	 * @return 변환된 AdminMenuLnb 객체
	 */
	private AdminMenuLnb menuBaseConvertToLnb(MenuBase menuBase) {
		AdminMenuLnb lnb = new AdminMenuLnb();
		lnb.setMenuNm(menuBase.getMenuNm());
		lnb.setMenuUrl(StringUtils.isBlank(menuBase.getMenuUrl()) ? "/" : menuBase.getMenuUrl());
		return lnb;
	}
	
	/**
	 * 메뉴 계층 구조를 생성하여 LNB 메뉴 트리를 반환합니다.
	 * @param menuList 메뉴 리스트, MenuBase 객체로 구성된 입력 데이터
	 * @return AdminMenuLnb 객체로 구성된 LNB 메뉴 트리 리스트
	 */
	private List<AdminMenuLnb> buildLnbMenuTree(List<MenuBase> menuList) {
		// 하위 메뉴 리스트 Map으로 변환 생성
		Map<Integer, List<MenuBase>> parentMap = menuList.stream()
				.filter(m -> m.getUpMenuNo() != 0)
				.collect(Collectors.groupingBy(MenuBase::getUpMenuNo));
		
		return menuList.stream()
				.filter(m -> m.getMenuLevel() == 1)
				.sorted(Comparator.comparingInt(MenuBase::getSortSeq))
				.map(m1 -> {
					// first depth menu 생성
					AdminMenuLnb lnb1 = menuBaseConvertToLnb(m1);
					
					// second depth menu 생성 (m1의 메뉴번호가 key인 list 조회)
					List<AdminMenuLnb> secondLevel = parentMap.getOrDefault(m1.getMenuNo(), Collections.emptyList()).stream()
							.sorted(Comparator.comparingInt(MenuBase::getSortSeq))
							.map(m2 -> {
								// second depth menu 생성
								AdminMenuLnb lnb2 = menuBaseConvertToLnb(m2);
								
								// third depth menu 생성 (m2의 메뉴번호가 key인 list 조회)
								List<AdminMenuLnb> thirdLevel = parentMap.getOrDefault(m2.getMenuNo(), Collections.emptyList()).stream()
										.sorted(Comparator.comparingInt(MenuBase::getSortSeq))
										.map(this::menuBaseConvertToLnb)
										.collect(Collectors.toList());
								lnb2.setSubMenus(thirdLevel);
								
								return lnb2;
							})
							.collect(Collectors.toList());
					
					lnb1.setSubMenus(secondLevel);
					return lnb1;
				})
				.collect(Collectors.toList());
	}
	
	/**
	 * 활성화된(AdminMenuLnb) LNB 메뉴 리스트를 반환합니다.
	 * 캐싱을 통해 성능을 최적화합니다.
	 * @return 활성 상태의 메뉴 데이터를 기반으로 계층형 LNB 메뉴 트리 형태로 구성된 AdminMenuLnb 리스트
	 */
	@Cacheable("menuListCache")
	public List<AdminMenuLnb> getAdminMenuLnbInfo() {
		// 사용 메뉴 전체 조회
		List<MenuBase> menuList = menuBaseRepository.findByUseYn("Y");
		
		// lnb 메뉴 리스트 tree 생성
		return buildLnbMenuTree(menuList);
	}

	// 메뉴 관리용 트리 목록을 반환합니다.
	public List<MenuManageVO> getAdminMenuManageList() {
		List<MenuBase> menuList = menuBaseRepository.findAll();
		Map<Integer, Integer> childCountMap = menuList.stream()
				.filter(item -> item != null && item.getUpMenuNo() != 0)
				.collect(Collectors.groupingBy(MenuBase::getUpMenuNo, Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));
		return menuList.stream()
				.filter(Objects::nonNull)
				.sorted(Comparator.comparingInt(MenuBase::getMenuLevel).thenComparingInt(MenuBase::getUpMenuNo).thenComparingInt(MenuBase::getSortSeq))
				.map(item -> toMenuManageVO(item, childCountMap.getOrDefault(item.getMenuNo(), 0)))
				.collect(Collectors.toList());
	}

	// 메뉴 관리 상세 정보를 조회합니다.
	public MenuManageVO getAdminMenuManageDetail(int menuNo) {
		MenuBase menu = menuBaseRepository.findByMenuNo(menuNo).orElse(null);
		if (menu == null) {
			return null;
		}
		int childCount = menuBaseRepository.countByUpMenuNo(menuNo);
		return toMenuManageVO(menu, childCount);
	}

	// 메뉴 URL 중복 여부와 규칙을 포함해 등록 요청을 검증합니다.
	public String validateAdminMenuCreate(MenuManageSavePO param) {
		normalizeMenuManageSaveParam(param);
		if (param == null) {
			return "요청 데이터가 없습니다.";
		}
		if (isBlank(param.getMenuNm())) {
			return "메뉴명을 입력해주세요.";
		}
		if (param.getRegNo() == null) {
			return "등록자 정보를 확인해주세요.";
		}
		if (!isBlank(param.getUseYn()) && !"Y".equals(param.getUseYn()) && !"N".equals(param.getUseYn())) {
			return "사용여부를 확인해주세요.";
		}
		int upMenuNo = param.getUpMenuNo();
		if (upMenuNo < 0) {
			return "상위 메뉴를 확인해주세요.";
		}
		MenuBase parent = findMenu(upMenuNo);
		if (upMenuNo != 0 && parent == null) {
			return "상위 메뉴를 확인해주세요.";
		}
		// 상위 메뉴 규칙을 적용합니다.
		if (upMenuNo != 0) {
			if (parent != null && parent.getMenuLevel() >= MENU_LEVEL_MAX) {
				return "3레벨 메뉴에는 하위 메뉴를 추가할 수 없습니다.";
			}
			if (StringUtils.isNotBlank(parent.getMenuUrl())) {
				return "메뉴 URL이 등록된 항목에는 하위 메뉴를 추가할 수 없습니다.";
			}
		}
		if (param.getMenuNm().length() > MENU_NAME_MAX_LENGTH) {
			return "메뉴명은 50자 이내로 입력해주세요.";
		}
		String resolvedMenuUrl = trimToNull(param.getMenuUrl());
		if (resolvedMenuUrl != null && resolvedMenuUrl.length() > MENU_URL_MAX_LENGTH) {
			return "메뉴 URL은 255자 이내로 입력해주세요.";
		}
		if (resolvedMenuUrl != null && menuBaseRepository.countByMenuUrl(resolvedMenuUrl) > 0) {
			return "이미 등록된 메뉴 URL입니다.";
		}
		return null;
	}

	// 메뉴 URL 중복 여부를 포함해 수정 요청을 검증합니다.
	public String validateAdminMenuUpdate(MenuManageSavePO param) {
		normalizeMenuManageSaveParam(param);
		if (param == null) {
			return "요청 데이터가 없습니다.";
		}
		if (param.getMenuNo() <= 0) {
			return "메뉴 번호를 확인해주세요.";
		}
		if (isBlank(param.getMenuNm())) {
			return "메뉴명을 입력해주세요.";
		}
		if (param.getUdtNo() == null) {
			return "수정자 정보를 확인해주세요.";
		}
		MenuBase current = menuBaseRepository.findByMenuNo(param.getMenuNo()).orElse(null);
		if (current == null) {
			return "메뉴 정보를 확인해주세요.";
		}
		if (!isBlank(param.getUseYn()) && !"Y".equals(param.getUseYn()) && !"N".equals(param.getUseYn())) {
			return "사용여부를 확인해주세요.";
		}
		if (param.getMenuNm().length() > MENU_NAME_MAX_LENGTH) {
			return "메뉴명은 50자 이내로 입력해주세요.";
		}
		int childCount = menuBaseRepository.countByUpMenuNo(param.getMenuNo());
		String resolvedMenuUrl = trimToNull(param.getMenuUrl());
		if (resolvedMenuUrl != null && childCount > 0) {
			return "하위 메뉴가 있는 메뉴는 메뉴 URL을 등록할 수 없습니다.";
		}
		if (resolvedMenuUrl != null && menuBaseRepository.countByMenuUrlAndMenuNoNot(resolvedMenuUrl, param.getMenuNo()) > 0) {
			return "이미 등록된 메뉴 URL입니다.";
		}
		if (resolvedMenuUrl != null && resolvedMenuUrl.length() > MENU_URL_MAX_LENGTH) {
			return "메뉴 URL은 255자 이내로 입력해주세요.";
		}
		return null;
	}

	// 메뉴 삭제 요청을 검증합니다.
	public String validateAdminMenuDelete(MenuManageSavePO param) {
		normalizeMenuManageSaveParam(param);
		if (param == null) {
			return "요청 데이터가 없습니다.";
		}
		if (param.getMenuNo() <= 0) {
			return "메뉴 번호를 확인해주세요.";
		}
		if (param.getUdtNo() == null) {
			return "수정자 정보를 확인해주세요.";
		}
		MenuBase menu = menuBaseRepository.findByMenuNo(param.getMenuNo()).orElse(null);
		if (menu == null) {
			return "메뉴 정보를 확인해주세요.";
		}
		if (menuBaseRepository.countByUpMenuNo(param.getMenuNo()) > 0) {
			return "하위 메뉴가 존재하여 삭제할 수 없습니다.";
		}
		return null;
	}

	// 메뉴를 등록합니다.
	@Transactional
	@CacheEvict(value = "menuListCache", allEntries = true)
	public int createAdminMenu(MenuManageSavePO param) {
		normalizeMenuManageSaveParam(param);
		int upMenuNo = param.getUpMenuNo();
		MenuBase menu = new MenuBase();
		menu.setUpMenuNo(upMenuNo);
		menu.setSortSeq(resolveMenuSortSeq(upMenuNo, param.getSortSeq()));
		menu.setMenuLevel(resolveMenuLevel(upMenuNo));
		menu.setMenuNm(param.getMenuNm());
		menu.setMenuUrl(trimToNull(param.getMenuUrl()));
		menu.setUseYn(resolveYn(param.getUseYn()));
		menu.setRegNo(param.getRegNo());
		menu.setRegDt(LocalDateTime.now());
		menu.setUdtNo(resolveEditorNo(param.getUdtNo(), param.getRegNo()));
		menu.setUdtDt(LocalDateTime.now());
		menuBaseRepository.save(menu);
		return menu.getMenuNo();
	}

	// 메뉴를 수정합니다.
	@Transactional
	@CacheEvict(value = "menuListCache", allEntries = true)
	public int updateAdminMenu(MenuManageSavePO param) {
		normalizeMenuManageSaveParam(param);
		MenuBase current = menuBaseRepository.findByMenuNo(param.getMenuNo()).orElse(null);
		if (current == null) {
			return 0;
		}
		current.setMenuNm(param.getMenuNm());
		current.setMenuUrl(trimToNull(param.getMenuUrl()));
		current.setUseYn(resolveYn(param.getUseYn()));
		current.setSortSeq(param.getSortSeq() == null ? current.getSortSeq() : param.getSortSeq());
		current.setUdtNo(param.getUdtNo());
		current.setUdtDt(LocalDateTime.now());
		menuBaseRepository.save(current);
		return 1;
	}

	// 메뉴를 삭제합니다.
	@Transactional
	@CacheEvict(value = "menuListCache", allEntries = true)
	public int deleteAdminMenu(MenuManageSavePO param) {
		if (param == null || param.getMenuNo() <= 0) {
			return 0;
		}
		MenuBase current = menuBaseRepository.findByMenuNo(param.getMenuNo()).orElse(null);
		if (current == null) {
			return 0;
		}
		menuBaseRepository.delete(current);
		return 1;
	}

	// 공통 코드 그룹 목록 조회용 그룹코드 검색값 길이 제한입니다.
	private static final int MENU_LEVEL_MAX = 3;
	// 메뉴명 길이 제한입니다.
	private static final int MENU_NAME_MAX_LENGTH = 50;
	// 메뉴 URL 길이 제한입니다.
	private static final int MENU_URL_MAX_LENGTH = 255;
	private static final int CODE_MAX_LENGTH = 20;
	// 공통 코드명 길이 제한입니다.
	private static final int CODE_NAME_MAX_LENGTH = 50;
	// 공통 코드 설명 길이 제한입니다.
	private static final int CODE_DESC_MAX_LENGTH = 60;

	// 공개용 공통 코드 목록을 조회합니다.
	public List<CommonCodeVO> getCommonCodeList(String grpCd) {
		return commonMapper.getCommonCodeList(grpCd);
	}

	// 관리자용 상위 공통 코드 목록을 조회합니다.
	public List<CommonCodeVO> getAdminRootCommonCodeList(String grpCd, String grpCdNm) {
		// 검색 파라미터 공백을 정리합니다.
		String resolvedGrpCd = trimToNull(grpCd);
		String resolvedGrpCdNm = trimToNull(grpCdNm);
		return commonMapper.getAdminRootCommonCodeList(resolvedGrpCd, resolvedGrpCdNm);
	}

	// 관리자용 하위 공통 코드 목록을 조회합니다.
	public List<CommonCodeVO> getAdminChildCommonCodeList(String grpCd) {
		// 그룹코드가 없으면 빈 목록을 반환합니다.
		String resolvedGrpCd = trimToNull(grpCd);
		if (resolvedGrpCd == null) {
			return List.of();
		}
		return commonMapper.getAdminChildCommonCodeList(resolvedGrpCd);
	}

	// 관리자용 공통 코드 등록 요청을 검증합니다.
	public String validateAdminCommonCodeCreate(CommonCodeManagePO param) {
		// 요청 데이터 필수값을 확인합니다.
		if (param == null) {
			return "요청 데이터가 없습니다.";
		}
		if (isBlank(param.getGrpCd())) {
			return "그룹코드를 입력해주세요.";
		}
		if (isBlank(param.getCd())) {
			return "코드를 입력해주세요.";
		}
		if (isBlank(param.getCdNm())) {
			return "코드명을 입력해주세요.";
		}
		if (param.getRegNo() == null) {
			return "등록자 정보를 확인해주세요.";
		}
		// 공통 코드 입력값 형식을 검증합니다.
		String formatMessage = validateCommonCodeFormat(param);
		if (formatMessage != null) {
			return formatMessage;
		}
		// 하위 코드 등록 시 상위 그룹코드 존재 여부를 확인합니다.
		if (!"ROOT".equals(param.getGrpCd()) && commonMapper.countAdminCommonCode("ROOT", param.getGrpCd()) == 0) {
			return "상위 그룹코드를 확인해주세요.";
		}
		// 중복 그룹코드/코드 조합 여부를 확인합니다.
		int duplicateCount = commonMapper.countAdminCommonCode(param.getGrpCd(), param.getCd());
		if (duplicateCount > 0) {
			return "이미 등록된 그룹코드/코드입니다.";
		}
		return null;
	}

	// 관리자용 공통 코드 수정 요청을 검증합니다.
	public String validateAdminCommonCodeUpdate(CommonCodeManagePO param) {
		// 요청 데이터 필수값을 확인합니다.
		if (param == null) {
			return "요청 데이터가 없습니다.";
		}
		if (isBlank(param.getOriginGrpCd()) || isBlank(param.getOriginCd())) {
			return "수정 대상 코드를 확인해주세요.";
		}
		if (isBlank(param.getGrpCd())) {
			return "그룹코드를 입력해주세요.";
		}
		if (isBlank(param.getCd())) {
			return "코드를 입력해주세요.";
		}
		if (isBlank(param.getCdNm())) {
			return "코드명을 입력해주세요.";
		}
		if (param.getUdtNo() == null) {
			return "수정자 정보를 확인해주세요.";
		}
		// 기존 코드 존재 여부를 확인합니다.
		CommonCodeVO current = commonMapper.getAdminCommonCodeDetail(param.getOriginGrpCd(), param.getOriginCd());
		if (current == null) {
			return "수정 대상 코드를 확인해주세요.";
		}
		// 공통 코드 입력값 형식을 검증합니다.
		String formatMessage = validateCommonCodeFormat(param);
		if (formatMessage != null) {
			return formatMessage;
		}
		// 하위 코드 등록 시 상위 그룹코드 존재 여부를 확인합니다.
		if (!"ROOT".equals(param.getGrpCd()) && commonMapper.countAdminCommonCode("ROOT", param.getGrpCd()) == 0) {
			return "상위 그룹코드를 확인해주세요.";
		}
		// 상위 그룹코드 변경 시 하위 데이터 존재 여부를 확인합니다.
		if ("ROOT".equals(param.getOriginGrpCd())
			&& !param.getOriginCd().equals(param.getCd())
			&& commonMapper.countAdminCommonCodeByGrpCd(param.getOriginCd()) > 0) {
			return "하위 코드가 존재하여 상위 코드값은 수정할 수 없습니다.";
		}
		// 중복 그룹코드/코드 조합 여부를 확인합니다.
		int duplicateCount = commonMapper.countAdminCommonCodeExcludeOrigin(
			param.getGrpCd(),
			param.getCd(),
			param.getOriginGrpCd(),
			param.getOriginCd()
		);
		if (duplicateCount > 0) {
			return "이미 등록된 그룹코드/코드입니다.";
		}
		return null;
	}

	// 관리자용 공통 코드를 등록합니다.
	@Transactional
	public int createAdminCommonCode(CommonCodeManagePO param) {
		// 저장 전 공백 값을 정리합니다.
		normalizeCommonCodeParam(param);
		// 등록자 기준으로 수정자를 설정합니다.
		if (param.getUdtNo() == null) {
			param.setUdtNo(param.getRegNo());
		}
		// 표시 여부 기본값을 설정합니다.
		if (isBlank(param.getUseYn())) {
			param.setUseYn("Y");
		}
		// 정렬순서 기본값을 설정합니다.
		if (param.getDispOrd() == null) {
			param.setDispOrd(1);
		}
		return commonMapper.insertAdminCommonCode(param);
	}

	// 관리자용 공통 코드를 수정합니다.
	@Transactional
	public int updateAdminCommonCode(CommonCodeManagePO param) {
		// 저장 전 공백 값을 정리합니다.
		normalizeCommonCodeParam(param);
		// 표시 여부 기본값을 설정합니다.
		if (isBlank(param.getUseYn())) {
			param.setUseYn("Y");
		}
		// 정렬순서 기본값을 설정합니다.
		if (param.getDispOrd() == null) {
			param.setDispOrd(1);
		}
		// 기본코드 정보를 수정합니다.
		int updated = commonMapper.updateAdminCommonCode(param);
		// 상위 코드 변경 시 하위 그룹코드를 동기화합니다.
		if ("ROOT".equals(param.getOriginGrpCd()) && !param.getOriginCd().equals(param.getCd())) {
			commonMapper.updateAdminCommonCodeChildrenGrpCd(param.getOriginCd(), param.getCd());
		}
		return updated;
	}

	// 공통 코드 저장 포맷을 검증합니다.
	private String validateCommonCodeFormat(CommonCodeManagePO param) {
		// 그룹코드 길이와 문자 형식을 검증합니다.
		String grpCd = trimToNull(param.getGrpCd());
		if (grpCd != null) {
			if (grpCd.length() > CODE_MAX_LENGTH) {
				return "그룹코드는 20자 이내로 입력해주세요.";
			}
			if (containsKorean(grpCd)) {
				return "그룹코드는 한글을 입력할 수 없습니다.";
			}
		}
		// 코드 길이와 문자 형식을 검증합니다.
		String cd = trimToNull(param.getCd());
		if (cd != null) {
			if (cd.length() > CODE_MAX_LENGTH) {
				return "코드는 20자 이내로 입력해주세요.";
			}
			if (containsKorean(cd)) {
				return "코드는 한글을 입력할 수 없습니다.";
			}
		}
		// 코드명 길이를 검증합니다.
		String cdNm = trimToNull(param.getCdNm());
		if (cdNm != null && cdNm.length() > CODE_NAME_MAX_LENGTH) {
			return "코드명은 50자 이내로 입력해주세요.";
		}
		// 코드 설명 길이를 검증합니다.
		String cdDesc = trimToNull(param.getCdDesc());
		if (cdDesc != null && cdDesc.length() > CODE_DESC_MAX_LENGTH) {
			return "코드 설명은 60자 이내로 입력해주세요.";
		}
		// 사용여부 형식을 검증합니다.
		String useYn = trimToNull(param.getUseYn());
		if (useYn != null && !"Y".equals(useYn) && !"N".equals(useYn)) {
			return "사용여부를 확인해주세요.";
		}
		return null;
	}

	// 공통 코드 저장 파라미터의 공백값을 정리합니다.
	private void normalizeCommonCodeParam(CommonCodeManagePO param) {
		param.setOriginGrpCd(trimToNull(param.getOriginGrpCd()));
		param.setOriginCd(trimToNull(param.getOriginCd()));
		param.setGrpCd(trimToNull(param.getGrpCd()));
		param.setCd(trimToNull(param.getCd()));
		param.setCdNm(trimToNull(param.getCdNm()));
		param.setCdDesc(trimToNull(param.getCdDesc()));
		param.setUseYn(trimToNull(param.getUseYn()));
	}

	// 문자열의 공백 여부를 확인합니다.
	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

	// 문자열 양끝 공백 제거 후 빈값은 null로 변환합니다.
	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	// 문자열에 한글 포함 여부를 확인합니다.
	private boolean containsKorean(String value) {
		return value != null && value.matches(".*[ㄱ-ㅎㅏ-ㅣ가-힣]+.*");
	}

	// 메뉴 관리 DTO를 View 객체로 변환합니다.
	private MenuManageVO toMenuManageVO(MenuBase item, int childCount) {
		MenuManageVO vo = new MenuManageVO();
		vo.setMenuNo(item.getMenuNo());
		vo.setUpMenuNo(item.getUpMenuNo());
		vo.setMenuLevel(item.getMenuLevel());
		vo.setMenuNm(item.getMenuNm());
		vo.setMenuUrl(item.getMenuUrl());
		vo.setSortSeq(item.getSortSeq());
		vo.setUseYn(item.getUseYn());
		vo.setChildCount(childCount);
		return vo;
	}

	// 메뉴 URL 중복/공백/유효성 검증을 위한 공통 정규화를 수행합니다.
	private void normalizeMenuManageSaveParam(MenuManageSavePO param) {
		if (param == null) {
			return;
		}
		param.setMenuNm(trimToNull(param.getMenuNm()));
		param.setMenuUrl(trimToNull(param.getMenuUrl()));
		param.setUseYn(trimToNull(param.getUseYn()));
		if (param.getUpMenuNo() == null) {
			param.setUpMenuNo(0);
		}
		if (param.getSortSeq() != null && param.getSortSeq() < 0) {
			param.setSortSeq(1);
		}
	}

	// 정렬 순서 기본값을 계산합니다.
	private int resolveMenuSortSeq(int upMenuNo, Integer sortSeq) {
		if (sortSeq != null && sortSeq > 0) {
			return sortSeq;
		}
		Integer maxSortSeq = menuBaseRepository.findMaxSortSeqByUpMenuNo(upMenuNo);
		return maxSortSeq == null ? 1 : maxSortSeq + 1;
	}

	// 메뉴 사용여부를 정규화합니다.
	private String resolveYn(String useYn) {
		String resolvedUseYn = trimToNull(useYn);
		return resolvedUseYn == null ? "Y" : resolvedUseYn;
	}

	// 메뉴 레벨을 계산합니다.
	private int resolveMenuLevel(int upMenuNo) {
		if (upMenuNo == 0) {
			return 1;
		}
		MenuBase parent = menuBaseRepository.findByMenuNo(upMenuNo).orElse(null);
		if (parent == null) {
			return 1;
		}
		return parent.getMenuLevel() + 1;
	}

	// 메뉴 번호로 메뉴 정보를 조회합니다.
	private MenuBase findMenu(int menuNo) {
		return menuBaseRepository.findByMenuNo(menuNo).orElse(null);
	}

	// 수정자 값이 없을 경우 등록자 값을 사용합니다.
	private Long resolveEditorNo(Long udtNo, Long regNo) {
		if (udtNo != null) {
			return udtNo;
		}
		return regNo;
	}
}
