package com.xodud1202.springbackend.service;

import static com.xodud1202.springbackend.common.util.CommonPaginationUtils.*;
import static com.xodud1202.springbackend.common.util.CommonTextUtils.*;

import com.xodud1202.springbackend.domain.admin.exhibition.ExhibitionDeletePO;
import com.xodud1202.springbackend.domain.admin.exhibition.ExhibitionDetailVO;
import com.xodud1202.springbackend.domain.admin.exhibition.ExhibitionGoodsPO;
import com.xodud1202.springbackend.domain.admin.exhibition.ExhibitionGoodsVO;
import com.xodud1202.springbackend.domain.admin.exhibition.ExhibitionPO;
import com.xodud1202.springbackend.domain.admin.exhibition.ExhibitionSavePO;
import com.xodud1202.springbackend.domain.admin.exhibition.ExhibitionTabPO;
import com.xodud1202.springbackend.domain.admin.exhibition.ExhibitionVO;
import com.xodud1202.springbackend.domain.common.FtpProperties;
import com.xodud1202.springbackend.domain.shop.exhibition.ShopExhibitionDetailVO;
import com.xodud1202.springbackend.domain.shop.exhibition.ShopExhibitionGoodsItemVO;
import com.xodud1202.springbackend.domain.shop.exhibition.ShopExhibitionGoodsPageVO;
import com.xodud1202.springbackend.domain.shop.exhibition.ShopExhibitionItemVO;
import com.xodud1202.springbackend.domain.shop.exhibition.ShopExhibitionPageVO;
import com.xodud1202.springbackend.domain.shop.exhibition.ShopExhibitionTabVO;
import com.xodud1202.springbackend.mapper.ExhibitionMapper;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
// 기획전 관리 비즈니스 로직을 처리합니다.
public class ExhibitionService {
	private static final String YN_Y = "Y";
	private static final String YN_N = "N";
	private static final String SEARCH_GB_NO = "NO";
	private static final String SEARCH_GB_NAME = "NM";
	private static final int MIN_PAGE_SIZE = 1;
	private static final int DEFAULT_PAGE_SIZE = 20;
	private static final int MAX_PAGE_SIZE = 200;
	private static final int SHOP_EXHIBITION_PAGE_SIZE = 20;
	private static final int SHOP_EXHIBITION_GOODS_PAGE_SIZE = 20;
	private static final int DEFAULT_MAX_ORDER = 99999;
	private static final DateTimeFormatter SEARCH_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	private static final DateTimeFormatter BASIC_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
	private static final DateTimeFormatter DISPLAY_PERIOD_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final String SHOP_EXHIBITION_NOT_FOUND_MESSAGE = "기획전 정보를 찾을 수 없습니다.";
	private static final String SHOP_EXHIBITION_TAB_NOT_FOUND_MESSAGE = "기획전 탭 정보를 찾을 수 없습니다.";
	private static final String HEADER_GOODS_ID = "상품코드";
	private static final String HEADER_DISPLAY_ORDER = "노출순서";
	private static final int EXHIBITION_THUMBNAIL_WIDTH = 750;
	private static final int EXHIBITION_THUMBNAIL_HEIGHT = 1024;

	private final ExhibitionMapper exhibitionMapper;
	private final FtpProperties ftpProperties;
	private final FtpFileService ftpFileService;

	// 기획전 목록을 조회합니다.
	public Map<String, Object> getAdminExhibitionList(
		Integer page,
		String searchGb,
		String searchValue,
		String searchStartDt,
		String searchEndDt,
		Integer pageSize
	) {
		// 페이지/사이즈를 기본값으로 정리합니다.
		int resolvedPage = normalizePage(page, MIN_PAGE_SIZE);
		int resolvedPageSize = normalizePageSize(pageSize, DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE);
		int offset = calculateOffset(resolvedPage, resolvedPageSize);

		ExhibitionPO param = new ExhibitionPO();
		param.setPage(resolvedPage);
		param.setPageSize(resolvedPageSize);
		param.setOffset(offset);
		param.setSearchGb(normalizeSearchGb(trimToNull(searchGb), trimToNull(searchValue)));
		param.setSearchValue(trimToNull(searchValue));

		// 검색 기간을 정규화하고 유효성을 검사합니다.
		String searchValidateMessage = normalizeSearchDateRange(param, searchStartDt, searchEndDt);
		if (searchValidateMessage != null) {
			throw new IllegalArgumentException(searchValidateMessage);
		}

		// 목록 및 건수를 조회합니다.
		List<ExhibitionVO> list = exhibitionMapper.getAdminExhibitionList(param);
		int totalCount = exhibitionMapper.getAdminExhibitionCount(param);

		Map<String, Object> result = new HashMap<>();
		result.put("list", list);
		result.put("totalCount", totalCount);
		result.put("page", resolvedPage);
		result.put("pageSize", resolvedPageSize);
		return result;
	}

	// 쇼핑몰 기획전 목록 페이지 데이터를 조회합니다.
	public ShopExhibitionPageVO getShopExhibitionPage(Integer requestedPageNo) {
		// 요청 페이지 번호를 1 이상 값으로 보정합니다.
		int resolvedRequestedPageNo = normalizePage(requestedPageNo, MIN_PAGE_SIZE);
		// 노출 조건에 맞는 전체 기획전 건수를 조회합니다.
		int totalCount = exhibitionMapper.countShopVisibleExhibitionList();
		// 전체 페이지 수를 계산합니다.
		int totalPageCount = calculateTotalPageCount(totalCount, SHOP_EXHIBITION_PAGE_SIZE);
		// 범위를 초과한 페이지 번호를 마지막 페이지로 보정합니다.
		int resolvedPageNo = resolvePageNoWithinRange(resolvedRequestedPageNo, totalPageCount);
		// 페이징 오프셋을 계산합니다.
		int offset = calculateOffset(resolvedPageNo, SHOP_EXHIBITION_PAGE_SIZE);
		// 노출 조건에 맞는 기획전 목록을 조회합니다.
		List<ShopExhibitionItemVO> exhibitionList = exhibitionMapper.getShopVisibleExhibitionList(offset, SHOP_EXHIBITION_PAGE_SIZE);
		List<ShopExhibitionItemVO> safeExhibitionList = exhibitionList == null ? List.of() : exhibitionList;

		// 쇼핑몰 기획전 목록 화면 응답 객체를 구성합니다.
		ShopExhibitionPageVO result = new ShopExhibitionPageVO();
		result.setExhibitionList(safeExhibitionList);
		result.setTotalCount(totalCount);
		result.setPageNo(resolvedPageNo);
		result.setPageSize(SHOP_EXHIBITION_PAGE_SIZE);
		result.setTotalPageCount(totalPageCount);
		return result;
	}

	// 쇼핑몰 기획전 상세 화면 데이터를 조회합니다.
	public ShopExhibitionDetailVO getShopExhibitionDetail(Integer exhibitionNo) {
		// 유효하지 않은 기획전 번호는 바로 예외 처리합니다.
		if (exhibitionNo == null || exhibitionNo < 1) {
			throw new IllegalArgumentException("기획전 번호를 확인해주세요.");
		}

		// 노출 가능한 기획전 마스터 정보를 조회합니다.
		ShopExhibitionDetailVO detail = exhibitionMapper.getShopVisibleExhibitionDetail(exhibitionNo);
		if (detail == null) {
			throw new IllegalArgumentException(SHOP_EXHIBITION_NOT_FOUND_MESSAGE);
		}

		// 노출 가능한 탭 목록을 조회하고 기본 선택 탭을 확정합니다.
		List<ShopExhibitionTabVO> tabList = exhibitionMapper.getShopVisibleExhibitionTabList(exhibitionNo);
		List<ShopExhibitionTabVO> safeTabList = tabList == null ? List.of() : tabList;
		if (safeTabList.isEmpty()) {
			throw new IllegalArgumentException(SHOP_EXHIBITION_NOT_FOUND_MESSAGE);
		}

		// PC/모바일 HTML과 기본 선택 탭을 응답 객체에 반영합니다.
		detail.setTabList(safeTabList);
		detail.setDefaultTabNo(safeTabList.get(0).getExhibitionTabNo());
		detail.setVisibleHtml(resolveVisibleExhibitionHtml(detail.getPcHtml(), detail.getMobileHtml()));
		return detail;
	}

	// 쇼핑몰 기획전 탭 상품 더보기 데이터를 조회합니다.
	public ShopExhibitionGoodsPageVO getShopExhibitionGoodsPage(Integer exhibitionNo, Integer exhibitionTabNo, Integer requestedPageNo) {
		// 필수 식별자 유효성을 먼저 확인합니다.
		if (exhibitionNo == null || exhibitionNo < 1) {
			throw new IllegalArgumentException("기획전 번호를 확인해주세요.");
		}
		if (exhibitionTabNo == null || exhibitionTabNo < 1) {
			throw new IllegalArgumentException("기획전 탭 번호를 확인해주세요.");
		}

		// 기획전/탭 노출 가능 여부를 먼저 확인합니다.
		assertShopExhibitionVisible(exhibitionNo);
		assertShopExhibitionTabVisible(exhibitionNo, exhibitionTabNo);

		// 페이지 번호와 전체 상품 건수를 기준으로 조회 범위를 계산합니다.
		int resolvedRequestedPageNo = normalizePage(requestedPageNo, MIN_PAGE_SIZE);
		int totalCount = exhibitionMapper.countShopVisibleExhibitionGoods(exhibitionNo, exhibitionTabNo);
		if (totalCount < 1) {
			throw new IllegalArgumentException(SHOP_EXHIBITION_TAB_NOT_FOUND_MESSAGE);
		}
		int totalPageCount = calculateTotalPageCount(totalCount, SHOP_EXHIBITION_GOODS_PAGE_SIZE);
		int resolvedPageNo = resolvePageNoWithinRange(resolvedRequestedPageNo, totalPageCount);
		int offset = calculateOffset(resolvedPageNo, SHOP_EXHIBITION_GOODS_PAGE_SIZE);

		// 탭 상품 목록을 조회하고 이미지 URL을 채웁니다.
		List<ShopExhibitionGoodsItemVO> goodsList =
			exhibitionMapper.getShopVisibleExhibitionGoodsList(exhibitionNo, exhibitionTabNo, offset, SHOP_EXHIBITION_GOODS_PAGE_SIZE);
		List<ShopExhibitionGoodsItemVO> safeGoodsList = goodsList == null ? List.of() : goodsList;
		applyShopExhibitionGoodsImageUrls(safeGoodsList);

		// 더보기 응답 객체를 구성합니다.
		ShopExhibitionGoodsPageVO result = new ShopExhibitionGoodsPageVO();
		result.setGoodsList(safeGoodsList);
		result.setTotalCount(totalCount);
		result.setPageNo(resolvedPageNo);
		result.setPageSize(SHOP_EXHIBITION_GOODS_PAGE_SIZE);
		result.setHasMore(resolvedPageNo < totalPageCount);
		result.setNextPageNo(resolvedPageNo < totalPageCount ? resolvedPageNo + 1 : null);
		return result;
	}

	// 기획전 상세를 조회합니다.
	public ExhibitionDetailVO getAdminExhibitionDetail(Integer exhibitionNo) {
		// 유효하지 않은 식별자는 조회하지 않습니다.
		if (exhibitionNo == null || exhibitionNo < 1) {
			return null;
		}
		// 기본 마스터 정보를 조회합니다.
		ExhibitionDetailVO detail = exhibitionMapper.getAdminExhibitionDetail(exhibitionNo);
		if (detail == null) {
			return null;
		}
		// 탭 및 상품 정보를 함께 조회합니다.
		detail.setTabList(exhibitionMapper.getExhibitionTabList(exhibitionNo));
		List<ExhibitionGoodsVO> goodsList = exhibitionMapper.getExhibitionGoodsList(exhibitionNo, null);
		applyGoodsImageUrls(goodsList);
		detail.setGoodsList(goodsList);
		return detail;
	}

	// 기획전 상품 이미지 URL을 적용합니다.
	private void applyGoodsImageUrls(List<ExhibitionGoodsVO> goodsList) {
		if (goodsList == null || goodsList.isEmpty()) {
			return;
		}
		for (ExhibitionGoodsVO item : goodsList) {
			if (item == null) {
				continue;
			}
			String imgPath = item.getImgPath();
			if (isBlank(imgPath)) {
				continue;
			}
			if (imgPath.startsWith("http://") || imgPath.startsWith("https://")) {
				item.setImgUrl(imgPath);
				continue;
			}
			item.setImgUrl(ftpFileService.buildGoodsImageUrl(item.getGoodsId(), imgPath));
		}
	}

	// 쇼핑몰 기획전 탭 상품 이미지 URL을 적용합니다.
	private void applyShopExhibitionGoodsImageUrls(List<ShopExhibitionGoodsItemVO> goodsList) {
		if (goodsList == null || goodsList.isEmpty()) {
			return;
		}
		for (ShopExhibitionGoodsItemVO item : goodsList) {
			if (item == null) {
				continue;
			}

			// 기본/보조 이미지 경로를 웹 접근 URL로 변환합니다.
			item.setImgUrl(resolveGoodsImageUrl(item.getGoodsId(), item.getImgPath()));
			item.setSecondaryImgUrl(resolveGoodsImageUrl(item.getGoodsId(), item.getSecondaryImgPath()));
		}
	}

	// 기획전 저장 요청의 공통 유효성을 검증합니다.
	public String validateExhibitionSave(ExhibitionSavePO param, boolean isCreate) {
		// 필수 요청값을 확인합니다.
		if (param == null) {
			return "요청 데이터가 없습니다.";
		}
		// 수정 모드는 기획전 번호가 필수입니다.
		if (!isCreate && (param.getExhibitionNo() == null || param.getExhibitionNo() < 1)) {
			return "기획전 번호를 확인해주세요.";
		}
		if (trimToNull(param.getExhibitionNm()) == null) {
			return "기획전명을 입력해주세요.";
		}
		if (param.getListShowYn() == null || !isYnValue(param.getListShowYn().trim())) {
			return "리스트 노출 여부를 확인해주세요.";
		}
		if (param.getShowYn() == null || !isYnValue(param.getShowYn().trim())) {
			return "노출 여부를 확인해주세요.";
		}
		if (isCreate && param.getRegNo() == null) {
			return "등록자 정보를 확인해주세요.";
		}
		if (!isCreate && param.getUdtNo() == null) {
			return "수정자 정보를 확인해주세요.";
		}

		// 등록/수정 일시를 정규화하고 유효성을 검증합니다.
		String dateValidation = normalizeAndValidateDisplayPeriod(param);
		if (dateValidation != null) {
			return dateValidation;
		}
		String tabDateValidation = normalizeAndValidateTabDisplayPeriod(param);
		return tabDateValidation;
	}

	// 기획전 등록 요청을 검증합니다.
	public String validateExhibitionCreate(ExhibitionSavePO param) {
		return validateExhibitionSave(param, true);
	}

	// 기획전 수정 요청을 검증합니다.
	public String validateExhibitionUpdate(ExhibitionSavePO param) {
		return validateExhibitionSave(param, false);
	}

	// 기획전 마스터 저장 요청을 검증합니다.
	public String validateExhibitionMasterSave(ExhibitionSavePO param) {
		boolean isCreateMode = param == null || param.getExhibitionNo() == null || param.getExhibitionNo() < 1;
		return validateExhibitionSave(param, isCreateMode);
	}

	// 기획전 탭 저장 요청을 검증합니다.
	public String validateExhibitionTabSave(ExhibitionSavePO param) {
		// 필수 요청값을 확인합니다.
		if (param == null) {
			return "요청 데이터가 없습니다.";
		}
		if (param.getExhibitionNo() == null || param.getExhibitionNo() < 1) {
			return "기획전 번호를 확인해주세요.";
		}
		if (param.getUdtNo() == null) {
			return "수정자 정보를 확인해주세요.";
		}
		// 기획전 존재 여부를 확인합니다.
		int exists = exhibitionMapper.countExhibitionByNo(param.getExhibitionNo());
		if (exists == 0) {
			return "기획전 정보를 확인해주세요.";
		}

		// 탭 노출 일시 형식을 정리하고 유효성을 검증합니다.
		String tabDateValidation = normalizeAndValidateTabDisplayPeriod(param);
		if (tabDateValidation != null) {
			return tabDateValidation;
		}

		// 탭 입력값을 검증합니다.
		List<ExhibitionTabPO> tabList = safeList(param.getTabList());
		for (int index = 0; index < tabList.size(); index += 1) {
			ExhibitionTabPO tab = tabList.get(index);
			if (tab == null) {
				continue;
			}
			String tabNm = trimToNull(tab.getTabNm());
			if (tabNm == null) {
				continue;
			}
			if (tabNm.length() > 50) {
				return "탭[" + (index + 1) + "] 이름은 50자 이내로 입력해주세요.";
			}
			String showYn = trimToNull(tab.getShowYn());
			if (showYn != null && !isYnValue(showYn)) {
				return "탭[" + (index + 1) + "] 노출여부를 확인해주세요.";
			}
		}
		return null;
	}

	// 기획전 탭 상품 저장 요청을 검증합니다.
	public String validateExhibitionGoodsSave(ExhibitionSavePO param) {
		// 필수 요청값을 확인합니다.
		if (param == null) {
			return "요청 데이터가 없습니다.";
		}
		if (param.getExhibitionNo() == null || param.getExhibitionNo() < 1) {
			return "기획전 번호를 확인해주세요.";
		}
		if (param.getUdtNo() == null) {
			return "수정자 정보를 확인해주세요.";
		}
		// 기획전 존재 여부를 확인합니다.
		int exists = exhibitionMapper.countExhibitionByNo(param.getExhibitionNo());
		if (exists == 0) {
			return "기획전 정보를 확인해주세요.";
		}

		// 기획전에 등록된 탭 번호 목록을 조회합니다.
		Set<Integer> validTabNoSet = exhibitionMapper.getExhibitionTabList(param.getExhibitionNo())
			.stream()
			.map(ExhibitionTabPO::getExhibitionTabNo)
			.filter(Objects::nonNull)
			.filter(tabNo -> tabNo > 0)
			.collect(Collectors.toSet());

		// 상품 입력값을 검증합니다.
		List<ExhibitionGoodsPO> goodsList = safeList(param.getGoodsList());
		for (int index = 0; index < goodsList.size(); index += 1) {
			ExhibitionGoodsPO goods = goodsList.get(index);
			if (goods == null) {
				continue;
			}
			String goodsId = trimToNull(goods.getGoodsId());
			if (goodsId == null) {
				continue;
			}
			Integer exhibitionTabNo = goods.getExhibitionTabNo();
			if (exhibitionTabNo == null || exhibitionTabNo < 1) {
				return "상품 저장 전에 탭 저장을 먼저 진행해주세요.";
			}
			if (!validTabNoSet.contains(exhibitionTabNo)) {
				return "상품[" + (index + 1) + "]의 탭 정보를 확인해주세요.";
			}
			String showYn = trimToNull(goods.getShowYn());
			if (showYn != null && !isYnValue(showYn)) {
				return "상품[" + (index + 1) + "] 노출여부를 확인해주세요.";
			}
		}
		return null;
	}

	// 기획전 삭제 요청을 검증합니다.
	public String validateExhibitionDelete(ExhibitionDeletePO param) {
		// 필수 요청값을 확인합니다.
		if (param == null) {
			return "요청 데이터가 없습니다.";
		}
		if (param.getExhibitionNo() == null || param.getExhibitionNo() < 1) {
			return "기획전 번호를 확인해주세요.";
		}
		if (param.getUdtNo() == null) {
			return "수정자 정보를 확인해주세요.";
		}
		// 존재 여부를 확인합니다.
		int exists = exhibitionMapper.countExhibitionByNo(param.getExhibitionNo());
		if (exists == 0) {
			return "기획전 정보를 확인해주세요.";
		}
		return null;
	}

	// 기획전 마스터 정보를 저장합니다.
	@Transactional
	public int saveExhibitionMaster(ExhibitionSavePO param) {
		// 신규/수정 모드를 식별합니다.
		boolean isCreateMode = param.getExhibitionNo() == null || param.getExhibitionNo() < 1;
		if (isCreateMode) {
			normalizeDefaultValues(param, true);
			exhibitionMapper.insertExhibitionBase(param);
			if (param.getExhibitionNo() == null || param.getExhibitionNo() < 1) {
				throw new IllegalArgumentException("기획전 등록에 실패했습니다.");
			}
			return param.getExhibitionNo();
		}

		// 수정 대상 존재 여부를 확인한 뒤 마스터 정보만 수정합니다.
		int exists = exhibitionMapper.countExhibitionByNo(param.getExhibitionNo());
		if (exists == 0) {
			throw new IllegalArgumentException("기획전 정보를 확인해주세요.");
		}
		normalizeDefaultValues(param, false);
		int updated = exhibitionMapper.updateExhibitionBase(param);
		if (updated == 0) {
			throw new IllegalArgumentException("기획전 수정에 실패했습니다.");
		}
		return param.getExhibitionNo();
	}

	// 기획전 탭 정보를 저장합니다.
	@Transactional
	public int saveExhibitionTabsOnly(ExhibitionSavePO param) {
		Integer exhibitionNo = param.getExhibitionNo();
		if (exhibitionNo == null || exhibitionNo < 1) {
			throw new IllegalArgumentException("기획전 번호를 확인해주세요.");
		}

		// 기존 탭 목록을 조회합니다.
		List<ExhibitionTabPO> existingTabs = exhibitionMapper.getExhibitionTabList(exhibitionNo);
		Map<Integer, ExhibitionTabPO> existingTabMap = existingTabs.stream()
			.filter(Objects::nonNull)
			.filter(item -> item.getExhibitionTabNo() != null && item.getExhibitionTabNo() > 0)
			.collect(Collectors.toMap(ExhibitionTabPO::getExhibitionTabNo, item -> item, (left, right) -> left));

		// 저장 대상 탭 목록을 정규화합니다.
		List<ExhibitionTabPO> sourceTabs = safeList(param.getTabList());
		List<ExhibitionTabPO> saveTabs = new ArrayList<>();
		Set<Integer> keepTabNoSet = new HashSet<>();
		for (ExhibitionTabPO source : sourceTabs) {
			if (source == null) {
				continue;
			}
			String tabNm = trimToNull(source.getTabNm());
			if (tabNm == null) {
				continue;
			}
			ExhibitionTabPO saveTarget = new ExhibitionTabPO();
			saveTarget.setExhibitionTabNo(source.getExhibitionTabNo());
			saveTarget.setExhibitionNo(exhibitionNo);
			saveTarget.setTabNm(tabNm);
			saveTarget.setDispStartDt(source.getDispStartDt());
			saveTarget.setDispEndDt(source.getDispEndDt());
			saveTarget.setShowYn(normalizeShowYn(source.getShowYn()));
			saveTarget.setRegNo(resolveAuditNo(param.getRegNo(), param.getUdtNo()));
			saveTarget.setUdtNo(resolveAuditNo(param.getRegNo(), param.getUdtNo()));
			saveTabs.add(saveTarget);
			if (saveTarget.getExhibitionTabNo() != null && saveTarget.getExhibitionTabNo() > 0) {
				keepTabNoSet.add(saveTarget.getExhibitionTabNo());
			}
		}

		// 삭제 대상 탭 번호를 계산합니다.
		List<Integer> deleteTabNoList = existingTabMap.keySet().stream()
			.filter(tabNo -> !keepTabNoSet.contains(tabNo))
			.collect(Collectors.toList());

		// 삭제 대상 탭에 연관 상품이 있으면 확인 플래그를 검증합니다.
		if (!deleteTabNoList.isEmpty()) {
			int linkedGoodsCount = exhibitionMapper.countExhibitionGoodsByTabNoList(deleteTabNoList);
			if (linkedGoodsCount > 0 && !Boolean.TRUE.equals(param.getForceDeleteGoodsWithTabs())) {
				throw new IllegalArgumentException("삭제할 탭에 등록된 상품이 있습니다. 계속하려면 다시 시도해주세요.");
			}
			if (linkedGoodsCount > 0) {
				exhibitionMapper.deleteExhibitionGoodsByTabNoList(deleteTabNoList);
			}
			for (Integer deleteTabNo : deleteTabNoList) {
				exhibitionMapper.deleteExhibitionTabByTabNo(deleteTabNo);
			}
		}

		// 탭을 등록/수정합니다.
		int savedCount = 0;
		for (ExhibitionTabPO saveTab : saveTabs) {
			Integer exhibitionTabNo = saveTab.getExhibitionTabNo();
			if (exhibitionTabNo != null && existingTabMap.containsKey(exhibitionTabNo)) {
				savedCount += exhibitionMapper.updateExhibitionTab(saveTab);
				continue;
			}
			exhibitionMapper.insertExhibitionTab(saveTab);
			savedCount += 1;
		}
		return savedCount;
	}

	// 기획전 탭 상품 정보를 저장합니다.
	@Transactional
	public int saveExhibitionGoodsOnly(ExhibitionSavePO param) {
		Integer exhibitionNo = param.getExhibitionNo();
		if (exhibitionNo == null || exhibitionNo < 1) {
			throw new IllegalArgumentException("기획전 번호를 확인해주세요.");
		}

		// 현재 기획전의 상품을 초기화한 뒤 전달된 목록을 다시 저장합니다.
		exhibitionMapper.deleteExhibitionGoodsByExhibitionNo(exhibitionNo);

		List<ExhibitionGoodsPO> sourceGoodsList = safeList(param.getGoodsList());
		Map<String, ExhibitionGoodsPO> dedupedGoodsMap = new LinkedHashMap<>();
		for (ExhibitionGoodsPO source : sourceGoodsList) {
			if (source == null) {
				continue;
			}
			String goodsId = trimToNull(source.getGoodsId());
			Integer exhibitionTabNo = source.getExhibitionTabNo();
			if (goodsId == null || exhibitionTabNo == null || exhibitionTabNo < 1) {
				continue;
			}
			String dedupeKey = exhibitionTabNo + "::" + goodsId;
			if (!dedupedGoodsMap.containsKey(dedupeKey)) {
				dedupedGoodsMap.put(dedupeKey, source);
			}
		}

		// 탭별 노출순서를 계산해 상품을 저장합니다.
		Map<Integer, Integer> tabOrderMap = new HashMap<>();
		int savedCount = 0;
		for (ExhibitionGoodsPO source : dedupedGoodsMap.values()) {
			Integer exhibitionTabNo = source.getExhibitionTabNo();
			if (exhibitionTabNo == null || exhibitionTabNo < 1) {
				continue;
			}
			int nextOrder = tabOrderMap.getOrDefault(exhibitionTabNo, 0) + 1;
			tabOrderMap.put(exhibitionTabNo, nextOrder);

			ExhibitionGoodsPO saveTarget = new ExhibitionGoodsPO();
			saveTarget.setExhibitionNo(exhibitionNo);
			saveTarget.setExhibitionTabNo(exhibitionTabNo);
			saveTarget.setGoodsId(trimToNull(source.getGoodsId()));
			saveTarget.setDispOrd(resolvePositiveInteger(source.getDispOrd(), nextOrder));
			saveTarget.setShowYn(normalizeShowYn(source.getShowYn()));
			saveTarget.setRegNo(resolveAuditNo(param.getRegNo(), param.getUdtNo()));
			saveTarget.setUdtNo(resolveAuditNo(param.getRegNo(), param.getUdtNo()));
			exhibitionMapper.insertExhibitionGoods(saveTarget);
			savedCount += 1;
		}
		return savedCount;
	}

	// 기획전 썸네일 업로드 요청을 검증합니다.
	public String validateExhibitionThumbnailUpload(Integer exhibitionNo, Long regNo, MultipartFile image) {
		if (exhibitionNo == null || exhibitionNo < 1) {
			return "기획전 번호를 확인해주세요.";
		}
		if (regNo == null) {
			return "등록자 정보를 확인해주세요.";
		}
		if (image == null || image.isEmpty()) {
			return "이미지를 선택해주세요.";
		}
		int exists = exhibitionMapper.countExhibitionByNo(exhibitionNo);
		if (exists == 0) {
			return "기획전 정보를 확인해주세요.";
		}
		long maxSizeInBytes = (long) ftpProperties.getUploadExhibitionMaxSize() * 1024 * 1024;
		if (image.getSize() > maxSizeInBytes) {
			return "파일 크기가 " + ftpProperties.getUploadExhibitionMaxSize() + "MB를 초과합니다.";
		}

		String originalFilename = image.getOriginalFilename();
		if (originalFilename == null) {
			return "파일명이 올바르지 않습니다.";
		}
		int lastDot = originalFilename.lastIndexOf(".");
		if (lastDot < 0 || lastDot == originalFilename.length() - 1) {
			return "이미지 확장자를 확인해주세요.";
		}
		String extension = originalFilename.substring(lastDot + 1).toLowerCase();
		if (isDisallowedImageExtension(ftpProperties.getUploadExhibitionAllowExtension(), extension)) {
			return "허용되지 않는 파일 형식입니다. 허용 형식: " + ftpProperties.getUploadExhibitionAllowExtension();
		}

		try {
			BufferedImage bufferedImage = ImageIO.read(image.getInputStream());
			if (bufferedImage == null) {
				return "이미지 파일을 확인해주세요.";
			}
			int width = bufferedImage.getWidth();
			int height = bufferedImage.getHeight();
			if (width != EXHIBITION_THUMBNAIL_WIDTH || height != EXHIBITION_THUMBNAIL_HEIGHT) {
				return "썸네일은 750x1024px만 가능합니다.";
			}
		} catch (IOException e) {
			return "이미지 파일을 확인해주세요.";
		}
		return null;
	}

	// 기획전 썸네일을 업로드하고 DB에 URL을 반영합니다.
	public String uploadExhibitionThumbnail(Integer exhibitionNo, Long regNo, MultipartFile image) throws IOException {
		String thumbnailUrl = ftpFileService.uploadExhibitionImage(image, String.valueOf(exhibitionNo), String.valueOf(regNo));
		int updated = exhibitionMapper.updateExhibitionThumbnail(exhibitionNo, thumbnailUrl);
		if (updated == 0) {
			throw new IllegalArgumentException("썸네일 저장에 실패했습니다.");
		}
		return thumbnailUrl;
	}

	// 기획전을 등록합니다.
	@Transactional
	public int createExhibition(ExhibitionSavePO param) {
		// 기본값을 정리한 뒤 등록합니다.
		normalizeDefaultValues(param, true);
		exhibitionMapper.insertExhibitionBase(param);
		if (param.getExhibitionNo() == null || param.getExhibitionNo() < 1) {
			throw new IllegalArgumentException("기획전 등록에 실패했습니다.");
		}
		saveExhibitionTabAndGoods(param.getExhibitionNo(), param);
		return param.getExhibitionNo();
	}

	// 기획전을 수정합니다.
	@Transactional
	public int updateExhibition(ExhibitionSavePO param) {
		// 수정 대상 존재 여부를 확인합니다.
		int exists = exhibitionMapper.countExhibitionByNo(param.getExhibitionNo());
		if (exists == 0) {
			throw new IllegalArgumentException("기획전 정보를 확인해주세요.");
		}

		// 기본값 및 일시를 정리한 뒤 갱신합니다.
		normalizeDefaultValues(param, false);
		int updated = exhibitionMapper.updateExhibitionBase(param);
		if (updated == 0) {
			throw new IllegalArgumentException("기획전 수정에 실패했습니다.");
		}
		saveExhibitionTabAndGoods(param.getExhibitionNo(), param);
		return updated;
	}

	// 기획전을 삭제 처리합니다.
	@Transactional
	public int deleteExhibition(ExhibitionDeletePO param) {
		// 하위 탭/상품을 모두 정리한 뒤 기획전 상태를 삭제로 변경합니다.
		exhibitionMapper.deleteExhibitionGoodsByExhibitionNo(param.getExhibitionNo());
		exhibitionMapper.deleteExhibitionTabByExhibitionNo(param.getExhibitionNo());
		return exhibitionMapper.updateExhibitionBaseDelete(param.getExhibitionNo(), param.getUdtNo());
	}

	// 기획전 탭/상품을 저장합니다.
	private void saveExhibitionTabAndGoods(Integer exhibitionNo, ExhibitionSavePO param) {
		if (exhibitionNo == null || exhibitionNo < 1) {
			return;
		}

		// 기획전 단위로 기존 정보를 초기화합니다.
		exhibitionMapper.deleteExhibitionGoodsByExhibitionNo(exhibitionNo);
		exhibitionMapper.deleteExhibitionTabByExhibitionNo(exhibitionNo);

		List<ExhibitionTabPO> tabList = safeList(param.getTabList());
		if (tabList.isEmpty()) {
			return;
		}

		// rowKey/기존 tabNo 매핑을 미리 준비합니다.
		Map<String, Integer> tabRowKeyToNo = new HashMap<>();
		Map<Integer, Integer> tabNoToNo = new HashMap<>();
		for (ExhibitionTabPO source : tabList) {
			if (source == null) {
				continue;
			}
			String tabNm = trimToNull(source.getTabNm());
			if (tabNm == null) {
				continue;
			}

			// 탭을 저장해 새 키를 받아옵니다.
			ExhibitionTabPO saveTarget = new ExhibitionTabPO();
			saveTarget.setExhibitionNo(exhibitionNo);
			saveTarget.setTabNm(tabNm);
			// 탭 노출 기간을 그대로 저장 대상으로 전달합니다.
			saveTarget.setDispStartDt(source.getDispStartDt());
			saveTarget.setDispEndDt(source.getDispEndDt());
			saveTarget.setShowYn(normalizeShowYn(source.getShowYn()));
			saveTarget.setDelYn(YN_N);
			saveTarget.setRegNo(resolveAuditNo(param.getRegNo(), param.getUdtNo()));
			saveTarget.setUdtNo(resolveAuditNo(param.getRegNo(), param.getUdtNo()));
			exhibitionMapper.insertExhibitionTab(saveTarget);
			if (saveTarget.getExhibitionTabNo() == null || saveTarget.getExhibitionTabNo() < 1) {
				throw new IllegalArgumentException("기획전 탭 등록에 실패했습니다.");
			}
			String sourceRowKey = trimToNull(source.getRowKey());
			if (sourceRowKey != null) {
				tabRowKeyToNo.put(sourceRowKey, saveTarget.getExhibitionTabNo());
			}
			Integer sourceTabNo = source.getExhibitionTabNo();
			if (sourceTabNo != null && sourceTabNo > 0) {
				tabNoToNo.put(sourceTabNo, saveTarget.getExhibitionTabNo());
			}
		}

		// 탭별 상품을 정렬 상태로 저장합니다.
		List<ExhibitionGoodsPO> goodsList = safeList(param.getGoodsList());
		Map<Integer, Integer> tabDispOrd = new HashMap<>();
		for (ExhibitionGoodsPO source : goodsList) {
			if (source == null) {
				continue;
			}
			String goodsId = trimToNull(source.getGoodsId());
			if (goodsId == null) {
				continue;
			}

			Integer targetTabNo = resolveGoodsTabNo(source, tabRowKeyToNo, tabNoToNo);
			if (targetTabNo == null || targetTabNo < 1) {
				throw new IllegalArgumentException("상품 탭을 선택해주세요.");
			}
			int nextOrder = tabDispOrd.getOrDefault(targetTabNo, 0) + 1;
			tabDispOrd.put(targetTabNo, nextOrder);

			ExhibitionGoodsPO saveTarget = new ExhibitionGoodsPO();
			saveTarget.setExhibitionNo(exhibitionNo);
			saveTarget.setExhibitionTabNo(targetTabNo);
			saveTarget.setGoodsId(goodsId);
			saveTarget.setDispOrd(resolvePositiveInteger(source.getDispOrd(), nextOrder));
			saveTarget.setShowYn(normalizeShowYn(source.getShowYn()));
			saveTarget.setDelYn(YN_N);
			saveTarget.setRegNo(resolveAuditNo(param.getRegNo(), param.getUdtNo()));
			saveTarget.setUdtNo(resolveAuditNo(param.getRegNo(), param.getUdtNo()));
			exhibitionMapper.insertExhibitionGoods(saveTarget);
		}
	}

	// 엑셀 업로드 요청을 검증합니다.
	public String validateExhibitionGoodsExcelUpload(MultipartFile file) {
		// 파일 존재을 확인합니다.
		if (file == null || file.isEmpty()) {
			return "엑셀 파일을 선택해주세요.";
		}
		String filename = file.getOriginalFilename();
		if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
			return "xlsx 파일만 업로드할 수 있습니다.";
		}
		return null;
	}

	// 엑셀 업로드 데이터를 파싱해 상품 목록으로 반환합니다.
	public List<Map<String, Object>> parseExhibitionGoodsExcel(MultipartFile file) throws IOException {
		List<Map<String, Object>> rows = new ArrayList<>();
		// 시트 정보를 읽어 헤더를 검증합니다.
		try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
			Sheet sheet = workbook.getSheetAt(0);
			if (sheet == null) {
				throw new IllegalArgumentException("엑셀 데이터를 확인해주세요.");
			}
			Row header = sheet.getRow(0);
			if (header == null
				|| !HEADER_GOODS_ID.equals(getCellString(header.getCell(0)))
				|| !HEADER_DISPLAY_ORDER.equals(getCellString(header.getCell(1)))) {
				throw new IllegalArgumentException("엑셀 헤더 형식을 확인해주세요.");
			}

			int lastRowNum = sheet.getLastRowNum();
			for (int rowIndex = 1; rowIndex <= lastRowNum; rowIndex += 1) {
				Row row = sheet.getRow(rowIndex);
				if (row == null) {
					continue;
				}
				String goodsId = trimToNull(getCellString(row.getCell(0)));
				Integer dispOrd = getCellInteger(row.getCell(1));
				if (goodsId == null) {
					continue;
				}
				if (dispOrd == null || dispOrd < 1) {
					throw new IllegalArgumentException("엑셀 업로드의 노출순서는 1 이상이어야 합니다.");
				}
				Map<String, Object> item = new HashMap<>();
				item.put("goodsId", goodsId);
				item.put("dispOrd", dispOrd);
				item.put("showYn", YN_Y);
				rows.add(item);
			}
		}
		return rows;
	}

	// 엑셀 다운로드용 상품 템플릿을 생성합니다.
	public byte[] buildExhibitionGoodsExcelTemplate(Integer exhibitionNo, Integer exhibitionTabNo) throws IOException {
		List<ExhibitionGoodsVO> rows = new ArrayList<>();
		if (exhibitionNo != null && exhibitionNo > 0) {
			rows = exhibitionMapper.getExhibitionGoodsList(exhibitionNo, exhibitionTabNo);
		}
		// 파일 헤더 및 상품 데이터를 생성합니다.
		try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			Sheet sheet = workbook.createSheet("template");
			Row header = sheet.createRow(0);
			header.createCell(0).setCellValue(HEADER_GOODS_ID);
			header.createCell(1).setCellValue(HEADER_DISPLAY_ORDER);
			for (int index = 0; index < rows.size(); index += 1) {
				ExhibitionGoodsVO item = rows.get(index);
				if (item == null || trimToNull(item.getGoodsId()) == null) {
					continue;
				}
				Row row = sheet.createRow(index + 1);
				row.createCell(0).setCellValue(item.getGoodsId());
				row.createCell(1).setCellValue(resolvePositiveInteger(item.getDispOrd(), index + 1));
			}
			workbook.write(outputStream);
			return outputStream.toByteArray();
		}
	}

	// 기획전 탭 수를 조회합니다.
	public int countExhibitionTabByNo(Integer exhibitionNo) {
		return exhibitionMapper.countExhibitionTabByNo(exhibitionNo);
	}

	// 쇼핑몰 기획전 마스터 노출 가능 여부를 확인합니다.
	private void assertShopExhibitionVisible(Integer exhibitionNo) {
		ShopExhibitionDetailVO detail = exhibitionMapper.getShopVisibleExhibitionDetail(exhibitionNo);
		if (detail == null) {
			throw new IllegalArgumentException(SHOP_EXHIBITION_NOT_FOUND_MESSAGE);
		}
	}

	// 쇼핑몰 기획전 탭 노출 가능 여부를 확인합니다.
	private void assertShopExhibitionTabVisible(Integer exhibitionNo, Integer exhibitionTabNo) {
		List<ShopExhibitionTabVO> tabList = exhibitionMapper.getShopVisibleExhibitionTabList(exhibitionNo);
		if (tabList == null || tabList.stream().noneMatch(item -> item != null && Objects.equals(item.getExhibitionTabNo(), exhibitionTabNo))) {
			throw new IllegalArgumentException(SHOP_EXHIBITION_TAB_NOT_FOUND_MESSAGE);
		}
	}

	// 쇼핑몰 기획전 본문에 노출할 기본 HTML을 계산합니다.
	private String resolveVisibleExhibitionHtml(String pcHtml, String mobileHtml) {
		String normalizedPcHtml = trimToNull(pcHtml);
		String normalizedMobileHtml = trimToNull(mobileHtml);
		return normalizedPcHtml != null ? normalizedPcHtml : normalizedMobileHtml;
	}

	// 상품 이미지 경로를 웹 접근 가능한 URL로 변환합니다.
	private String resolveGoodsImageUrl(String goodsId, String filePath) {
		if (isBlank(goodsId) || isBlank(filePath)) {
			return null;
		}
		if (filePath.startsWith("http://") || filePath.startsWith("https://")) {
			return filePath;
		}
		return ftpFileService.buildGoodsImageUrl(goodsId, filePath);
	}

	// 문자열 YN 값을 정규화합니다.
	private String normalizeShowYn(String value) {
		String normalized = trimToNull(value);
		return YN_N.equals(normalized) ? YN_N : YN_Y;
	}

	// YN 값을 검증합니다.
	private boolean isYnValue(String value) {
		return YN_Y.equals(value) || YN_N.equals(value);
	}

	// 조회 파라미터 기본값을 정리합니다.
	private String normalizeSearchGb(String searchGb, String searchValue) {
		if (searchValue == null || searchValue.trim().isEmpty()) {
			return null;
		}
		if (SEARCH_GB_NO.equals(searchGb) || SEARCH_GB_NAME.equals(searchGb)) {
			return searchGb;
		}
		return SEARCH_GB_NAME;
	}

	// 입력값 기본값을 정리하고 YN 값을 보정합니다.
	private void normalizeDefaultValues(ExhibitionSavePO param, boolean isCreate) {
		param.setExhibitionNm(trimToNull(param.getExhibitionNm()));
		param.setListShowYn(normalizeShowYn(param.getListShowYn()));
		param.setShowYn(normalizeShowYn(param.getShowYn()));
		param.setExhibitionPcDesc(trimToNull(param.getExhibitionPcDesc()));
		param.setExhibitionMoDesc(trimToNull(param.getExhibitionMoDesc()));
		if (!isCreate && param.getUdtNo() == null && param.getRegNo() != null) {
			param.setUdtNo(param.getRegNo());
		}
	}

	// 검색 기간을 정규화하고 유효성을 확인합니다.
	private String normalizeSearchDateRange(ExhibitionPO param, String rawSearchStartDt, String rawSearchEndDt) {
		String start = normalizeSearchDate(rawSearchStartDt, false);
		String end = normalizeSearchDate(rawSearchEndDt, true);
		param.setSearchStartDt(start);
		param.setSearchEndDt(end);

		if (start == null || end == null) {
			return null;
		}
		LocalDateTime startDt = parseDateTime(start);
		LocalDateTime endDt = parseDateTime(end);
		if (startDt == null || endDt == null) {
			return "검색 기간 형식을 확인해주세요.";
		}
		if (startDt.isAfter(endDt)) {
			return "검색 시작일시는 종료일시보다 늦을 수 없습니다.";
		}
		return null;
	}

	// 저장용 노출 기간을 정규화하고 유효성을 검증합니다.
	private String normalizeAndValidateDisplayPeriod(ExhibitionSavePO param) {
		String rawStart = trimToNull(param.getDispStartDt());
		String rawEnd = trimToNull(param.getDispEndDt());

		LocalDateTime startDateTime = null;
		LocalDateTime endDateTime = null;
		if (rawStart != null) {
			startDateTime = parseDateTimeForSave(rawStart, false);
			if (startDateTime == null) {
				return "노출 시작일시 형식을 확인해주세요.";
			}
			param.setDispStartDt(DISPLAY_PERIOD_FORMATTER.format(startDateTime));
		}
		if (rawEnd != null) {
			endDateTime = parseDateTimeForSave(rawEnd, true);
			if (endDateTime == null) {
				return "노출 종료일시 형식을 확인해주세요.";
			}
			param.setDispEndDt(DISPLAY_PERIOD_FORMATTER.format(endDateTime));
		}

		if (startDateTime != null && endDateTime != null && startDateTime.isAfter(endDateTime)) {
			return "노출 시작일시는 종료일시보다 늦을 수 없습니다.";
		}

		if (rawStart == null) {
			param.setDispStartDt(null);
		}
		if (rawEnd == null) {
			param.setDispEndDt(null);
		}
		return null;
	}

	// 탭 노출 기간을 정규화하고 유효성을 검증합니다.
	private String normalizeAndValidateTabDisplayPeriod(ExhibitionSavePO param) {
		List<ExhibitionTabPO> tabList = safeList(param.getTabList());
		for (int index = 0; index < tabList.size(); index += 1) {
			ExhibitionTabPO tab = tabList.get(index);
			if (tab == null || trimToNull(tab.getTabNm()) == null) {
				continue;
			}

			String rawStart = trimToNull(tab.getDispStartDt());
			String rawEnd = trimToNull(tab.getDispEndDt());
			LocalDateTime startDateTime = null;
			LocalDateTime endDateTime = null;

			if (rawStart != null) {
				startDateTime = parseDateTimeForSave(rawStart, false);
				if (startDateTime == null) {
					return "탭[" + (index + 1) + "] 노출 시작일시 형식을 확인해주세요.";
				}
				tab.setDispStartDt(DISPLAY_PERIOD_FORMATTER.format(startDateTime));
			}
			if (rawEnd != null) {
				endDateTime = parseDateTimeForSave(rawEnd, true);
				if (endDateTime == null) {
					return "탭[" + (index + 1) + "] 노출 종료일시 형식을 확인해주세요.";
				}
				tab.setDispEndDt(DISPLAY_PERIOD_FORMATTER.format(endDateTime));
			}

			if (startDateTime != null && endDateTime != null && startDateTime.isAfter(endDateTime)) {
				return "탭[" + (index + 1) + "] 노출 시작일시는 종료일시보다 늦을 수 없습니다.";
			}
			if (rawStart == null) {
				tab.setDispStartDt(null);
			}
			if (rawEnd == null) {
				tab.setDispEndDt(null);
			}
		}
		return null;
	}

	// 검색용 날짜를 정규화하고 범위를 확보합니다.
	private String normalizeSearchDate(String value, boolean isEnd) {
		String normalized = trimToNull(value);
		if (normalized == null) {
			return null;
		}
		LocalDate localDate = parseLocalDate(normalized);
		if (localDate != null) {
			if (isEnd) {
				return DISPLAY_PERIOD_FORMATTER.format(localDate.atTime(23, 59, 59));
			}
			return DISPLAY_PERIOD_FORMATTER.format(localDate.atStartOfDay());
		}

		if (normalized.length() == 16) {
			normalized = normalized + ":00";
		}
		LocalDateTime dateTime = parseDateTime(normalized);
		return dateTime == null ? null : DISPLAY_PERIOD_FORMATTER.format(dateTime);
	}

	// 저장용 날짜/시간을 정규화합니다.
	private LocalDateTime parseDateTimeForSave(String value, boolean isEnd) {
		String normalized = trimToNull(value);
		if (normalized == null) {
			return null;
		}
		normalized = normalized.replace("T", " ").trim();
		if (normalized.length() == 10) {
			LocalDate date = parseLocalDate(normalized);
			if (date == null) {
				return null;
			}
			return isEnd ? date.atTime(23, 59, 59) : date.atStartOfDay();
		}
		if (normalized.length() == 13 && normalized.charAt(10) == ' ') {
			LocalDate date = parseLocalDate(normalized.substring(0, 10));
			if (date == null) {
				return null;
			}
			Integer hour = parseHour(normalized.substring(11, 13));
			if (hour == null || hour < 0 || hour > 24) {
				return null;
			}
			return hour == 24 ? date.atTime(23, 59, 59) : date.atTime(hour, 0, 0);
		}
		return parseDateTime(normalized);
	}

	// 시간값을 정수로 추출합니다.
	private Integer parseHour(String value) {
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException ignore) {
			return null;
		}
	}

	// 입력값에서 로컬 날짜를 추출합니다.
	private LocalDate parseLocalDate(String value) {
		String normalized = value.trim();
		try {
			if (normalized.length() == 8) {
				return LocalDate.parse(normalized, BASIC_DATE_FORMATTER);
			}
			if (normalized.length() >= 10) {
				return LocalDate.parse(normalized.substring(0, 10), SEARCH_DATE_FORMATTER);
			}
		} catch (DateTimeParseException ignore) {
			return null;
		}
		return null;
	}

	// 문자열 날짜/시간을 파싱합니다.
	private LocalDateTime parseDateTime(String value) {
		try {
			String normalized = value.replace("T", " ").trim();
			if (normalized.length() == 16) {
				normalized = normalized + ":00";
			}
			if (normalized.length() == 19) {
				return LocalDateTime.parse(normalized, DISPLAY_PERIOD_FORMATTER);
			}
		} catch (DateTimeParseException ignore) {
			// 파싱 실패는 null 처리합니다.
		}
		return null;
	}

	// 음수/비어있는 정렬 값을 보정합니다.
	private int resolvePositiveInteger(Integer value, int fallback) {
		if (value == null || value < 1) {
			return fallback;
		}
		return value;
	}

	// 신규/수정자 번호를 정리합니다.
	private Long resolveAuditNo(Long regNo, Long udtNo) {
		return regNo != null ? regNo : udtNo;
	}

	// 상품 탭 번호를 rowKey 기반으로 매핑합니다.
	private Integer resolveGoodsTabNo(ExhibitionGoodsPO source, Map<String, Integer> tabRowKeyToNo, Map<Integer, Integer> tabNoToNo) {
		if (source == null) {
			return null;
		}
		if (source.getExhibitionTabNo() != null && source.getExhibitionTabNo() > 0) {
			Integer mapped = tabNoToNo.get(source.getExhibitionTabNo());
			if (mapped != null) {
				return mapped;
			}
		}
		String tabRowKey = trimToNull(source.getExhibitionTabRowKey());
		if (tabRowKey != null) {
			return tabRowKeyToNo.get(tabRowKey);
		}
		return null;
	}

	// 시트 셀 문자열을 반환합니다.
	private String getCellString(Cell cell) {
		if (cell == null) {
			return "";
		}
		try {
			return String.valueOf(cell.getStringCellValue()).trim();
		} catch (Exception ignored) {
			try {
				return String.valueOf((long) cell.getNumericCellValue());
			} catch (Exception ignored2) {
				return "";
			}
		}
	}

	// 시트 셀 정수를 반환합니다.
	private Integer getCellInteger(Cell cell) {
		if (cell == null) {
			return null;
		}
		try {
			return (int) cell.getNumericCellValue();
		} catch (Exception ignored) {
			String raw = trimToNull(getCellString(cell));
			if (raw == null) {
				return null;
			}
			try {
				return Integer.parseInt(raw);
			} catch (NumberFormatException ignored2) {
				return null;
			}
		}
	}

	// 허용 확장자 목록에 없는 이미지 확장자인지 확인합니다.
	private boolean isDisallowedImageExtension(String allowedExtensions, String extension) {
		if (allowedExtensions == null || allowedExtensions.trim().isEmpty()) {
			return true;
		}
		if (extension == null || extension.trim().isEmpty()) {
			return true;
		}
		String normalizedExtension = extension.trim().toLowerCase();
		String[] tokens = allowedExtensions.split(",");
		for (String token : tokens) {
			if (normalizedExtension.equals(token.trim().toLowerCase())) {
				return false;
			}
		}
		return true;
	}

	// 리스트가 null인 경우 빈 리스트로 변환합니다.
	private <T> List<T> safeList(List<T> source) {
		return source == null ? List.of() : source;
	}
}
