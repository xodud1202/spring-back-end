package com.xodud1202.springbackend.domain.resume;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

// 복합키 클래스도 별도로 생성해야 함
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumeIntroduceId implements Serializable {
	private Long usrNo;
	private Integer sortSeq;
}
