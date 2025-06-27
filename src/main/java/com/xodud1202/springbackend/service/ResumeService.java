package com.xodud1202.springbackend.service;

import com.xodud1202.springbackend.domain.resume.ResumeBase;
import com.xodud1202.springbackend.domain.resume.ResumeExperienceBase;
import com.xodud1202.springbackend.domain.resume.ResumeIntroduce;
import com.xodud1202.springbackend.domain.resume.ResumeOtherExperience;
import com.xodud1202.springbackend.mapper.ResumeMapper;
import com.xodud1202.springbackend.repository.ResumeRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResumeService {
	
	@PersistenceContext
	private EntityManager em;
	
	private final ResumeRepository resumeRepository;
	private final ResumeMapper resumeMapper;
	
	/**
	 * Retrieves a {@code ResumeBase} entity based on the provided login ID.
	 * The method fetches the resume associated with the specified login ID from the repository.
	 * If a resume is found, it processes the {@code skills} field into a list of individual skills
	 * and sets this list in the {@code skillList} property before returning the result.
	 * @param loginId the login ID of the user whose resume is to be retrieved
	 * @return an {@code Optional} containing the {@code ResumeBase} with the processed {@code skillList}
	 *         if the resume is found, or an empty {@code Optional} if no resume exists for the given login ID
	 */
	public Optional<ResumeBase> getResumeByLoginId(String loginId) {
		Optional<ResumeBase> resumeOpt = resumeRepository.findResumeByLoginId(loginId);
		
		if (resumeOpt.isPresent()) {
			ResumeBase resume = resumeOpt.get();
			
			// skills를 skillList로 변환
			if (StringUtils.hasText(resume.getSkills())) {
				List<String> skillList = Arrays.stream(resume.getSkills().split(",,"))
						.map(String::trim)
						.filter(skill -> !skill.trim().isEmpty())
						.collect(Collectors.toList());
				resume.setSkillList(skillList);
			}
			
			return Optional.of(resume);
		}
		
		return Optional.empty();
	}
	
	/**
	 * Retrieves a list of {@code ResumeIntroduce} entities associated with a given user number.
	 * The method fetches the user's resume introductions from the underlying repository, filtering by user number
	 * and ensuring only non-deleted records are returned.
	 * @param usrNo the unique identifier of the user whose resume introductions are to be retrieved
	 * @return a list of {@code ResumeIntroduce} entities associated with the specified user number,
	 *         or an empty list if no matching records are found
	 */
	public List<ResumeIntroduce> getResumeIntroduceByUsrNo(Long usrNo) {
		return resumeRepository.findResumeIntroduceByUsrNo(usrNo);
	}
	
	/**
	 * Retrieves a list of {@code ResumeExperienceBase} entities with detailed information for a specific user.
	 * The method fetches the work experiences associated with the provided user number, including company details,
	 * employment type, position, duties, and associated experience details.
	 * @param usrNo the unique identifier of the user whose resume experiences are to be retrieved
	 * @return a list of {@code ResumeExperienceBase} entities that contains the user's work experience details,
	 *         or an empty list if no matching records are found
	 */
	public List<ResumeExperienceBase> getResumeExperienceWithDetails(Long usrNo) {
		return resumeMapper.getResumeExperienceWithDetails(usrNo);
	}
	
	public List<ResumeOtherExperience> getResumeOtherExperienceList(Long usrNo) {
		return resumeMapper.getResumeOtherExperienceList(usrNo);
	}
}