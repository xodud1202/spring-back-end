package com.xodud1202.springbackend.domain.admin.resume;

import com.xodud1202.springbackend.domain.common.CommonVO;
import lombok.Data;

@Data
public class ResumeVO extends CommonVO {
	private Long usrNo;
	private String loginId;
	private String userNm;
	
	private int lastPay;
	private String resumeNm;
	private String subTitle;
	private String mobile;
	private String email;
	private String portfolio;
	private String faceImgPath;
	private String skills;
	private String addr;
}
