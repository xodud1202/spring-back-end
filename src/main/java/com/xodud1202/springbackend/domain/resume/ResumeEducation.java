package com.xodud1202.springbackend.domain.resume;

import lombok.Data;

@Data
public class ResumeEducation {
	private Long usrNo;
	
	private String educationNm;
	private String department;
	private String educationScore;
	private String educationStat;
	private String educationStartDt;
	private String educationEndDt;
	private String logoPath;
}
