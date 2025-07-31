package com.xodud1202.springbackend.service;

import com.xodud1202.springbackend.domain.admin.resume.ResumePO;
import com.xodud1202.springbackend.domain.admin.resume.ResumeVO;
import com.xodud1202.springbackend.domain.resume.ResumeEducation;
import com.xodud1202.springbackend.domain.resume.ResumeExperienceBase;
import com.xodud1202.springbackend.domain.resume.ResumeOtherExperience;
import com.xodud1202.springbackend.entity.ResumeBaseEntity;
import com.xodud1202.springbackend.entity.ResumeIntroduceEntity;
import com.xodud1202.springbackend.mapper.ResumeMapper;
import com.xodud1202.springbackend.repository.ResumeBaseRepository;
import com.xodud1202.springbackend.repository.ResumeIntroduceRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class ResumeService {
	
	@PersistenceContext
	private EntityManager em;
	
	private final ResumeMapper resumeMapper;                            // mybatis용 mapper
	private final ResumeBaseRepository resumeBaseRepository;            // RESUME_BASE 단일 테이블 조회
	private final ResumeIntroduceRepository resumeIntroduceRepository;  // RESUME_INTRODUCE 단일 테이블 조회
	
	private List<String> setSkillListFromSkillsResumeBase(String skills) {
		List<String> skillList = null;
		if (StringUtils.isNotBlank(skills)) {
			skillList = Arrays.stream(skills.split(",,"))
					.map(String::trim)
					.filter(skill -> !skill.trim().isEmpty())
					.collect(Collectors.toList());
		}
		
		return skillList;
	}
	
	/**
	 * 주어진 사용자 로그인 ID를 기반으로 이력서 정보를 조회합니다.
	 * 반환된 이력서는 스킬을 파싱하여 스킬 리스트로 변환된 결과를 포함합니다.
	 * @param loginId 사용자 로그인 ID
	 * @return 로그인 ID에 해당하는 이력서 정보. 데이터가 없으면 빈 Optional을 반환
	 */
	public Optional<ResumeBaseEntity> getResumeByLoginId(String loginId) {
		ResumeBaseEntity resume = resumeBaseRepository.findByUserBaseLoginIdAndUserBaseUsrStatCdAndDelYn(loginId, "02", "N");
		log.info("check resume ::: {}", resume);
		
		if (resume != null) {
			// skills를 skillList로 변환
			resume.setSkillList(this.setSkillListFromSkillsResumeBase(resume.getSkills()));
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
	public List<ResumeIntroduceEntity> getResumeIntroduceByUsrNo(Long usrNo) {
		return resumeIntroduceRepository.findByUsrNoAndDelYnOrderBySortSeq(usrNo, "N");
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
	
	/**
	 * 주어진 사용자 번호를 기반으로 사용자의 기타 이력 정보 목록을 조회합니다.
	 * 이 메서드는 사용자 번호를 매개변수로 받아 관련 데이터를 매퍼를 통해 가져옵니다.
	 * @param usrNo 사용자 고유 번호
	 * @return 사용자와 관련된 기타 이력 정보 목록. 만약 해당 사용자의 기타 이력이 없을 경우 빈 리스트 반환
	 */
	public List<ResumeOtherExperience> getResumeOtherExperienceList(Long usrNo) {
		return resumeMapper.getResumeOtherExperienceList(usrNo);
	}
	
	/**
	 * 주어진 사용자 번호를 기반으로 사용자의 학력 정보 목록을 조회합니다.
	 * 이 메서드는 사용자 번호를 매개변수로 받아 관련 데이터를 매퍼를 통해 가져옵니다.
	 * @param usrNo 사용자 고유 번호
	 * @return 사용자와 관련된 학력 정보 목록. 만약 해당 학력 정보가 없을 경우 빈 리스트 반환
	 */
	public List<ResumeEducation> getResumeEducationList(Long usrNo) {
		return resumeMapper.getResumeEducationList(usrNo);
	}
	
	/**
	 * 관리자에 의해 조회 가능한 이력서 목록을 반환합니다.
	 * 이 메서드는 매개변수로 받은 조건을 기반으로 필터링된 이력서 리스트를 반환합니다.
	 * @param param 이력서 목록 조회 조건을 담고 있는 ResumePO 객체
	 * @return 조건에 따라 필터링된 이력서 목록. 만약 데이터가 없을 경우 빈 리스트 반환
	 */
	public List<ResumeVO> getAdminResumeList(ResumePO param) {
		return resumeMapper.getAdminResumeList(param);
	}
	
	/**
	 * 주어진 사용자 번호를 기반으로 이력서 기본 정보를 조회합니다.
	 * 반환된 이력서 기본 정보에는 스킬 문자열이 파싱되어 스킬 리스트로 변환된 결과가 포함됩니다.
	 * @param usrNo 사용자 고유 번호
	 * @return 사용자 번호에 해당하는 이력서 기본 정보. 데이터가 없을 경우 null 반환
	 */
	public ResumeBaseEntity getResumeBaseByUsrNo(long usrNo) {
		ResumeBaseEntity resumeBase = this.getResumeBaseByUsrNoAndDelYn(usrNo, "N");
		
		// skills를 skillList로 변환
		resumeBase.setSkillList(this.setSkillListFromSkillsResumeBase(resumeBase.getSkills()));
		
		return resumeBase;
	}
	
	/**
	 * 주어진 사용자 고유 번호와 삭제 여부를 기반으로 이력서 기본 정보를 조회합니다.
	 * @param usrNo 사용자 고유 번호
	 * @param delYn 삭제 여부 상태 (예: "Y", "N")
	 * @return 주어진 조건에 해당하는 이력서 기본 정보. 데이터가 없을 경우 null 반환
	 */
	public ResumeBaseEntity getResumeBaseByUsrNoAndDelYn(long usrNo, String delYn) {
		return resumeBaseRepository.findByUsrNoAndDelYn(usrNo, delYn);
	}
}