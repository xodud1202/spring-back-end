package com.xodud1202.springbackend.repository;

import com.xodud1202.springbackend.domain.admin.common.MenuBase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuBaseRepository extends JpaRepository<MenuBase, Long> {
	
	List<MenuBase> findByUseYn(String useYn);
}
