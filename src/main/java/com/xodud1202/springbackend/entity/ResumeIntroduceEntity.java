// src/main/java/com/xodud1202/springbackend/domain/resume/ResumeIntroduce.java
package com.xodud1202.springbackend.entity;

import com.xodud1202.springbackend.domain.resume.ResumeIntroduceId;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "RESUME_INTRODUCE")
@IdClass(ResumeIntroduceId.class)
public class ResumeIntroduceEntity {
	@Id
	@Column(name = "USR_NO")
	private Long usrNo;
	
	@Id
	@Column(name = "SORT_SEQ")
	private Integer sortSeq;
	
	@Column(name = "INTRODUCE_TITLE")
	private String introduceTitle;
	
	@Column(name = "INTRODUCE", columnDefinition = "TEXT")
	private String introduce;
	
	@Column(name = "DEL_YN")
	private String delYn;
}

