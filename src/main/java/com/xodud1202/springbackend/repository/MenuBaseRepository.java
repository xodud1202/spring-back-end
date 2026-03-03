package com.xodud1202.springbackend.repository;

import com.xodud1202.springbackend.domain.admin.common.MenuBase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MenuBaseRepository extends JpaRepository<MenuBase, Long> {
	
	// 사용 여부로 메뉴 목록을 조회합니다.
	List<MenuBase> findByUseYn(String useYn);

	// 메뉴 번호로 단일 메뉴를 조회합니다.
	Optional<MenuBase> findByMenuNo(int menuNo);

	// 상위 메뉴 번호로 하위 메뉴 목록을 조회합니다.
	List<MenuBase> findByUpMenuNoOrderBySortSeqAsc(int upMenuNo);

	// 메뉴 URL이 존재하는 개수를 조회합니다.
	int countByMenuUrl(String menuUrl);

	// 특정 메뉴를 제외한 메뉴 URL 중복 개수를 조회합니다.
	@Query("select count(m) from MenuBase m where m.menuUrl = :menuUrl and m.menuNo <> :menuNo")
	int countByMenuUrlAndMenuNoNot(@Param("menuUrl") String menuUrl, @Param("menuNo") int menuNo);

	// 하위 메뉴가 존재하는 개수를 조회합니다.
	int countByUpMenuNo(int upMenuNo);

	// 상위 메뉴 기준 정렬순서 최대값을 조회합니다.
	@Query("select coalesce(max(m.sortSeq), 0) from MenuBase m where m.upMenuNo = :upMenuNo")
	Integer findMaxSortSeqByUpMenuNo(@Param("upMenuNo") int upMenuNo);
}
