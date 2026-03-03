package com.xodud1202.springbackend.service;

import com.xodud1202.springbackend.domain.admin.common.MenuBase;
import com.xodud1202.springbackend.domain.admin.common.MenuManageSavePO;
import com.xodud1202.springbackend.domain.admin.common.MenuManageVO;
import com.xodud1202.springbackend.mapper.CommonMapper;
import com.xodud1202.springbackend.repository.MenuBaseRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
// 메뉴 관리 서비스의 핵심 검증/저장 로직을 테스트합니다.
class AdminCommonServiceTests {
	@Mock
	private MenuBaseRepository menuBaseRepository;

	@Mock
	private CommonMapper commonMapper;

	@InjectMocks
	private AdminCommonService adminCommonService;

	@Test
	@DisplayName("메뉴 저장: 상위 메뉴가 URL을 가지면 자식 메뉴 등록이 불가")
	// 상위 메뉴가 URL이 있는 경우 하위 메뉴 등록을 차단합니다.
	void validateAdminMenuCreate_returnsErrorWhenParentHasMenuUrl() {
		// 상위 메뉴에 URL을 가진 메뉴를 설정합니다.
		MenuBase parent = new MenuBase();
		parent.setMenuNo(1);
		parent.setMenuNm("운영");
		parent.setMenuLevel(1);
		parent.setUpMenuNo(0);
		parent.setSortSeq(1);
		parent.setUseYn("Y");
		parent.setMenuUrl("/manage/admin");
		parent.setRegNo(1L);
		parent.setRegDt(LocalDateTime.now());
		parent.setUdtNo(1L);
		parent.setUdtDt(LocalDateTime.now());
		when(menuBaseRepository.findByMenuNo(1)).thenReturn(Optional.of(parent));

		MenuManageSavePO request = new MenuManageSavePO();
		request.setUpMenuNo(1);
		request.setMenuNm("하위");
		request.setRegNo(1L);

		// 하위 생성 검증을 수행합니다.
		String message = adminCommonService.validateAdminMenuCreate(request);

		// URL이 있는 상위 메뉴에는 하위 메뉴를 허용하지 않는 메시지를 확인합니다.
		assertEquals("메뉴 URL이 등록된 항목에는 하위 메뉴를 추가할 수 없습니다.", message);
	}

	@Test
	@DisplayName("메뉴 저장: URL 중복은 등록할 수 없음")
	// 같은 URL이 이미 존재하면 등록 검증을 실패 처리합니다.
	void validateAdminMenuCreate_returnsErrorWhenMenuUrlDuplicate() {
		// 기본 상위 메뉴는 등록 가능한 상태로 구성합니다.
		MenuBase parent = new MenuBase();
		parent.setMenuNo(1);
		parent.setMenuNm("운영");
		parent.setMenuLevel(1);
		parent.setUpMenuNo(0);
		parent.setSortSeq(1);
		parent.setUseYn("Y");
		parent.setRegNo(1L);
		parent.setRegDt(LocalDateTime.now());
		parent.setUdtNo(1L);
		parent.setUdtDt(LocalDateTime.now());
		when(menuBaseRepository.findByMenuNo(1)).thenReturn(Optional.of(parent));
		when(menuBaseRepository.countByMenuUrl("/dup")).thenReturn(1);

		MenuManageSavePO request = new MenuManageSavePO();
		request.setUpMenuNo(1);
		request.setMenuNm("중복");
		request.setMenuUrl("/dup");
		request.setRegNo(1L);

		// URL 중복 검증을 수행합니다.
		String message = adminCommonService.validateAdminMenuCreate(request);

		// 중복 URL 메시지를 확인합니다.
		assertEquals("이미 등록된 메뉴 URL입니다.", message);
	}

	@Test
	@DisplayName("메뉴 수정: 하위 메뉴가 있으면 URL 등록을 막는다")
	// 자식 메뉴가 있는 항목은 URL을 가진 상태로 저장할 수 없습니다.
	void validateAdminMenuUpdate_returnsErrorWhenMenuHasChildrenAndMenuUrlSet() {
		// 현재 메뉴 정보를 구성합니다.
		MenuBase current = new MenuBase();
		current.setMenuNo(10);
		current.setMenuNm("상품");
		current.setMenuLevel(2);
		current.setUpMenuNo(1);
		current.setSortSeq(1);
		current.setUseYn("Y");
		current.setRegNo(1L);
		current.setRegDt(LocalDateTime.now());
		current.setUdtNo(1L);
		current.setUdtDt(LocalDateTime.now());
		when(menuBaseRepository.findByMenuNo(10)).thenReturn(Optional.of(current));
		when(menuBaseRepository.countByUpMenuNo(10)).thenReturn(2);

		MenuManageSavePO request = new MenuManageSavePO();
		request.setMenuNo(10);
		request.setMenuNm("상품");
		request.setMenuUrl("/new");
		request.setUdtNo(1L);

		// 수정 요청 검증을 수행합니다.
		String message = adminCommonService.validateAdminMenuUpdate(request);

		// 하위 메뉴가 존재하면 URL 등록을 차단하는지 확인합니다.
		assertEquals("하위 메뉴가 있는 메뉴는 메뉴 URL을 등록할 수 없습니다.", message);
	}

	@Test
	@DisplayName("메뉴 목록: 자식 수와 함께 트리 표시용 목록을 반환")
	// 자식 개수를 계산해 반환값에 반영합니다.
	void getAdminMenuManageList_populatesChildCount() {
		// 샘플 메뉴를 구성합니다.
		MenuBase root = new MenuBase();
		root.setMenuNo(1);
		root.setMenuNm("루트");
		root.setMenuLevel(1);
		root.setUpMenuNo(0);
		root.setSortSeq(1);
		root.setUseYn("Y");
		MenuBase child = new MenuBase();
		child.setMenuNo(2);
		child.setMenuNm("하위");
		child.setMenuLevel(2);
		child.setUpMenuNo(1);
		child.setSortSeq(1);
		child.setUseYn("Y");
		when(menuBaseRepository.findAll()).thenReturn(List.of(root, child));

		// 목록을 조회합니다.
		List<MenuManageVO> list = adminCommonService.getAdminMenuManageList();

		// 목록과 자식 수를 확인합니다.
		assertEquals(2, list.size());
		MenuManageVO rootVO = list.get(0).getMenuNo() == 1 ? list.get(0) : list.get(1);
		assertEquals(1, rootVO.getChildCount());
		assertEquals(0, list.stream().filter(item -> item.getMenuNo() == 2).findFirst().get().getChildCount());
	}

	@Test
	@DisplayName("메뉴 등록: 기본 정렬순서를 반영해 저장 후 메뉴번호를 반환")
	// 입력 정렬순서가 없으면 상위 기준 최대값 + 1로 저장합니다.
	void createAdminMenu_savesAndReturnsGeneratedMenuNo() {
		// 상위 메뉴를 구성합니다.
		MenuBase parent = new MenuBase();
		parent.setMenuNo(1);
		parent.setMenuNm("루트");
		parent.setMenuLevel(1);
		parent.setUpMenuNo(0);
		parent.setSortSeq(1);
		parent.setUseYn("Y");
		parent.setMenuUrl("");
		parent.setRegNo(1L);
		parent.setRegDt(LocalDateTime.now());
		parent.setUdtNo(1L);
		parent.setUdtDt(LocalDateTime.now());
		when(menuBaseRepository.findByMenuNo(1)).thenReturn(Optional.of(parent));
		when(menuBaseRepository.countByMenuUrl("/new")).thenReturn(0);
		when(menuBaseRepository.findMaxSortSeqByUpMenuNo(1)).thenReturn(3);
		when(menuBaseRepository.save(any(MenuBase.class))).thenAnswer(invocation -> {
			MenuBase saved = invocation.getArgument(0);
			saved.setMenuNo(100);
			return saved;
		});

		MenuManageSavePO request = new MenuManageSavePO();
		request.setUpMenuNo(1);
		request.setMenuNm("신규");
		request.setMenuUrl("/new");
		request.setRegNo(10L);
		request.setUdtNo(10L);
		request.setUseYn("Y");

		// 정렬순서 미지정 상태로 저장을 실행합니다.
		Integer newMenuNo = adminCommonService.createAdminMenu(request);
		assertEquals(100, newMenuNo);

		// 정렬순서가 반영되었는지 확인합니다.
		ArgumentCaptor<MenuBase> captor = ArgumentCaptor.forClass(MenuBase.class);
		verify(menuBaseRepository).save(captor.capture());
		assertNotNull(captor.getValue());
		assertEquals(4, captor.getValue().getSortSeq());
	}

	@Test
	@DisplayName("메뉴 삭제: 사용자가 누락되면 검증 실패 메시지 반환")
	// 삭제 요청 시 수정자 정보 미입력은 실패 메시지를 반환합니다.
	void validateAdminMenuDelete_returnsErrorWhenUdtNoMissing() {
		// 삭제 대상을 설정합니다.
		MenuBase target = new MenuBase();
		target.setMenuNo(20);
		target.setMenuNm("메뉴");
		target.setMenuLevel(2);
		target.setUpMenuNo(1);
		target.setSortSeq(1);
		target.setUseYn("Y");
		when(menuBaseRepository.findByMenuNo(20)).thenReturn(Optional.of(target));
		when(menuBaseRepository.countByUpMenuNo(20)).thenReturn(0);

		MenuManageSavePO request = new MenuManageSavePO();
		request.setMenuNo(20);

		// 삭제 검증을 수행합니다.
		String message = adminCommonService.validateAdminMenuDelete(request);

		// 수정자 미입력 메시지를 확인합니다.
		assertEquals("수정자 정보를 확인해주세요.", message);
	}
}
