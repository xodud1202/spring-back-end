package com.xodud1202.springbackend.service;

import com.xodud1202.springbackend.domain.news.RssArticleItem;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;

@Component
// HTTP 기반 RSS/Atom 피드 파싱 기능을 제공합니다.
public class HttpRssFeedClient implements RssFeedClient {
	private static final int HTTP_TIMEOUT_SECONDS = 10;
	private static final DateTimeFormatter RFC_1123_OFFSET_COLON_FORMATTER =
		DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss XXX", Locale.ENGLISH);
	private static final DateTimeFormatter RFC_1123_OFFSET_NO_COLON_FORMATTER =
		DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);

	private final HttpClient httpClient = HttpClient.newBuilder().build();

	@Override
	// RSS URL에서 기사 목록을 조회합니다.
	public List<RssArticleItem> fetchFeed(String rssUrl) {
		// RSS URL 유효성을 확인합니다.
		String normalizedUrl = trimToNull(rssUrl);
		if (normalizedUrl == null) {
			return List.of();
		}

		try {
			// HTTP 요청으로 피드 XML 문자열을 조회합니다.
			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(normalizedUrl))
				.timeout(java.time.Duration.ofSeconds(HTTP_TIMEOUT_SECONDS))
				.header("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml, */*")
				.GET()
				.build();
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() < 200 || response.statusCode() >= 300) {
				throw new IllegalStateException("RSS HTTP 응답 코드가 비정상입니다. status=" + response.statusCode());
			}

			// 조회한 XML을 파싱해 기사 목록으로 변환합니다.
			Document document = parseXml(response.body());
			return extractArticleItems(document);
		} catch (Exception exception) {
			throw new IllegalStateException("RSS 조회/파싱에 실패했습니다. url=" + normalizedUrl, exception);
		}
	}

	// XML 문자열을 DOM 문서로 파싱합니다.
	private Document parseXml(String xmlText) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
		factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
		DocumentBuilder builder = factory.newDocumentBuilder();
		return builder.parse(new InputSource(new StringReader(xmlText)));
	}

	// DOM 문서에서 item/entry 노드를 추출해 기사 목록으로 변환합니다.
	private List<RssArticleItem> extractArticleItems(Document document) {
		List<RssArticleItem> articleItems = new ArrayList<>();
		NodeList rssItemNodes = document.getElementsByTagName("item");
		for (int index = 0; index < rssItemNodes.getLength(); index += 1) {
			Node node = rssItemNodes.item(index);
			if (node instanceof Element element) {
				articleItems.add(toRssArticleItemFromRss(element));
			}
		}
		if (!articleItems.isEmpty()) {
			return articleItems;
		}

		// RSS item이 없으면 Atom entry 포맷으로 재시도합니다.
		NodeList atomEntryNodes = document.getElementsByTagName("entry");
		for (int index = 0; index < atomEntryNodes.getLength(); index += 1) {
			Node node = atomEntryNodes.item(index);
			if (node instanceof Element element) {
				articleItems.add(toRssArticleItemFromAtom(element));
			}
		}
		return articleItems;
	}

	// RSS item 엘리먼트를 기사 데이터로 변환합니다.
	private RssArticleItem toRssArticleItemFromRss(Element itemElement) {
		String guid = getChildTextByNames(itemElement, "guid");
		String link = getChildTextByNames(itemElement, "link");
		String title = getChildTextByNames(itemElement, "title");
		String summary = getChildTextByNames(itemElement, "description", "content");
		String authorNm = getChildTextByNames(itemElement, "author", "creator");
		String publishedRaw = getChildTextByNames(itemElement, "pubDate", "published", "updated");
		LocalDateTime publishedDt = parsePublishedDateTime(publishedRaw);

		// 썸네일은 media:thumbnail, enclosure(type=image), media:content 순서로 추출합니다.
		String thumbnailUrl = getChildAttributeByNames(itemElement, "thumbnail", "url");
		if (thumbnailUrl == null) {
			thumbnailUrl = getEnclosureImageUrl(itemElement);
		}
		if (thumbnailUrl == null) {
			thumbnailUrl = getChildAttributeByNames(itemElement, "content", "url");
		}

		return new RssArticleItem(
			trimToNull(guid),
			trimToNull(link),
			trimToNull(title),
			trimToNull(summary),
			trimToNull(thumbnailUrl),
			trimToNull(authorNm),
			publishedDt
		);
	}

	// Atom entry 엘리먼트를 기사 데이터로 변환합니다.
	private RssArticleItem toRssArticleItemFromAtom(Element entryElement) {
		String guid = getChildTextByNames(entryElement, "id");
		String link = getAtomLink(entryElement);
		String title = getChildTextByNames(entryElement, "title");
		String summary = getChildTextByNames(entryElement, "summary", "content");
		String authorNm = getAtomAuthor(entryElement);
		String publishedRaw = getChildTextByNames(entryElement, "published", "updated");
		LocalDateTime publishedDt = parsePublishedDateTime(publishedRaw);
		String thumbnailUrl = getChildAttributeByNames(entryElement, "thumbnail", "url");

		return new RssArticleItem(
			trimToNull(guid),
			trimToNull(link),
			trimToNull(title),
			trimToNull(summary),
			trimToNull(thumbnailUrl),
			trimToNull(authorNm),
			publishedDt
		);
	}

	// RSS/Atom 공통으로 자식 텍스트를 태그명 기준으로 조회합니다.
	private String getChildTextByNames(Element parentElement, String... targetNames) {
		NodeList childNodes = parentElement.getChildNodes();
		for (int index = 0; index < childNodes.getLength(); index += 1) {
			Node childNode = childNodes.item(index);
			if (!(childNode instanceof Element childElement)) {
				continue;
			}
			String localName = resolveNodeName(childElement);
			for (String targetName : targetNames) {
				if (targetName.equalsIgnoreCase(localName)) {
					return trimToNull(childElement.getTextContent());
				}
			}
		}
		return null;
	}

	// 자식 엘리먼트의 속성값을 태그명 기준으로 조회합니다.
	private String getChildAttributeByNames(Element parentElement, String targetName, String attributeName) {
		NodeList childNodes = parentElement.getChildNodes();
		for (int index = 0; index < childNodes.getLength(); index += 1) {
			Node childNode = childNodes.item(index);
			if (!(childNode instanceof Element childElement)) {
				continue;
			}
			String localName = resolveNodeName(childElement);
			if (!targetName.equalsIgnoreCase(localName)) {
				continue;
			}
			String value = trimToNull(childElement.getAttribute(attributeName));
			if (value != null) {
				return value;
			}
		}
		return null;
	}

	// RSS enclosure 중 이미지 타입 URL을 조회합니다.
	private String getEnclosureImageUrl(Element itemElement) {
		NodeList childNodes = itemElement.getChildNodes();
		for (int index = 0; index < childNodes.getLength(); index += 1) {
			Node childNode = childNodes.item(index);
			if (!(childNode instanceof Element childElement)) {
				continue;
			}
			if (!"enclosure".equalsIgnoreCase(resolveNodeName(childElement))) {
				continue;
			}
			String type = trimToNull(childElement.getAttribute("type"));
			String url = trimToNull(childElement.getAttribute("url"));
			if (url == null) {
				continue;
			}
			if (type == null || type.toLowerCase().startsWith("image/")) {
				return url;
			}
		}
		return null;
	}

	// Atom entry의 link href 값을 조회합니다.
	private String getAtomLink(Element entryElement) {
		NodeList childNodes = entryElement.getChildNodes();
		for (int index = 0; index < childNodes.getLength(); index += 1) {
			Node childNode = childNodes.item(index);
			if (!(childNode instanceof Element childElement)) {
				continue;
			}
			if (!"link".equalsIgnoreCase(resolveNodeName(childElement))) {
				continue;
			}
			String href = trimToNull(childElement.getAttribute("href"));
			if (href != null) {
				return href;
			}
		}
		return null;
	}

	// Atom entry의 author명을 조회합니다.
	private String getAtomAuthor(Element entryElement) {
		NodeList childNodes = entryElement.getChildNodes();
		for (int index = 0; index < childNodes.getLength(); index += 1) {
			Node childNode = childNodes.item(index);
			if (!(childNode instanceof Element childElement)) {
				continue;
			}
			if (!"author".equalsIgnoreCase(resolveNodeName(childElement))) {
				continue;
			}
			String authorName = getChildTextByNames(childElement, "name");
			if (authorName != null) {
				return authorName;
			}
		}
		return null;
	}

	// 발행일 문자열을 LocalDateTime으로 변환합니다.
	private LocalDateTime parsePublishedDateTime(String publishedRaw) {
		String normalized = trimToNull(publishedRaw);
		if (normalized == null) {
			return null;
		}

		// RSS RFC-1123 포맷 변환을 우선 시도합니다.
		try {
			return ZonedDateTime.parse(normalized, DateTimeFormatter.RFC_1123_DATE_TIME).toLocalDateTime();
		} catch (Exception ignored) {
		}
		// RFC-1123 +09:00 포맷 변환을 시도합니다.
		try {
			return OffsetDateTime.parse(normalized, RFC_1123_OFFSET_COLON_FORMATTER).toLocalDateTime();
		} catch (Exception ignored) {
		}
		// RFC-1123 +0900 포맷 변환을 시도합니다.
		try {
			return OffsetDateTime.parse(normalized, RFC_1123_OFFSET_NO_COLON_FORMATTER).toLocalDateTime();
		} catch (Exception ignored) {
		}
		// ISO 오프셋 포맷 변환을 시도합니다.
		try {
			return OffsetDateTime.parse(normalized).toLocalDateTime();
		} catch (Exception ignored) {
		}
		// LocalDateTime 포맷 변환을 시도합니다.
		try {
			return LocalDateTime.parse(normalized.replace(" ", "T"));
		} catch (Exception ignored) {
		}
		return null;
	}

	// 네임스페이스 접두사를 제거한 노드명을 반환합니다.
	private String resolveNodeName(Element element) {
		String localName = element.getLocalName();
		if (localName != null) {
			return localName;
		}
		String nodeName = element.getNodeName();
		if (nodeName == null) {
			return "";
		}
		int separatorIndex = nodeName.indexOf(':');
		if (separatorIndex < 0) {
			return nodeName;
		}
		return nodeName.substring(separatorIndex + 1);
	}

	// 문자열을 trim하고 빈 문자열은 null로 반환합니다.
	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
