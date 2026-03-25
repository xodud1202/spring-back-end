package com.xodud1202.springbackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
@MapperScan("com.xodud1202.springbackend.mapper")
// 스프링부트 애플리케이션을 부팅합니다.
public class SpringBackEndApplication {
	// 애플리케이션 진입점을 실행합니다.
	public static void main(String[] args) {
		SpringApplication.run(SpringBackEndApplication.class, args);
	}
}
