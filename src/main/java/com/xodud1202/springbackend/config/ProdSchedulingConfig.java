package com.xodud1202.springbackend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@Profile("prod")
@EnableScheduling
// 운영 prod 프로파일에서만 스케줄링 기능을 활성화하는 설정입니다.
public class ProdSchedulingConfig {
}
