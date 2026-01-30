package com.xodud1202.springbackend.domain.admin.goods;

import com.xodud1202.springbackend.domain.common.CommonVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class GoodsVO extends CommonVO {
	private String goodsId;
	private String erpStyleCd;
	private String goodsNm;
	private String goodsStatCd;
	private String goodsStatNm;
	private String goodsDivCd;
	private String goodsDivNm;
	private String showYn;
}
