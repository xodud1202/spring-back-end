package com.xodud1202.springbackend.domain.resume;

import lombok.Data;

@Data
public class ResumeOtherExperience {
	private Long otherExperienceNo;
	private Long usrNo;
	
	private Integer sortSeq;
	
	private String experienceTitle;
	private String experienceSubTitle;
	private String experienceDesc;
	private String experienceStartDt;
	private String experienceEndDt;
}
