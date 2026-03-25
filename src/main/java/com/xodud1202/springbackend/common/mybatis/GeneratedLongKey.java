package com.xodud1202.springbackend.common.mybatis;

// MyBatis 생성키를 외부 명령 객체와 분리해 수집하는 홀더입니다.
public class GeneratedLongKey {
	private Long value;

	// 생성된 Long 키 값을 반환합니다.
	public Long getValue() {
		return value;
	}

	// MyBatis가 채운 생성키 값을 저장합니다.
	public void setValue(Long value) {
		this.value = value;
	}
}
