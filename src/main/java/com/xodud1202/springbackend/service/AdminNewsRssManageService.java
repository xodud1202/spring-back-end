package com.xodud1202.springbackend.service;

import com.xodud1202.springbackend.domain.admin.news.AdminNewsCategoryDeletePO;
import com.xodud1202.springbackend.domain.admin.news.AdminNewsCategoryRowVO;
import com.xodud1202.springbackend.domain.admin.news.AdminNewsCategorySavePO;
import com.xodud1202.springbackend.domain.admin.news.AdminNewsCategorySaveRowPO;
import com.xodud1202.springbackend.domain.admin.news.AdminNewsPressDeletePO;
import com.xodud1202.springbackend.domain.admin.news.AdminNewsPressRowVO;
import com.xodud1202.springbackend.domain.admin.news.AdminNewsPressSavePO;
import com.xodud1202.springbackend.domain.admin.news.AdminNewsPressSaveRowPO;
import com.xodud1202.springbackend.mapper.AdminNewsRssManageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
// 관리자 뉴스 RSS 관리 비즈니스 로직을 제공합니다.
public class AdminNewsRssManageService {
	private final AdminNewsRssManageMapper adminNewsRssManageMapper;

	// 관리자 뉴스 언론사 목록을 조회합니다.
	public List<AdminNewsPressRowVO> getAdminNewsPressList() {
		return adminNewsRssManageMapper.getAdminNewsPressList();
	}

	// 관리자 뉴스 카테고리 목록을 조회합니다.
	public List<AdminNewsCategoryRowVO> getAdminNewsCategoryListByPressNo(Long pressNo) {
		return adminNewsRssManageMapper.getAdminNewsCategoryListByPressNo(pressNo);
	}

	// 관리자 뉴스 언론사 저장 요청을 검증합니다.
	public String validateAdminNewsPressSave(AdminNewsPressSavePO param) {
		// 필수 파라미터를 검증합니다.
		if (param == null) {
			return "요청 데이터가 없습니다.";
		}
		if (param.getRows() == null || param.getRows().isEmpty()) {
			return "저장할 언론사 데이터가 없습니다.";
		}
		if (param.getRegNo() == null) {
			return "등록자 정보를 확인해주세요.";
		}
		if (param.getUdtNo() == null) {
			return "수정자 정보를 확인해주세요.";
		}
		// 행 단위 입력값을 검증합니다.
		for (AdminNewsPressSaveRowPO row : param.getRows()) {
			if (row == null) {
				return "언론사 데이터 형식이 올바르지 않습니다.";
			}
			String pressNm = trimToNull(row.getPressNm());
			if (pressNm == null) {
				return "언론사명을 입력해주세요.";
			}
			if (pressNm.length() > 100) {
				return "언론사명은 100자 이내로 입력해주세요.";
			}
			String useYn = trimToNull(row.getUseYn());
			if (!"Y".equals(useYn) && !"N".equals(useYn)) {
				return "사용여부를 확인해주세요.";
			}
		}
		return null;
	}

	// 관리자 뉴스 카테고리 저장 요청을 검증합니다.
	public String validateAdminNewsCategorySave(AdminNewsCategorySavePO param) {
		// 필수 파라미터를 검증합니다.
		if (param == null) {
			return "요청 데이터가 없습니다.";
		}
		if (param.getPressNo() == null) {
			return "언론사를 선택해주세요.";
		}
		if (param.getRows() == null || param.getRows().isEmpty()) {
			return "저장할 카테고리 데이터가 없습니다.";
		}
		if (param.getRegNo() == null) {
			return "등록자 정보를 확인해주세요.";
		}
		if (param.getUdtNo() == null) {
			return "수정자 정보를 확인해주세요.";
		}
		// 행 단위 입력값을 검증합니다.
		for (AdminNewsCategorySaveRowPO row : param.getRows()) {
			if (row == null) {
				return "카테고리 데이터 형식이 올바르지 않습니다.";
			}
			String categoryCd = trimToNull(row.getCategoryCd());
			String categoryNm = trimToNull(row.getCategoryNm());
			String useYn = trimToNull(row.getUseYn());
			String sourceNm = trimToNull(row.getSourceNm());
			String rssUrl = trimToNull(row.getRssUrl());
			if (categoryCd == null) {
				return "카테고리코드를 입력해주세요.";
			}
			if (categoryCd.length() > 50) {
				return "카테고리코드는 50자 이내로 입력해주세요.";
			}
			if (categoryNm == null) {
				return "카테고리명을 입력해주세요.";
			}
			if (categoryNm.length() > 100) {
				return "카테고리명은 100자 이내로 입력해주세요.";
			}
			if (!"Y".equals(useYn) && !"N".equals(useYn)) {
				return "사용여부를 확인해주세요.";
			}
			if (sourceNm == null) {
				return "소스명을 입력해주세요.";
			}
			if (sourceNm.length() > 150) {
				return "소스명은 150자 이내로 입력해주세요.";
			}
			if (rssUrl == null) {
				return "RSS URL을 입력해주세요.";
			}
			if (rssUrl.length() > 150) {
				return "RSS URL은 150자 이내로 입력해주세요.";
			}
		}
		return null;
	}

	// 관리자 뉴스 언론사 삭제 요청을 검증합니다.
	public String validateAdminNewsPressDelete(AdminNewsPressDeletePO param) {
		// 필수 파라미터를 검증합니다.
		if (param == null) {
			return "요청 데이터가 없습니다.";
		}
		if (param.getPressNoList() == null || param.getPressNoList().isEmpty()) {
			return "삭제할 언론사를 선택해주세요.";
		}
		if (param.getUdtNo() == null) {
			return "수정자 정보를 확인해주세요.";
		}
		return null;
	}

	// 관리자 뉴스 카테고리 삭제 요청을 검증합니다.
	public String validateAdminNewsCategoryDelete(AdminNewsCategoryDeletePO param) {
		// 필수 파라미터를 검증합니다.
		if (param == null) {
			return "요청 데이터가 없습니다.";
		}
		if (param.getPressNo() == null) {
			return "언론사를 선택해주세요.";
		}
		if (param.getCategoryCdList() == null || param.getCategoryCdList().isEmpty()) {
			return "삭제할 카테고리를 선택해주세요.";
		}
		if (param.getUdtNo() == null) {
			return "수정자 정보를 확인해주세요.";
		}
		return null;
	}

	@Transactional
	// 관리자 뉴스 언론사를 저장합니다.
	public int saveAdminNewsPress(AdminNewsPressSavePO param) {
		int affectedCount = 0;
		int sortSeq = 1;

		// 목록 순서를 기준으로 등록/수정과 정렬 순서 저장을 수행합니다.
		for (AdminNewsPressSaveRowPO row : param.getRows()) {
			String pressNm = trimToNull(row.getPressNm());
			String useYn = trimToNull(row.getUseYn());

			if (row.getPressNo() == null) {
				// 신규 언론사 등록 파라미터를 구성합니다.
				row.setPressCd(buildAutoPressCd(sortSeq));
				row.setPressNm(pressNm);
				row.setUseYn(useYn);
				row.setSortSeq(sortSeq);
				row.setRegNo(param.getRegNo());
				row.setUdtNo(param.getUdtNo());
				affectedCount += adminNewsRssManageMapper.insertAdminNewsPress(row);
			} else {
				// 기존 언론사 수정 파라미터를 구성합니다.
				row.setPressNm(pressNm);
				row.setUseYn(useYn);
				row.setUdtNo(param.getUdtNo());
				affectedCount += adminNewsRssManageMapper.updateAdminNewsPress(row);
			}

			// 최종 정렬 순서를 저장합니다.
			affectedCount += adminNewsRssManageMapper.updateAdminNewsPressSortSeq(row.getPressNo(), sortSeq, param.getUdtNo());
			sortSeq += 1;
		}
		return affectedCount;
	}

	@Transactional
	// 관리자 뉴스 카테고리를 저장합니다.
	public int saveAdminNewsCategory(AdminNewsCategorySavePO param) {
		int affectedCount = 0;
		int sortSeq = 1;

		// 목록 순서를 기준으로 등록/수정과 정렬 순서 저장을 수행합니다.
		for (AdminNewsCategorySaveRowPO row : param.getRows()) {
			String categoryCd = trimToNull(row.getCategoryCd());
			String categoryNm = trimToNull(row.getCategoryNm());
			String useYn = trimToNull(row.getUseYn());
			String sourceNm = trimToNull(row.getSourceNm());
			String rssUrl = trimToNull(row.getRssUrl());

			row.setPressNo(param.getPressNo());
			row.setCategoryCd(categoryCd);
			row.setCategoryNm(categoryNm);
			row.setUseYn(useYn);
			row.setSourceNm(sourceNm);
			row.setRssUrl(rssUrl);
			row.setSortSeq(sortSeq);
			row.setRegNo(param.getRegNo());
			row.setUdtNo(param.getUdtNo());

			// 존재 여부를 키 값 기준으로 판단해 등록/수정을 분기합니다.
			if (isCreateCategoryRow(row)) {
				affectedCount += adminNewsRssManageMapper.insertAdminNewsCategory(row);
			} else {
				affectedCount += adminNewsRssManageMapper.updateAdminNewsCategory(row);
			}

			// 최종 정렬 순서를 저장합니다.
			affectedCount += adminNewsRssManageMapper.updateAdminNewsCategorySortSeq(
				row.getPressNo(),
				row.getCategoryCd(),
				sortSeq,
				param.getUdtNo()
			);
			sortSeq += 1;
		}
		return affectedCount;
	}

	@Transactional
	// 관리자 뉴스 언론사를 삭제합니다.
	public int deleteAdminNewsPress(AdminNewsPressDeletePO param) {
		int affectedCount = 0;
		// 선택된 언론사별 연관 기사/카테고리/언론사 순으로 삭제합니다.
		for (Long pressNo : param.getPressNoList()) {
			affectedCount += adminNewsRssManageMapper.deleteAdminNewsArticleByPressNo(pressNo);
			affectedCount += adminNewsRssManageMapper.deleteAdminNewsCategoryByPressNo(pressNo);
			affectedCount += adminNewsRssManageMapper.deleteAdminNewsPressByPressNo(pressNo);
		}
		return affectedCount;
	}

	@Transactional
	// 관리자 뉴스 카테고리를 삭제합니다.
	public int deleteAdminNewsCategory(AdminNewsCategoryDeletePO param) {
		int affectedCount = 0;
		// 선택된 카테고리별 연관 기사/카테고리 순으로 삭제합니다.
		for (String categoryCd : param.getCategoryCdList()) {
			String resolvedCategoryCd = trimToNull(categoryCd);
			if (resolvedCategoryCd == null) {
				continue;
			}
			affectedCount += adminNewsRssManageMapper.deleteAdminNewsArticleByPressNoAndCategoryCd(param.getPressNo(), resolvedCategoryCd);
			affectedCount += adminNewsRssManageMapper.deleteAdminNewsCategoryByPressNoAndCategoryCd(param.getPressNo(), resolvedCategoryCd);
		}
		return affectedCount;
	}

	// 카테고리 신규 등록 여부를 판단합니다.
	private boolean isCreateCategoryRow(AdminNewsCategorySaveRowPO row) {
		List<AdminNewsCategoryRowVO> currentList = adminNewsRssManageMapper.getAdminNewsCategoryListByPressNo(row.getPressNo());
		for (AdminNewsCategoryRowVO current : currentList) {
			if (current.getCategoryCd() != null && current.getCategoryCd().equals(row.getCategoryCd())) {
				return false;
			}
		}
		return true;
	}

	// 자동 생성 언론사 코드를 반환합니다.
	private String buildAutoPressCd(int sortSeq) {
		String timeToken = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
		return "AUTO_PRESS_" + timeToken + "_" + sortSeq;
	}

	// 문자열 공백을 제거하고 빈값은 null로 변환합니다.
	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
