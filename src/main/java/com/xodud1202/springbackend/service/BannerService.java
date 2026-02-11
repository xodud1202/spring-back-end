package com.xodud1202.springbackend.service;

import com.xodud1202.springbackend.domain.admin.banner.BannerDetailVO;
import com.xodud1202.springbackend.domain.admin.banner.BannerGoodsOrderSavePO;
import com.xodud1202.springbackend.domain.admin.banner.BannerGoodsPO;
import com.xodud1202.springbackend.domain.admin.banner.BannerGoodsVO;
import com.xodud1202.springbackend.domain.admin.banner.BannerImageInfoPO;
import com.xodud1202.springbackend.domain.admin.banner.BannerPO;
import com.xodud1202.springbackend.domain.admin.banner.BannerSavePO;
import com.xodud1202.springbackend.domain.admin.banner.BannerTabPO;
import com.xodud1202.springbackend.domain.admin.banner.BannerVO;
import com.xodud1202.springbackend.mapper.BannerMapper;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
// 관리자 배너 관련 비즈니스 로직을 처리합니다.
public class BannerService {
	private static final String DIV_01 = "BANNER_DIV_01";
	private static final String DIV_02 = "BANNER_DIV_02";
	private static final String DIV_03 = "BANNER_DIV_03";
	private static final String DIV_04 = "BANNER_DIV_04";
	private static final DateTimeFormatter DISPLAY_PERIOD_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final BannerMapper bannerMapper;
	private final FtpFileService ftpFileService;

	// 관리자 배너 목록을 조회합니다.
	public Map<String, Object> getAdminBannerList(
		String bannerDivCd,
		String showYn,
		String searchValue,
		String searchStartDt,
		String searchEndDt,
		Integer page,
		Integer pageSize
	) {
		// 페이징 기본값을 계산합니다.
		int resolvedPage = page == null || page < 1 ? 1 : page;
		int resolvedPageSize = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 200);
		int offset = (resolvedPage - 1) * resolvedPageSize;

		// 조회 조건 객체를 구성합니다.
		BannerPO param = new BannerPO();
		param.setBannerDivCd(trimToNull(bannerDivCd));
		param.setShowYn(trimToNull(showYn));
		param.setSearchValue(trimToNull(searchValue));
		normalizeSearchDisplayPeriod(param, searchStartDt, searchEndDt);
		param.setPage(resolvedPage);
		param.setPageSize(resolvedPageSize);
		param.setOffset(offset);

		// 목록과 건수를 조회합니다.
		List<BannerVO> list = bannerMapper.getAdminBannerList(param);
		int totalCount = bannerMapper.getAdminBannerCount(param);

		Map<String, Object> result = new HashMap<>();
		result.put("list", list);
		result.put("totalCount", totalCount);
		result.put("page", resolvedPage);
		result.put("pageSize", resolvedPageSize);
		return result;
	}

	// 관리자 배너 상세를 조회합니다.
	public BannerDetailVO getAdminBannerDetail(Integer bannerNo) {
		// 상세 기본 정보를 조회합니다.
		BannerDetailVO detail = bannerMapper.getAdminBannerDetail(bannerNo);
		if (detail == null) {
			return null;
		}
		// 이미지 배너 정보를 조회해 단건으로 세팅합니다.
		List<BannerImageInfoPO> imageInfoList = bannerMapper.getBannerImageInfoList(bannerNo);
		if (imageInfoList != null && !imageInfoList.isEmpty()) {
			detail.setImageInfo(imageInfoList.get(0));
		}
		// 탭/상품 정보를 조회합니다.
		detail.setTabList(bannerMapper.getBannerTabList(bannerNo));
		List<BannerGoodsVO> goodsList = bannerMapper.getBannerGoodsList(bannerNo);
		applyGoodsImageUrls(goodsList);
		detail.setGoodsList(goodsList);
		return detail;
	}

	// 배너 등록 요청을 검증합니다.
	public String validateBannerCreate(BannerSavePO param, MultipartFile image) {
		// 공통 검증을 수행합니다.
		String commonValidation = validateBannerCommon(param, true);
		if (commonValidation != null) {
			return commonValidation;
		}
		// 신규 등록 시 분기별 검증을 수행합니다.
		return validateDivSpecific(param, image, true);
	}

	// 배너 수정 요청을 검증합니다.
	public String validateBannerUpdate(BannerSavePO param, MultipartFile image) {
		// 공통 검증을 수행합니다.
		String commonValidation = validateBannerCommon(param, false);
		if (commonValidation != null) {
			return commonValidation;
		}
		// 수정 시 분기별 검증을 수행합니다.
		return validateDivSpecific(param, image, false);
	}

	// 배너 상품 정렬 저장 요청을 검증합니다.
	public String validateBannerGoodsOrder(BannerGoodsOrderSavePO param) {
		// 요청 데이터 유효성을 확인합니다.
		if (param == null) {
			return "요청 데이터가 없습니다.";
		}
		if (param.getBannerNo() == null || param.getBannerNo() < 1) {
			return "배너 번호를 확인해주세요.";
		}
		if (param.getUdtNo() == null) {
			return "수정자 정보를 확인해주세요.";
		}
		if (param.getOrders() == null || param.getOrders().isEmpty()) {
			return "저장할 정렬 정보가 없습니다.";
		}
		for (BannerGoodsPO item : param.getOrders()) {
			if (item == null || isBlank(item.getGoodsId()) || item.getDispOrd() == null) {
				return "정렬 정보가 올바르지 않습니다.";
			}
		}
		return null;
	}

	// 배너 상품 엑셀 파싱 요청을 검증합니다.
	public String validateBannerGoodsExcelParse(String bannerDivCd, MultipartFile file) {
		// 요청 데이터 유효성을 확인합니다.
		if (!DIV_02.equals(bannerDivCd) && !DIV_04.equals(bannerDivCd)) {
			return "엑셀 업로드는 상품탭배너/상품리스트배너에서만 가능합니다.";
		}
		if (file == null || file.isEmpty()) {
			return "엑셀 파일을 선택해주세요.";
		}
		String fileName = file.getOriginalFilename();
		if (fileName == null || !fileName.toLowerCase().endsWith(".xlsx")) {
			return "xlsx 파일만 업로드할 수 있습니다.";
		}
		return null;
	}

	// 배너 상품 엑셀 템플릿 요청을 검증합니다.
	public String validateBannerGoodsExcelTemplate(String bannerDivCd) {
		// 배너 구분 유효성을 확인합니다.
		if (!DIV_02.equals(bannerDivCd) && !DIV_04.equals(bannerDivCd)) {
			return "엑셀 템플릿은 상품탭배너/상품리스트배너에서만 제공합니다.";
		}
		return null;
	}

	@Transactional
	// 관리자 배너를 등록합니다.
	public Integer createBanner(BannerSavePO param, MultipartFile image) throws IOException {
		// 수정자 번호 기본값을 등록자로 맞춥니다.
		if (param.getUdtNo() == null) {
			param.setUdtNo(param.getRegNo());
		}
		if (param.getDispOrd() == null) {
			param.setDispOrd(1);
		}
		if (isBlank(param.getShowYn())) {
			param.setShowYn("Y");
		}

		// 배너 기본 정보를 등록합니다.
		bannerMapper.insertBannerBase(param);
		Integer bannerNo = param.getBannerNo();
		if (bannerNo == null || bannerNo < 1) {
			throw new IllegalArgumentException("배너 등록에 실패했습니다.");
		}
		// 분기별 상세 데이터를 등록합니다.
		saveDivSpecificData(param, image, bannerNo, true);
		return bannerNo;
	}

	@Transactional
	// 관리자 배너를 수정합니다.
	public int updateBanner(BannerSavePO param, MultipartFile image) throws IOException {
		// 기본 정보 수정 전 배너 존재 여부를 확인합니다.
		if (bannerMapper.countBannerByNo(param.getBannerNo()) == 0) {
			throw new IllegalArgumentException("배너 정보를 확인해주세요.");
		}
		if (param.getDispOrd() == null) {
			param.setDispOrd(1);
		}
		if (isBlank(param.getShowYn())) {
			param.setShowYn("Y");
		}
		// 배너 기본 정보를 수정합니다.
		int updated = bannerMapper.updateBannerBase(param);
		// 분기별 상세 데이터를 갱신합니다.
		saveDivSpecificData(param, image, param.getBannerNo(), false);
		return updated;
	}

	@Transactional
	// 배너 상품 정렬 순서를 저장합니다.
	public int saveBannerGoodsOrder(BannerGoodsOrderSavePO param) {
		// 요청 데이터 기반으로 정렬 저장을 수행합니다.
		return bannerMapper.updateBannerGoodsOrder(param);
	}

	// 배너 상품 엑셀을 파싱합니다.
	public List<Map<String, Object>> parseBannerGoodsExcel(String bannerDivCd, MultipartFile file) throws IOException {
		List<Map<String, Object>> rows = new ArrayList<>();
		// 엑셀 데이터를 로드합니다.
		try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
			Sheet sheet = workbook.getSheetAt(0);
			if (sheet == null) {
				throw new IllegalArgumentException("엑셀 데이터를 확인해주세요.");
			}
			for (int i = 1; i <= sheet.getLastRowNum(); i += 1) {
				Row row = sheet.getRow(i);
				if (row == null) {
					continue;
				}
				Map<String, Object> item = new HashMap<>();
				String goodsId = getCellString(row, 0);
				String tabNm = DIV_02.equals(bannerDivCd) ? getCellString(row, 1) : "";
				int dispOrd = DIV_02.equals(bannerDivCd) ? getCellInt(row, 2) : getCellInt(row, 1);
				if (isBlank(goodsId)) {
					throw new IllegalArgumentException("엑셀 데이터의 상품코드를 확인해주세요.");
				}
				if (DIV_02.equals(bannerDivCd) && isBlank(tabNm)) {
					throw new IllegalArgumentException("상품탭배너 엑셀의 탭명을 확인해주세요.");
				}
				item.put("goodsId", goodsId);
				item.put("tabNm", tabNm);
				item.put("dispOrd", dispOrd < 1 ? 1 : dispOrd);
				item.put("showYn", "Y");
				rows.add(item);
			}
		}
		return rows;
	}

	// 배너 상품 엑셀 템플릿을 생성합니다.
	public byte[] buildBannerGoodsExcelTemplate(String bannerDivCd) throws IOException {
		// 엑셀 워크북을 생성합니다.
		try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			Sheet sheet = workbook.createSheet("template");
			Row header = sheet.createRow(0);
			header.createCell(0).setCellValue("GOODS_ID");
			if (DIV_02.equals(bannerDivCd)) {
				header.createCell(1).setCellValue("TAB_NM");
				header.createCell(2).setCellValue("DISP_ORD");
			} else {
				header.createCell(1).setCellValue("DISP_ORD");
			}
			workbook.write(outputStream);
			return outputStream.toByteArray();
		}
	}

	// 배너 공통 요청을 검증합니다.
	private String validateBannerCommon(BannerSavePO param, boolean isCreate) {
		if (param == null) {
			return "요청 데이터가 없습니다.";
		}
		if (!isCreate && (param.getBannerNo() == null || param.getBannerNo() < 1)) {
			return "배너 번호를 확인해주세요.";
		}
		if (isBlank(param.getBannerDivCd())) {
			return "배너 구분을 선택해주세요.";
		}
		if (isBlank(param.getShowYn())) {
			return "노출 여부를 선택해주세요.";
		}
		if (isCreate && param.getRegNo() == null) {
			return "등록자 정보를 확인해주세요.";
		}
		if (!isCreate && param.getUdtNo() == null) {
			return "수정자 정보를 확인해주세요.";
		}
		if (!DIV_01.equals(param.getBannerDivCd())
			&& !DIV_02.equals(param.getBannerDivCd())
			&& !DIV_03.equals(param.getBannerDivCd())
			&& !DIV_04.equals(param.getBannerDivCd())) {
			return "배너 구분이 올바르지 않습니다.";
		}
		// 노출기간 형식을 검증하고 표준 형식으로 정규화합니다.
		String displayPeriodValidation = normalizeAndValidateDisplayPeriod(param);
		if (displayPeriodValidation != null) {
			return displayPeriodValidation;
		}
		return null;
	}

	// 배너 구분별 요청을 검증합니다.
	private String validateDivSpecific(BannerSavePO param, MultipartFile image, boolean isCreate) {
		String bannerDivCd = param.getBannerDivCd();
		if (DIV_01.equals(bannerDivCd) || DIV_03.equals(bannerDivCd)) {
			// 이미지 배너 검증을 수행합니다.
			String imageValidation = validateImageBanner(param, image, isCreate);
			if (imageValidation != null) {
				return imageValidation;
			}
		}
		if (DIV_02.equals(bannerDivCd)) {
			// 탭 배너 검증을 수행합니다.
			if (param.getTabList() == null || param.getTabList().isEmpty()) {
				return "상품탭배너는 탭을 1개 이상 등록해주세요.";
			}
		}
		if (DIV_04.equals(bannerDivCd)) {
			// 상품리스트배너 검증을 수행합니다.
			if (param.getGoodsList() == null || param.getGoodsList().isEmpty()) {
				return "상품리스트배너는 상품을 1개 이상 등록해주세요.";
			}
		}
		return null;
	}

	// 이미지 배너 요청을 검증합니다.
	private String validateImageBanner(BannerSavePO param, MultipartFile image, boolean isCreate) {
		// 이미지 파일 유무를 확인합니다.
		if (isCreate && (image == null || image.isEmpty())) {
			return "이미지 파일을 선택해주세요.";
		}
		if (image != null && !image.isEmpty()) {
			try {
				BufferedImage bufferedImage = ImageIO.read(image.getInputStream());
				if (bufferedImage == null) {
					return "이미지 파일을 확인해주세요.";
				}
				int width = bufferedImage.getWidth();
				int height = bufferedImage.getHeight();
				if (DIV_01.equals(param.getBannerDivCd()) && (width != 1280 || height != 1280)) {
					return "대배너 이미지는 1280x1280만 가능합니다.";
				}
				if (DIV_03.equals(param.getBannerDivCd()) && (width != 1280 || height != 200)) {
					return "띠배너 이미지는 1280x200만 가능합니다.";
				}
			} catch (IOException e) {
				return "이미지 파일을 확인해주세요.";
			}
		}
		if (isCreate && (param.getImageInfo() == null) && (image == null || image.isEmpty())) {
			return "이미지 정보를 확인해주세요.";
		}
		return null;
	}

	// 배너 구분별 상세 데이터를 저장합니다.
	private void saveDivSpecificData(BannerSavePO param, MultipartFile image, Integer bannerNo, boolean isCreate) throws IOException {
		// 기존 상세 데이터를 정리합니다.
		bannerMapper.deleteImageBannerInfoByBannerNo(bannerNo);
		bannerMapper.deleteBannerGoodsByBannerNo(bannerNo);
		bannerMapper.deleteBannerTabByBannerNo(bannerNo);

		String bannerDivCd = param.getBannerDivCd();
		if (DIV_01.equals(bannerDivCd) || DIV_03.equals(bannerDivCd)) {
			// 이미지 배너 상세를 저장합니다.
			saveImageBannerInfo(param, image, bannerNo, isCreate);
			return;
		}
		if (DIV_02.equals(bannerDivCd)) {
			// 상품탭배너 상세를 저장합니다.
			saveTabBannerInfo(param, bannerNo);
			return;
		}
		if (DIV_04.equals(bannerDivCd)) {
			// 상품리스트배너 상세를 저장합니다.
			saveGoodsListBannerInfo(param, bannerNo);
		}
	}

	// 이미지 배너 상세를 저장합니다.
	private void saveImageBannerInfo(BannerSavePO param, MultipartFile image, Integer bannerNo, boolean isCreate) throws IOException {
		// 이미지 경로를 계산합니다.
		String imagePath = null;
		if (image != null && !image.isEmpty()) {
			imagePath = ftpFileService.uploadBannerImage(image, bannerNo, String.valueOf(isCreate ? param.getRegNo() : param.getUdtNo()));
		} else if (param.getImageInfo() != null) {
			imagePath = trimToNull(param.getImageInfo().getImgPath());
		}
		if (isBlank(imagePath)) {
			throw new IllegalArgumentException("이미지 파일을 선택해주세요.");
		}

		BannerImageInfoPO imageInfo = new BannerImageInfoPO();
		imageInfo.setBannerNo(bannerNo);
		imageInfo.setImgPath(imagePath);
		imageInfo.setUrl(param.getImageInfo() == null ? null : param.getImageInfo().getUrl());
		imageInfo.setBannerOpenCd(param.getImageInfo() == null ? null : param.getImageInfo().getBannerOpenCd());
		imageInfo.setDispOrd(param.getImageInfo() == null || param.getImageInfo().getDispOrd() == null ? 1 : param.getImageInfo().getDispOrd());
		imageInfo.setRegNo(param.getRegNo());
		imageInfo.setUdtNo(param.getUdtNo());
		bannerMapper.insertImageBannerInfo(imageInfo);
	}

	// 상품탭배너 상세를 저장합니다.
	private void saveTabBannerInfo(BannerSavePO param, Integer bannerNo) {
		List<BannerTabPO> tabList = param.getTabList() == null ? List.of() : param.getTabList();
		for (int tabIndex = 0; tabIndex < tabList.size(); tabIndex += 1) {
			BannerTabPO tab = tabList.get(tabIndex);
			if (tab == null || isBlank(tab.getTabNm())) {
				continue;
			}
			// 탭 정보를 등록합니다.
			tab.setBannerNo(bannerNo);
			tab.setDispOrd(tab.getDispOrd() == null ? tabIndex + 1 : tab.getDispOrd());
			tab.setShowYn(isBlank(tab.getShowYn()) ? "Y" : tab.getShowYn());
			tab.setDelYn("N");
			tab.setRegNo(param.getRegNo());
			tab.setUdtNo(param.getUdtNo());
			bannerMapper.insertBannerTab(tab);

			// 탭별 상품 정보를 등록합니다.
			if (tab.getBannerTabNo() == null || tab.getBannerTabNo() < 1) {
				continue;
			}
			List<BannerGoodsPO> sourceGoods = param.getGoodsList() == null ? List.of() : param.getGoodsList();
			int dispOrd = 1;
			for (BannerGoodsPO goods : sourceGoods) {
				if (goods == null || isBlank(goods.getGoodsId())) {
					continue;
				}
				if (!tab.getTabNm().equals(goods.getTabNm())) {
					continue;
				}
				BannerGoodsPO saveGoods = new BannerGoodsPO();
				saveGoods.setBannerNo(bannerNo);
				saveGoods.setBannerTabNo(tab.getBannerTabNo());
				saveGoods.setGoodsId(goods.getGoodsId());
				saveGoods.setDispOrd(goods.getDispOrd() == null ? dispOrd : goods.getDispOrd());
				saveGoods.setShowYn(isBlank(goods.getShowYn()) ? "Y" : goods.getShowYn());
				saveGoods.setRegNo(param.getRegNo());
				saveGoods.setUdtNo(param.getUdtNo());
				bannerMapper.insertBannerGoods(saveGoods);
				dispOrd += 1;
			}
		}
	}

	// 상품리스트배너 상세를 저장합니다.
	private void saveGoodsListBannerInfo(BannerSavePO param, Integer bannerNo) {
		List<BannerGoodsPO> goodsList = param.getGoodsList() == null ? List.of() : param.getGoodsList();
		for (int index = 0; index < goodsList.size(); index += 1) {
			BannerGoodsPO goods = goodsList.get(index);
			if (goods == null || isBlank(goods.getGoodsId())) {
				continue;
			}
			// 탭 미사용 배너 상품 정보를 등록합니다.
			BannerGoodsPO saveGoods = new BannerGoodsPO();
			saveGoods.setBannerNo(bannerNo);
			saveGoods.setBannerTabNo(0);
			saveGoods.setGoodsId(goods.getGoodsId());
			saveGoods.setDispOrd(goods.getDispOrd() == null ? index + 1 : goods.getDispOrd());
			saveGoods.setShowYn(isBlank(goods.getShowYn()) ? "Y" : goods.getShowYn());
			saveGoods.setRegNo(param.getRegNo());
			saveGoods.setUdtNo(param.getUdtNo());
			bannerMapper.insertBannerGoods(saveGoods);
		}
	}

	// 배너 상품 이미지 URL을 적용합니다.
	private void applyGoodsImageUrls(List<BannerGoodsVO> goodsList) {
		if (goodsList == null || goodsList.isEmpty()) {
			return;
		}
		for (BannerGoodsVO item : goodsList) {
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

	// 문자열 공백 여부를 확인합니다.
	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

	// 문자열을 trim 처리하고 빈값이면 null로 반환합니다.
	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	// 노출기간을 검증하고 DB 저장 형식으로 정규화합니다.
	private String normalizeAndValidateDisplayPeriod(BannerSavePO param) {
		String rawStart = trimToNull(param.getDispStartDt());
		String rawEnd = trimToNull(param.getDispEndDt());

		LocalDateTime startDateTime = null;
		LocalDateTime endDateTime = null;

		if (rawStart != null) {
			startDateTime = parseDateTime(rawStart);
			if (startDateTime == null) {
				return "노출 시작일시 형식을 확인해주세요.";
			}
			param.setDispStartDt(DISPLAY_PERIOD_FORMATTER.format(startDateTime));
		} else {
			param.setDispStartDt(null);
		}

		if (rawEnd != null) {
			endDateTime = parseDateTime(rawEnd);
			if (endDateTime == null) {
				return "노출 종료일시 형식을 확인해주세요.";
			}
			param.setDispEndDt(DISPLAY_PERIOD_FORMATTER.format(endDateTime));
		} else {
			param.setDispEndDt(null);
		}

		if (startDateTime != null && endDateTime != null && startDateTime.isAfter(endDateTime)) {
			return "노출 시작일시는 종료일시보다 늦을 수 없습니다.";
		}
		return null;
	}

	// 노출기간 문자열을 LocalDateTime으로 변환합니다.
	private LocalDateTime parseDateTime(String value) {
		String normalized = value.trim().replace("T", " ");
		try {
			if (normalized.length() == 16) {
				normalized = normalized + ":00";
			}
			return LocalDateTime.parse(normalized, DISPLAY_PERIOD_FORMATTER);
		} catch (DateTimeParseException e) {
			return null;
		}
	}

	// 목록 검색용 노출기간을 검증하고 DB 비교 형식으로 정규화합니다.
	private void normalizeSearchDisplayPeriod(BannerPO param, String searchStartDt, String searchEndDt) {
		String rawStart = trimToNull(searchStartDt);
		String rawEnd = trimToNull(searchEndDt);
		if (rawStart != null) {
			param.setSearchStartDt(toStartOfDayDateTime(rawStart));
		}
		if (rawEnd != null) {
			param.setSearchEndDt(toEndOfDayDateTime(rawEnd));
		}
	}

	// 날짜/일시 문자열을 시작시각(00:00:00) 기준 일시 문자열로 변환합니다.
	private String toStartOfDayDateTime(String value) {
		String normalized = value.trim().replace("T", " ");
		if (normalized.length() == 10) {
			return normalized + " 00:00:00";
		}
		if (normalized.length() == 16) {
			return normalized + ":00";
		}
		if (normalized.length() == 19) {
			return normalized;
		}
		return normalized;
	}

	// 날짜/일시 문자열을 종료시각(23:59:59) 기준 일시 문자열로 변환합니다.
	private String toEndOfDayDateTime(String value) {
		String normalized = value.trim().replace("T", " ");
		if (normalized.length() == 10) {
			return normalized + " 23:59:59";
		}
		if (normalized.length() == 16) {
			return normalized + ":59";
		}
		if (normalized.length() == 19) {
			return normalized;
		}
		return normalized;
	}

	// 셀 문자열 값을 반환합니다.
	private String getCellString(Row row, int cellIndex) {
		if (row == null || row.getCell(cellIndex) == null) {
			return "";
		}
		try {
			String value = row.getCell(cellIndex).getStringCellValue();
			return value == null ? "" : value.trim();
		} catch (Exception e) {
			try {
				return String.valueOf((long) row.getCell(cellIndex).getNumericCellValue());
			} catch (Exception ignored) {
				return "";
			}
		}
	}

	// 셀 정수 값을 반환합니다.
	private int getCellInt(Row row, int cellIndex) {
		if (row == null || row.getCell(cellIndex) == null) {
			return 1;
		}
		try {
			return (int) row.getCell(cellIndex).getNumericCellValue();
		} catch (Exception e) {
			String value = getCellString(row, cellIndex);
			if (isBlank(value)) {
				return 1;
			}
			try {
				return Integer.parseInt(value);
			} catch (NumberFormatException ignored) {
				return 1;
			}
		}
	}
}
