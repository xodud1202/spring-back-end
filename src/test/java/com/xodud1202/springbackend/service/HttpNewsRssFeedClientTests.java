package com.xodud1202.springbackend.service;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.xodud1202.springbackend.domain.news.RssArticleItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// HTTP RSS 클라이언트 인코딩/파싱 동작을 검증하는 테스트입니다.
class HttpNewsRssFeedClientTests {
	@Test
	@DisplayName("EUC-KR 선언 RSS는 한글 제목을 정상 파싱한다")
	// Content-Type charset이 없어도 XML 선언 인코딩(EUC-KR)으로 한글을 정상 파싱하는지 확인합니다.
	void fetchArticleItems_parsesEucKrXmlWithKoreanText() throws Exception {
		// EUC-KR XML 원문을 테스트 서버로 응답하도록 구성합니다.
		String xml = "<?xml version=\"1.0\" encoding=\"EUC-KR\" ?>"
			+ "<rss version=\"2.0\"><channel><item>"
			+ "<title>국민일보 제목</title>"
			+ "<link>https://example.com/politics/1</link>"
			+ "<description>요약</description>"
			+ "<pubDate>Fri, 06 Mar 2026 10:00:00 +0900</pubDate>"
			+ "</item></channel></rss>";
		byte[] bodyBytes = xml.getBytes(Charset.forName("EUC-KR"));
		HttpServer server = createServer(bodyBytes, "text/xml");

		try {
			// RSS 클라이언트로 조회해 한글 제목/링크가 정상 파싱되는지 검증합니다.
			HttpNewsRssFeedClient client = new HttpNewsRssFeedClient();
			String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/rss";
			List<RssArticleItem> items = client.fetchArticleItems(url);

			assertThat(items).hasSize(1);
			assertThat(items.get(0).title()).isEqualTo("국민일보 제목");
			assertThat(items.get(0).link()).isEqualTo("https://example.com/politics/1");
		} finally {
			server.stop(0);
		}
	}

	@Test
	@DisplayName("UTF-8 RSS는 기존과 동일하게 정상 파싱한다")
	// UTF-8 피드도 회귀 없이 기사 항목이 정상 파싱되는지 확인합니다.
	void fetchArticleItems_parsesUtf8XmlNormally() throws Exception {
		// UTF-8 XML 원문을 테스트 서버로 응답하도록 구성합니다.
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
			+ "<rss version=\"2.0\"><channel><item>"
			+ "<title>이데일리 뉴스</title>"
			+ "<link>https://example.com/economy/1</link>"
			+ "<description>설명</description>"
			+ "<pubDate>Fri, 06 Mar 2026 10:00:00 +0900</pubDate>"
			+ "</item></channel></rss>";
		byte[] bodyBytes = xml.getBytes(StandardCharsets.UTF_8);
		HttpServer server = createServer(bodyBytes, "text/xml");

		try {
			// RSS 클라이언트로 조회해 UTF-8 기사 파싱 결과를 검증합니다.
			HttpNewsRssFeedClient client = new HttpNewsRssFeedClient();
			String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/rss";
			List<RssArticleItem> items = client.fetchArticleItems(url);

			assertThat(items).hasSize(1);
			assertThat(items.get(0).title()).isEqualTo("이데일리 뉴스");
			assertThat(items.get(0).link()).isEqualTo("https://example.com/economy/1");
		} finally {
			server.stop(0);
		}
	}

	// 테스트용 RSS HTTP 서버를 생성합니다.
	private HttpServer createServer(byte[] bodyBytes, String contentType) throws Exception {
		// 로컬 임시 포트로 서버를 띄우고 고정 응답 핸들러를 등록합니다.
		HttpServer server = HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/rss", new FixedResponseHandler(bodyBytes, contentType));
		server.start();
		return server;
	}

	// 고정 바이트 RSS 응답을 반환하는 핸들러입니다.
	private static class FixedResponseHandler implements HttpHandler {
		private final byte[] bodyBytes;
		private final String contentType;

		// 핸들러 응답 본문/Content-Type을 초기화합니다.
		private FixedResponseHandler(byte[] bodyBytes, String contentType) {
			this.bodyBytes = bodyBytes;
			this.contentType = contentType;
		}

		@Override
		// 등록된 고정 RSS 응답을 반환합니다.
		public void handle(HttpExchange exchange) throws java.io.IOException {
			// 상태코드와 헤더를 설정한 뒤 본문 바이트를 작성합니다.
			exchange.getResponseHeaders().add("Content-Type", contentType);
			exchange.sendResponseHeaders(200, bodyBytes.length);
			try (var outputStream = exchange.getResponseBody()) {
				outputStream.write(bodyBytes);
			}
		}
	}
}
