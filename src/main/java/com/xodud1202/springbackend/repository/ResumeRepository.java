package com.xodud1202.springbackend.repository;

import com.xodud1202.springbackend.domain.resume.ResumeBase;
import com.xodud1202.springbackend.domain.resume.ResumeIntroduce;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class ResumeRepository {
	
	@PersistenceContext
	private EntityManager entityManager;
	
	public Optional<ResumeBase> findResumeByLoginId(String loginId) {
		String sql = """
            SELECT RB.USR_NO
                 , RB.USER_NM
                 , RB.SUB_TITLE
                 , RB.MOBILE
                 , RB.EMAIL
                 , RB.PORTFOLIO
                 , RB.FACE_IMG_PATH
                 , RB.SKILLS
                 , RB.ADDR
                 , RB.LAST_PAY
              FROM USER_BASE UB
             INNER JOIN RESUME_BASE RB
                ON UB.USR_NO = RB.USR_NO
             WHERE UB.USR_STAT_CD = '02'
               AND RB.DEL_YN = 'N'
               AND UB.LOGIN_ID = :loginId
            """;
		
		Query query = entityManager.createNativeQuery(sql, "ResumeBaseMapping");
		query.setParameter("loginId", loginId);
		
		try {
			log.info("check 111 : ");
			ResumeBase result = (ResumeBase) query.getSingleResult();
			log.info("check 222 : {}", result);
			return Optional.of(result);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return Optional.empty();
		}
	}
	
	public List<ResumeIntroduce> findResumeIntroduceByUsrNo(Long usrNo) {
		String sql = """
        SELECT RI.INTRODUCE_TITLE
             , RI.INTRODUCE
          FROM RESUME_INTRODUCE RI
         WHERE RI.USR_NO = :usrNo
           AND RI.DEL_YN = 'N'
         ORDER BY RI.SORT_SEQ
        """;
		
		Query query = entityManager.createNativeQuery(sql);
		query.setParameter("usrNo", usrNo);
		
		try {
			@SuppressWarnings("unchecked")
			List<Object[]> results = query.getResultList();
			
			return results.stream()
					.map(row -> {
						ResumeIntroduce introduce = new ResumeIntroduce();
						introduce.setIntroduceTitle((String) row[0]); // INTRODUCE_TITLE
						introduce.setIntroduce((String) row[1]);      // INTRODUCE
						return introduce;
					})
					.collect(Collectors.toList());
		} catch (Exception e) {
			return List.of(); // 빈 리스트 반환
		}
	}
}