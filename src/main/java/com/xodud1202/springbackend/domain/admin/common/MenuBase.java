package com.xodud1202.springbackend.domain.admin.common;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "MENU_BASE")
public class MenuBase {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "MENU_NO")
	private int menuNo;
	
	@Column(name = "UP_MENU_NO", nullable = false)
	private int upMenuNo;
	
	@Column(name = "SORT_SEQ", nullable = false)
	private int sortSeq;
	
	@Column(name = "MENU_LEVEL", nullable = false)
	private int menuLevel;
	
	@Column(name = "MENU_NM", nullable = false)
	private String menuNm;
	
	@Column(name = "MENU_URL")
	private String menuUrl;
	
	@Column(name = "USE_YN", nullable = false, length = 1)
	private String useYn;
	
	@Column(name = "REG_NO", nullable = false)
	private Long regNo;
	
	@Column(name = "REG_DT", nullable = false)
	private LocalDateTime regDt;
	
	@Column(name = "UDT_NO")
	private Long udtNo;
	
	@Column(name = "UDT_DT")
	private LocalDateTime udtDt;
}
