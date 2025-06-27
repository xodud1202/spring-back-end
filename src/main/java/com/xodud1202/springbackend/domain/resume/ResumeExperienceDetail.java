package com.xodud1202.springbackend.domain.resume;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ResumeExperienceDetail {
	private Long usrNo;
	private String workTitle;
	private String workDesc;
	private String workStartDt;
	private String workEndDt;
	private Integer sortSeq;
}