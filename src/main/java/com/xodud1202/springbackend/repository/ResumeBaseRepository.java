package com.xodud1202.springbackend.repository;

import com.xodud1202.springbackend.entity.ResumeBaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ResumeBaseRepository extends JpaRepository<ResumeBaseEntity, Long> {
	
	Optional<ResumeBaseEntity> findByUserBaseLoginIdAndUserBaseUsrStatCdAndDelYn(String loginId, String usrStatCd, String delYn);
	
	ResumeBaseEntity findByUsrNoAndDelYn(Long usrNo, String delYn);
}
