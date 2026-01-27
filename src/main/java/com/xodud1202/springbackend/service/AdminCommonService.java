package com.xodud1202.springbackend.service;

import com.xodud1202.springbackend.domain.admin.common.AdminMenuLnb;
import com.xodud1202.springbackend.domain.admin.common.MenuBase;
import com.xodud1202.springbackend.domain.common.CommonCodeVO;
import com.xodud1202.springbackend.mapper.CommonMapper;
import com.xodud1202.springbackend.repository.MenuBaseRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

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

	public List<CommonCodeVO> getCommonCodeList(String grpCd) {
		return commonMapper.getCommonCodeList(grpCd);
	}
}
