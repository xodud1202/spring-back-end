package com.xodud1202.springbackend.domain.resume;

import lombok.Data;

@Data
public class ResumeOtherExperience {
	private Long usrNo;
	
	private int sortSeq;
	
	private String experienceTitle;
	private String experienceSubTitle;
	private String experienceDesc;
	private String experienceStartDt;
	private String experienceEndDt;
}
