package com.xodud1202.springbackend.mapper;

import com.xodud1202.springbackend.domain.admin.resume.ResumePO;
import com.xodud1202.springbackend.domain.admin.resume.ResumeVO;
import com.xodud1202.springbackend.domain.resume.ResumeEducation;
import com.xodud1202.springbackend.domain.resume.ResumeExperienceBase;
import com.xodud1202.springbackend.domain.resume.ResumeOtherExperience;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ResumeMapper {
	List<ResumeExperienceBase> getResumeExperienceWithDetails(@Param("usrNo") Long usrNo);
	List<ResumeExperienceBase> getAdminResumeExperienceList(@Param("usrNo") Long usrNo);
	int insertResumeExperienceBase(ResumeExperienceBase param);
	int updateResumeExperienceBase(ResumeExperienceBase param);
	int softDeleteResumeExperienceBase(@Param("experienceNo") Long experienceNo, @Param("usrNo") Long usrNo);
	int softDeleteResumeExperienceDetail(@Param("experienceNo") Long experienceNo, @Param("usrNo") Long usrNo);
	int insertResumeExperienceDetails(@Param("experienceNo") Long experienceNo, @Param("usrNo") Long usrNo, @Param("detailList") List<com.xodud1202.springbackend.domain.resume.ResumeExperienceDetail> detailList);
	List<ResumeOtherExperience> getResumeOtherExperienceList(@Param("usrNo") Long usrNo);
	List<ResumeEducation> getResumeEducationList(@Param("usrNo") Long usrNo);
	List<ResumeVO> getAdminResumeList(ResumePO param);
}
