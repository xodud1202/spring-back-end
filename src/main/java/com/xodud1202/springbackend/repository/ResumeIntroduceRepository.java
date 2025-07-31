package com.xodud1202.springbackend.repository;

import com.xodud1202.springbackend.entity.ResumeIntroduceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResumeIntroduceRepository extends JpaRepository<ResumeIntroduceEntity, Long> {
	List<ResumeIntroduceEntity> findByUsrNoAndDelYnOrderBySortSeq(Long usrNo, String delYn);
}
