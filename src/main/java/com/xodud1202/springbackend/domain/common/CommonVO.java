package com.xodud1202.springbackend.domain.common;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class CommonVO {
	private Long regNo;
	private String regId;
	private String regNm;
	private Timestamp regDt;
	private Long udtNo;
	private String udtId;
	private String udtNm;
	private Timestamp udtDt;
}
