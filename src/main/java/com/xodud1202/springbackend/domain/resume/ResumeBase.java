package com.xodud1202.springbackend.domain.resume;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Entity
@NoArgsConstructor
@SqlResultSetMapping(
		name = "ResumeBaseMapping",
		classes = @ConstructorResult(
				targetClass = ResumeBase.class,
				columns = {
						@ColumnResult(name = "USR_NO", type = Long.class),
						@ColumnResult(name = "USER_NM", type = String.class),
						@ColumnResult(name = "SUB_TITLE", type = String.class),
						@ColumnResult(name = "MOBILE", type = String.class),
						@ColumnResult(name = "EMAIL", type = String.class),
						@ColumnResult(name = "PORTFOLIO", type = String.class),
						@ColumnResult(name = "FACE_IMG_PATH", type = String.class),
						@ColumnResult(name = "SKILLS", type = String.class),
						@ColumnResult(name = "ADDR", type = String.class)
				}
		)
)
@Table(name = "RESUME_BASE")
public class ResumeBase {
	@Id
	private Long usrNo;
	private String userNm;
	private String subTitle;
	private String mobile;
	private String email;
	private String portfolio;
	private String faceImgPath;
	private String skills;
	private String addr;
	private List<String> skillList;
	
	// 생성자 추가
	public ResumeBase(Long usrNo, String userNm, String subTitle, String mobile,
	                  String email, String portfolio, String faceImgPath,
	                  String skills, String addr) {
		this.usrNo = usrNo;
		this.userNm = userNm;
		this.subTitle = subTitle;
		this.mobile = mobile;
		this.email = email;
		this.portfolio = portfolio;
		this.faceImgPath = faceImgPath;
		this.skills = skills;
		this.addr = addr;
	}
}