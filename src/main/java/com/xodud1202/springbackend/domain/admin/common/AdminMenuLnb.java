package com.xodud1202.springbackend.domain.admin.common;

import lombok.Data;

import java.util.List;

@Data
public class AdminMenuLnb {
	private String menuNm;
	private String menuUrl;
	private List<AdminMenuLnb> subMenus;
}
