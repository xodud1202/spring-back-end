package com.xodud1202.springbackend.mapper;

import com.xodud1202.springbackend.domain.resume.ResumeEducation;
import com.xodud1202.springbackend.domain.resume.ResumeExperienceBase;
import com.xodud1202.springbackend.domain.resume.ResumeOtherExperience;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ResumeMapper {
	List<ResumeExperienceBase> getResumeExperienceWithDetails(@Param("usrNo") Long usrNo);
	List<ResumeOtherExperience> getResumeOtherExperienceList(@Param("usrNo") Long usrNo);
	List<ResumeEducation> getResumeEducationList(@Param("usrNo") Long usrNo);
}