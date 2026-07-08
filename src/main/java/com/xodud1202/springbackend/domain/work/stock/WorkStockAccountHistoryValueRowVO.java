package com.xodud1202.springbackend.domain.work.stock;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
// 주식계좌이력 월별 표의 단일 지표 행을 전달합니다.
public class WorkStockAccountHistoryValueRowVO {
	private String rowKey;
	private String rowLabel;
	private String valueType;
	private Map<String, BigDecimal> valueMap;
}
