package com.xodud1202.springbackend.service;

import com.xodud1202.springbackend.domain.admin.resume.ResumePO;
import com.xodud1202.springbackend.domain.admin.resume.ResumeVO;
import com.xodud1202.springbackend.domain.resume.*;
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
	 * 주어진 사용자 로그인 ID를 기반으로 이력서 정보를 조회합니다.
	 * 반환된 이력서는 스킬을 파싱하여 스킬 리스트로 변환된 결과를 포함합니다.
	 * @param loginId 사용자 로그인 ID
	 * @return 로그인 ID에 해당하는 이력서 정보. 데이터가 없으면 빈 Optional을 반환
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
	 * 주어진 사용자 번호를 기반으로 사용자와 관련된 자기소개 리스트를 조회합니다.
	 * 이 메서드는 삭제 상태가 아닌 자기소개 데이터를 정렬 순서에 따라 반환합니다.
	 * @param usrNo 사용자 고유 번호
	 * @return 사용자와 관련된 자기소개 리스트. 만약 데이터가 없거나 오류가 발생하면 빈 리스트를 반환
	 */
	public List<ResumeIntroduce> getResumeIntroduceByUsrNo(Long usrNo) {
		return resumeRepository.findResumeIntroduceByUsrNo(usrNo);
	}
	
	/**
	 * 주어진 사용자 번호로 사용자의 이력서 경험과 관련 상세 정보를 조회합니다.
	 * 이 메서드는 사용자 번호를 기준으로 매퍼를 통해 데이터를 가져옵니다.
	 * @param usrNo 사용자 고유 번호
	 * @return 사용자와 관련된 이력서 경험 정보 목록. 만약 해당 사용자의 이력서 경험이 없을 경우 빈 리스트 반환
	 */
	public List<ResumeExperienceBase> getResumeExperienceWithDetails(Long usrNo) {
		return resumeMapper.getResumeExperienceWithDetails(usrNo);
	}
	
	public List<ResumeOtherExperience> getResumeOtherExperienceList(Long usrNo) {
		return resumeMapper.getResumeOtherExperienceList(usrNo);
	}
	
	public List<ResumeEducation> getResumeEducationList(Long usrNo) {
		return resumeMapper.getResumeEducationList(usrNo);
	}
	
	public List<ResumeVO> getAdminResumeList(ResumePO param) {
		return resumeMapper.getAdminResumeList(param);
	}
}