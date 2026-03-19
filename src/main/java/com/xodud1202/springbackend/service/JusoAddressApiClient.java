package com.xodud1202.springbackend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderAddressSearchCommonVO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderAddressSearchItemVO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderAddressSearchResponseVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
// 행안부 주소 검색 API 연동 기능을 제공합니다.
public class JusoAddressApiClient {
	private static final String JUSO_ADDRESS_SEARCH_URL = "https://business.juso.go.kr/addrlink/addrLinkApi.do";
	private static final int HTTP_TIMEOUT_SECONDS = 15;

	private final ObjectMapper objectMapper;
	private final String apiKey;
	private final HttpClient httpClient = HttpClient.newBuilder().build();

	// 생성자에서 주소 검색 API 키를 주입받습니다.
	public JusoAddressApiClient(
		ObjectMapper objectMapper,
		@Value("${juso.api-key:}") String apiKey
	) {
		this.objectMapper = objectMapper;
		this.apiKey = safeValue(apiKey);
	}

	// 도로명 주소 검색 API를 호출해 검색 결과를 반환합니다.
	public ShopOrderAddressSearchResponseVO searchRoadAddress(String keyword, int currentPage, int countPerPage) {
		// 필수 API 키와 검색어를 확인합니다.
		String normalizedKeyword = trimToNull(keyword);
		if (normalizedKeyword == null) {
			throw new IllegalArgumentException("주소 검색어를 입력해주세요.");
		}
		validateApiKey();

		try {
			// 주소 검색 GET 요청을 생성해 JSON 응답을 조회합니다.
			String requestUrl = buildSearchRequestUrl(normalizedKeyword, currentPage, countPerPage);
			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(requestUrl))
				.timeout(Duration.ofSeconds(HTTP_TIMEOUT_SECONDS))
				.header("Accept", "application/json")
				.GET()
				.build();
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
			if (response.statusCode() < 200 || response.statusCode() >= 300) {
				throw new IllegalStateException("주소 검색 API 응답 코드가 비정상입니다. status=" + response.statusCode());
			}

			// JSON 응답을 주문서 주소 검색 응답 형식으로 변환합니다.
			return parseSearchResponse(response.body());
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("주소 검색 API 호출이 중단되었습니다.", exception);
		} catch (IllegalArgumentException exception) {
			throw exception;
		} catch (Exception exception) {
			throw new IllegalStateException("주소 검색 API 호출에 실패했습니다.", exception);
		}
	}

	// 주소 검색 요청 URL을 생성합니다.
	private String buildSearchRequestUrl(String keyword, int currentPage, int countPerPage) {
		String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
		String encodedApiKey = URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
		return JUSO_ADDRESS_SEARCH_URL
			+ "?currentPage=" + currentPage
			+ "&countPerPage=" + countPerPage
			+ "&keyword=" + encodedKeyword
			+ "&confmKey=" + encodedApiKey
			+ "&resultType=json";
	}

	// 주소 검색 JSON 응답을 응답 객체로 파싱합니다.
	private ShopOrderAddressSearchResponseVO parseSearchResponse(String responseBody) throws Exception {
		JsonNode rootNode = objectMapper.readTree(safeValue(responseBody));
		JsonNode resultsNode = rootNode.path("results");
		JsonNode commonNode = resultsNode.path("common");
		JsonNode jusoNode = resultsNode.path("juso");

		// 공통 응답 정보와 주소 목록을 순서대로 조합합니다.
		ShopOrderAddressSearchResponseVO result = new ShopOrderAddressSearchResponseVO();
		result.setCommon(toSearchCommon(commonNode));
		result.setJusoList(toSearchItemList(jusoNode));
		return result;
	}

	// 공통 응답 노드를 응답 객체로 변환합니다.
	private ShopOrderAddressSearchCommonVO toSearchCommon(JsonNode commonNode) {
		ShopOrderAddressSearchCommonVO common = new ShopOrderAddressSearchCommonVO();
		common.setErrorCode(readText(commonNode, "errorCode"));
		common.setErrorMessage(readText(commonNode, "errorMessage"));
		common.setTotalCount(readInteger(commonNode, "totalCount"));
		common.setCurrentPage(readInteger(commonNode, "currentPage"));
		common.setCountPerPage(readInteger(commonNode, "countPerPage"));
		return common;
	}

	// 주소 검색 결과 배열 노드를 목록으로 변환합니다.
	private List<ShopOrderAddressSearchItemVO> toSearchItemList(JsonNode jusoNode) {
		List<ShopOrderAddressSearchItemVO> result = new ArrayList<>();
		if (jusoNode == null || !jusoNode.isArray()) {
			return result;
		}

		// 각 주소 노드를 화면용 결과 객체로 변환합니다.
		for (JsonNode itemNode : jusoNode) {
			if (itemNode == null || itemNode.isMissingNode()) {
				continue;
			}
			ShopOrderAddressSearchItemVO item = new ShopOrderAddressSearchItemVO();
			item.setRoadAddr(readText(itemNode, "roadAddr"));
			item.setRoadAddrPart1(readText(itemNode, "roadAddrPart1"));
			item.setRoadAddrPart2(readText(itemNode, "roadAddrPart2"));
			item.setJibunAddr(readText(itemNode, "jibunAddr"));
			item.setZipNo(readText(itemNode, "zipNo"));
			item.setAdmCd(readText(itemNode, "admCd"));
			item.setRnMgtSn(readText(itemNode, "rnMgtSn"));
			item.setBdMgtSn(readText(itemNode, "bdMgtSn"));
			result.add(item);
		}
		return result;
	}

	// JSON 노드에서 문자열 값을 조회합니다.
	private String readText(JsonNode node, String fieldName) {
		if (node == null || node.isMissingNode()) {
			return "";
		}
		JsonNode fieldNode = node.path(fieldName);
		return fieldNode.isMissingNode() || fieldNode.isNull() ? "" : safeValue(fieldNode.asText(""));
	}

	// JSON 노드에서 숫자 값을 안전하게 정수로 변환합니다.
	private Integer readInteger(JsonNode node, String fieldName) {
		String value = trimToNull(readText(node, fieldName));
		if (value == null) {
			return 0;
		}
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException exception) {
			return 0;
		}
	}

	// API 호출 전 필수 키 설정을 확인합니다.
	private void validateApiKey() {
		if (trimToNull(apiKey) == null) {
			throw new IllegalStateException("juso.api-key 설정이 필요합니다.");
		}
	}

	// 문자열을 trim 처리하고 비어 있으면 null을 반환합니다.
	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	// null 문자열을 빈 문자열로 변환합니다.
	private String safeValue(String value) {
		return value == null ? "" : value;
	}
}
