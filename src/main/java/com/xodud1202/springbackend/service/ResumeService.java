package com.xodud1202.springbackend.service;

import com.xodud1202.springbackend.domain.admin.resume.ResumePO;
import com.xodud1202.springbackend.domain.admin.resume.ResumeVO;
import com.xodud1202.springbackend.domain.resume.ResumeEducation;
import com.xodud1202.springbackend.domain.resume.ResumeExperienceBase;
import com.xodud1202.springbackend.domain.resume.ResumeExperienceDetail;
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

import java.util.*;
import java.util.stream.Collectors;

import java.nio.charset.Charset;

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

	/**
	 * 주어진 문자열에서 스킬 목록을 추출하여 리스트로 변환합니다.
	 * 입력 문자열은 '~~,,~~,,~~' 형식으로 쉼표 두 개로 구분된 스킬의 집합이며,
	 * 이 메서드는 이를 파싱하여 개별 스킬로 분리한 후 트리밍 및 비어 있는 항목을 필터링하여 리스트로 만듭니다.
	 * @param skills 쉼표 두 개(,,)로 구분된 스킬 문자열. 빈 문자열이나 null일 경우 빈 리스트 반환.
	 * @return 변환된 스킬 리스트. 입력이 null 또는 유효하지 않은 경우 null 반환.
	 */
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
	 * 주어진 스킬 목록을 중복 제거, 공백 제거, 트리밍하여 문자열로 변환합니다.
	 * 변환된 문자열은 쉼표 두 개(,,)로 각 스킬을 구분합니다.
	 * @param skillList 스킬 문자열 목록. null 또는 비어 있는 리스트일 경우 null을 반환.
	 * @return 처리된 스킬 문자열. 결과가 null이거나 유효한 스킬이 없을 경우 null 반환.
	 */
	private String setSkillsFromSkillList(List<String> skillList) {
		// 3. skillList를 중복/공백 제거 후 문자열로 변환 (,, 구분자 사용)
		if (skillList != null && !skillList.isEmpty()) {
			String combinedSkills = skillList.stream()
					.filter(Objects::nonNull)						// null 방지
					.map(String::trim)								// 좌우 공백 제거
					.filter(s -> !s.isEmpty())				// 공백 제거 후 빈 문자열("")인 경우 제외
					.distinct()												// 중복 데이터 제거
					.collect(Collectors.joining(",,"));	// ",," 구분자로 연결

			// 결과가 모두 빈 문자열이라서 combinedSkills가 비어있을 경우 처리
			return combinedSkills.isEmpty() ? null : combinedSkills;
		} else {
			return null;
		}
	}
	
	/**
	 * 주어진 사용자 로그인 ID를 기반으로 이력서 정보를 조회합니다.
	 * 반환된 이력서는 스킬을 파싱하여 스킬 리스트로 변환된 결과를 포함합니다.
	 * @param loginId 사용자 로그인 ID
	 * @return 로그인 ID에 해당하는 이력서 정보. 데이터가 없으면 빈 Optional을 반환
	 */
	public Optional<ResumeBaseEntity> getResumeByLoginId(String loginId) {
		Optional<ResumeBaseEntity> resumeOpt = resumeBaseRepository.findByUserBaseLoginIdAndUserBaseUsrStatCdAndDelYn(loginId, "02", "N");
		if (resumeOpt.isPresent()) {
			ResumeBaseEntity resume = resumeOpt.get();
			
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

	private String normalizeText(String value) {
		return value == null ? "" : value.trim();
	}

	private String normalizeDate(String value) {
		return value == null ? "" : value.trim();
	}

	private boolean isDetailChanged(ResumeExperienceDetail incoming, ResumeExperienceDetail existing) {
		if (existing == null) {
			return true;
		}
		if (!normalizeText(incoming.getWorkTitle()).equals(normalizeText(existing.getWorkTitle()))) {
			return true;
		}
		if (!normalizeText(incoming.getWorkDesc()).equals(normalizeText(existing.getWorkDesc()))) {
			return true;
		}
		if (!normalizeDate(incoming.getWorkStartDt()).equals(normalizeDate(existing.getWorkStartDt()))) {
			return true;
		}
		if (!normalizeDate(incoming.getWorkEndDt()).equals(normalizeDate(existing.getWorkEndDt()))) {
			return true;
		}
		return !Objects.equals(incoming.getSortSeq(), existing.getSortSeq());
	}

	private ResumeIntroduceEntity createDefaultIntroduce(Long usrNo) {
		ResumeIntroduceEntity entity = new ResumeIntroduceEntity();
		entity.setUsrNo(usrNo);
		entity.setSortSeq(1);
		entity.setDelYn("N");
		return entity;
	}

	public Map<String, String> updateResumeIntroduceByUsrNo(Long usrNo, String introduce) {
		Map<String, String> result = new HashMap<>();

		try {
			List<ResumeIntroduceEntity> introduceList = resumeIntroduceRepository.findByUsrNoAndDelYnOrderBySortSeq(usrNo, "N");
			ResumeIntroduceEntity entity = introduceList.isEmpty()
					? createDefaultIntroduce(usrNo)
					: introduceList.get(0);

			entity.setIntroduce(introduce);

			resumeIntroduceRepository.save(entity);

			result.put("result", "success");
			result.put("message", "자기소개가 성공적으로 수정되었습니다.");
		} catch (Exception e) {
			log.error("자기소개 수정 중 오류 발생: ", e);
			result.put("result", "error");
			result.put("message", "서버 오류가 발생했습니다.");
		}

		return result;
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

	public List<ResumeExperienceBase> getAdminResumeExperienceList(Long usrNo) {
		return resumeMapper.getAdminResumeExperienceList(usrNo);
	}

	public Map<String, String> saveResumeExperience(Long usrNo, ResumeExperienceBase param) {
		Map<String, String> result = new HashMap<>();

		if (param == null) {
			result.put("result", "fail");
			result.put("message", "요청 데이터가 없습니다.");
			return result;
		}

		param.setUsrNo(usrNo);

		if (StringUtils.isBlank(param.getCompanyNm())
				|| StringUtils.isBlank(param.getEmploymentTypeCd())
				|| StringUtils.isBlank(param.getPosition())
				|| StringUtils.isBlank(param.getDuty())) {
			result.put("result", "fail");
			result.put("message", "필수 항목을 입력하세요.");
			return result;
		}

		try {
			boolean isNew = param.getExperienceNo() == null;

			if (isNew) {
				resumeMapper.insertResumeExperienceBase(param);
			} else {
				resumeMapper.updateResumeExperienceBase(param);
			}

			Long experienceNo = param.getExperienceNo();
			List<ResumeExperienceDetail> detailList = param.getResumeExperienceDetailList();

			if (detailList != null) {
				List<ResumeExperienceDetail> filteredList = new ArrayList<>();
				int seq = 1;
				for (ResumeExperienceDetail detail : detailList) {
					if (detail == null || StringUtils.isBlank(detail.getWorkTitle())) {
						continue;
					}
					if (detail.getSortSeq() == null) {
						detail.setSortSeq(seq);
					}
					seq += 1;
					filteredList.add(detail);
				}

				if (isNew) {
					if (!filteredList.isEmpty()) {
						resumeMapper.insertResumeExperienceDetails(experienceNo, usrNo, filteredList);
					}
				} else {
					List<ResumeExperienceDetail> existingList = resumeMapper.getResumeExperienceDetailList(experienceNo, usrNo);
					Map<Long, ResumeExperienceDetail> existingMap = existingList.stream()
							.filter(detail -> detail.getExperienceDtlNo() != null)
							.collect(Collectors.toMap(ResumeExperienceDetail::getExperienceDtlNo, detail -> detail));

					Set<Long> incomingIds = filteredList.stream()
							.map(ResumeExperienceDetail::getExperienceDtlNo)
							.filter(Objects::nonNull)
							.collect(Collectors.toSet());

					List<Long> deleteIds = existingMap.keySet().stream()
							.filter(id -> !incomingIds.contains(id))
							.collect(Collectors.toList());

					if (!deleteIds.isEmpty()) {
						resumeMapper.softDeleteResumeExperienceDetailByIds(experienceNo, usrNo, deleteIds);
					}

					List<ResumeExperienceDetail> insertList = new ArrayList<>();
					for (ResumeExperienceDetail detail : filteredList) {
						Long detailNo = detail.getExperienceDtlNo();
						if (detailNo == null) {
							insertList.add(detail);
							continue;
						}
						ResumeExperienceDetail existing = existingMap.get(detailNo);
						if (existing == null) {
							insertList.add(detail);
							continue;
						}
						if (isDetailChanged(detail, existing)) {
							resumeMapper.updateResumeExperienceDetail(experienceNo, usrNo, detail);
						}
					}

					if (!insertList.isEmpty()) {
						resumeMapper.insertResumeExperienceDetails(experienceNo, usrNo, insertList);
					}
				}
			}

			result.put("result", "success");
			result.put("message", isNew ? "경력이 등록되었습니다." : "경력이 수정되었습니다.");
		} catch (Exception e) {
			log.error("경력 저장 중 오류 발생: ", e);
			result.put("result", "error");
			result.put("message", "서버 오류가 발생했습니다.");
		}

		return result;
	}

	public Map<String, String> deleteResumeExperience(Long usrNo, Long experienceNo) {
		Map<String, String> result = new HashMap<>();

		if (experienceNo == null) {
			result.put("result", "fail");
			result.put("message", "경력 번호가 없습니다.");
			return result;
		}

		try {
			resumeMapper.softDeleteResumeExperienceBase(experienceNo, usrNo);
			resumeMapper.softDeleteResumeExperienceDetail(experienceNo, usrNo);
			result.put("result", "success");
			result.put("message", "경력이 삭제되었습니다.");
		} catch (Exception e) {
			log.error("경력 삭제 중 오류 발생: ", e);
			result.put("result", "error");
			result.put("message", "서버 오류가 발생했습니다.");
		}

		return result;
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

	public List<ResumeEducation> getAdminResumeEducationList(Long usrNo) {
		log.info("file.encoding={}", System.getProperty("file.encoding"));
		log.info("sun.jnu.encoding={}", System.getProperty("sun.jnu.encoding"));
		log.info("defaultCharset={}", Charset.defaultCharset());
		return resumeMapper.getAdminResumeEducationList(usrNo);
	}

	public Map<String, String> saveResumeEducation(Long usrNo, ResumeEducation param) {
		Map<String, String> result = new HashMap<>();

		if (param == null) {
			result.put("result", "fail");
			result.put("message", "요청 데이터가 없습니다.");
			return result;
		}

		param.setUsrNo(usrNo);

		if (StringUtils.isBlank(param.getEducationNm())
				|| StringUtils.isBlank(param.getDepartment())
				|| StringUtils.isBlank(param.getEducationStatCd())
				|| StringUtils.isBlank(param.getEducationStartDt())) {
			result.put("result", "fail");
			result.put("message", "필수 항목을 입력하세요.");
			return result;
		}

		try {
			boolean isNew = param.getEducationNo() == null;
			int affected;

			if (isNew) {
				affected = resumeMapper.insertResumeEducation(param);
			} else {
				affected = resumeMapper.updateResumeEducation(param);
			}

			if (affected < 1 && !isNew) {
				result.put("result", "fail");
				result.put("message", "학력 수정 대상이 없습니다.");
				return result;
			}

			result.put("result", "success");
			result.put("message", isNew ? "학력이 등록되었습니다." : "학력이 수정되었습니다.");
		} catch (Exception e) {
			log.error("학력 저장 중 오류 발생: ", e);
			result.put("result", "error");
			result.put("message", "서버 오류가 발생했습니다.");
		}

		return result;
	}

	public Map<String, String> deleteResumeEducation(Long usrNo, Long educationNo) {
		Map<String, String> result = new HashMap<>();

		if (educationNo == null) {
			result.put("result", "fail");
			result.put("message", "삭제 대상 정보가 없습니다.");
			return result;
		}

		try {
			int affected = resumeMapper.softDeleteResumeEducation(usrNo, educationNo);
			if (affected < 1) {
				result.put("result", "fail");
				result.put("message", "학력 삭제 대상이 없습니다.");
				return result;
			}

			result.put("result", "success");
			result.put("message", "학력이 삭제되었습니다.");
		} catch (Exception e) {
			log.error("학력 삭제 중 오류 발생: ", e);
			result.put("result", "error");
			result.put("message", "서버 오류가 발생했습니다.");
		}

		return result;
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

	/**
	 * 주어진 사용자 번호를 기반으로 이력서 기본 정보를 업데이트합니다.
	 *
	 * @param param 업데이트할 이력서 정보를 포함하는 {@link ResumeBaseEntity} 객체
	 *              - 사용자 번호(usrNo), 사용자 이름(userNm), 부제목(subTitle), 전화번호(mobile),
	 *                이메일(email), 포트폴리오 URL(portfolio), 최근 연봉(lastPay), 프로필 이미지 경로(faceImgPath),
	 *                주소(addr), 기술 목록(skillList) 정보가 포함되어야 합니다.
	 * @return 업데이트 처리 결과를 담은 {@link Map} 객체
	 *         - "result": 처리 결과("success", "fail", "error")
	 *         - "message": 처리 결과 메시지
	 */
	public Map<String, String> updateResumeBaseByUsrNo(ResumeBaseEntity param) {
		Map<String, String> result = new HashMap<>();

		try {
			// 1. 기존 데이터 조회 (영속 상태)
			ResumeBaseEntity resumeBase = this.getResumeBaseByUsrNoAndDelYn(param.getUsrNo(), "N");

			if (resumeBase != null) {
				// 2. 데이터 업데이트 (Dirty Checking 활용)
				resumeBase.setUserNm(param.getUserNm());
				resumeBase.setSubTitle(param.getSubTitle());
				resumeBase.setMobile(param.getMobile());
				resumeBase.setEmail(param.getEmail());
				resumeBase.setPortfolio(param.getPortfolio());
				resumeBase.setLastPay(param.getLastPay());
				resumeBase.setFaceImgPath(param.getFaceImgPath());
				resumeBase.setAddr(param.getAddr());
				resumeBase.setSkills(this.setSkillsFromSkillList(param.getSkillList()));

				// @Transactional이 걸려있으므로 save()를 호출하지 않아도 자동으로 DB에 반영됩니다.
				result.put("result", "success");
				result.put("message", "이력서가 성공적으로 수정되었습니다.");
			} else {
				result.put("result", "fail");
				result.put("message", "해당 사용자의 이력서 정보를 찾을 수 없습니다.");
			}
		} catch (Exception e) {
			log.error("이력서 업데이트 중 오류 발생: ", e);
			result.put("result", "error");
			result.put("message", "서버 오류가 발생했습니다.");
		}

		return result;
	}
}
