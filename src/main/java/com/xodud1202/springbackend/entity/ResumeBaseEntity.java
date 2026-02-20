package com.xodud1202.springbackend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Entity
@NoArgsConstructor
@Table(name = "RESUME_BASE")
/**
 * RESUME_BASE 테이블 매핑 엔티티입니다.
 */
public class ResumeBaseEntity {
	@Id
	@Column(name = "USR_NO")
	private Long usrNo;
	
	@Column(name = "USER_NM")
	private String userNm;

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
	
	/**
	 * SKILLS 문자열 컬럼을 API 응답용 리스트로 변환해 담는 비영속 필드입니다.
	 */
	@Transient
	private List<String> skillList;
	
	@OneToOne
	@JoinColumn(name = "USR_NO", referencedColumnName = "USR_NO")
	private UserBaseEntity userBase;
}
