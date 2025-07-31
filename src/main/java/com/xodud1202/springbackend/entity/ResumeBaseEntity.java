package com.xodud1202.springbackend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Entity
@NoArgsConstructor
@Table(name = "RESUME_BASE")
@IdClass(ResumeBaseEntity.class)
public class ResumeBaseEntity {
	@Id
	@Column(name = "USR_NO")
	private Long usrNo;
	
	@Column(name = "USER_NM")
	private Integer userNm;
	
	@Column(name = "SUB_TITLE")
	private String subTitle;
	
	@Column(name = "MOBILE")
	private String mobile;
	
	@Column(name = "EMAIL")
	private String email;
	
	@Column(name = "PORTFOLIO")
	private String portfolio;
	
	@Column(name = "LAST_PAY")
	private Long lastPay;
	
	@Column(name = "FACE_IMG_PATH")
	private String faceImgPath;
	
	@Column(name = "SKILLS")
	private String skills;
	
	@Column(name = "ADDR")
	private String addr;
	
	@Column(name = "DEL_YN")
	private String delYn;
	
	private List<String> skillList;
	
	@OneToOne
	@JoinColumn(name = "USR_NO", referencedColumnName = "USR_NO")
	private UserBaseEntity userBase;
}
