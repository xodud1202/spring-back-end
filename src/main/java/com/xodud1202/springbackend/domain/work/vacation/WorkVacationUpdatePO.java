package com.xodud1202.springbackend.domain.work.vacation;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
// 휴가 수정 요청 정보를 정의합니다.
public class WorkVacationUpdatePO extends WorkVacationCreatePO {
}
