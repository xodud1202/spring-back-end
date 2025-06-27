package com.xodud1202.springbackend.domain.resume;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class ResumeExperienceBase {
	private Long usrNo;
	private Long experienceNo;
	private String companyNm;
	private String employmentTypeCd;
	private String employmentType; // 코드명 변환된 값
	private String position;
	private String duty;
	private String workStartDt;
	private String workEndDt;
	
	private List<ResumeExperienceDetail> resumeExperienceDetailList;
}