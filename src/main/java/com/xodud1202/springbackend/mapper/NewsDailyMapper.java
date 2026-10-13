package com.xodud1202.springbackend.mapper;

import com.xodud1202.springbackend.domain.news.*;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface NewsDailyMapper {
    String COLUMNS = "YYYYMMDD, SEQ, REQUEST_ID, PAYLOAD_HASH, SOURCE_GENERATED_AT, ARTICLE_COUNT, REG_NO, "
            + "STATUS, FAILURE_CODE, REG_DT AS STARTED_AT, FINISHED_DT AS FINISHED_AT";

    @Insert("INSERT INTO news.NEWS_DAILY_SEQUENCE (YYYYMMDD, LAST_SEQ) VALUES (#{date}, 0) "
          + "ON DUPLICATE KEY UPDATE YYYYMMDD = VALUES(YYYYMMDD)")
    void ensureDate(String date);

    @Select("SELECT LAST_SEQ FROM news.NEWS_DAILY_SEQUENCE WHERE YYYYMMDD = #{date} FOR UPDATE")
    int lockDate(String date);

    @Select("SELECT COALESCE(MAX(SEQ), 0) FROM news.NEWS_DAILY WHERE YYYYMMDD = #{date}")
    int existingMax(String date);

    @Update("UPDATE news.NEWS_DAILY_SEQUENCE SET LAST_SEQ = #{seq} WHERE YYYYMMDD = #{date}")
    void advance(@Param("date") String date, @Param("seq") int seq);

    @Select("SELECT " + COLUMNS + " FROM news.NEWS_DAILY_BATCH WHERE YYYYMMDD = #{date} AND REQUEST_ID = #{requestId}")
    NewsDailyBatch find(@Param("date") String date, @Param("requestId") String requestId);

    @Select("SELECT " + COLUMNS + " FROM news.NEWS_DAILY_BATCH WHERE YYYYMMDD = #{date} AND REQUEST_ID = #{requestId} FOR UPDATE")
    NewsDailyBatch lockAttempt(@Param("date") String date, @Param("requestId") String requestId);

    @Insert("INSERT INTO news.NEWS_DAILY_BATCH "
          + "(YYYYMMDD, SEQ, REQUEST_ID, SOURCE_GENERATED_AT, ARTICLE_COUNT, REG_NO, STATUS) "
          + "VALUES (#{date}, #{seq}, #{requestId}, #{source}, 0, #{actor}, 'PENDING')")
    void reserve(@Param("date") String date, @Param("seq") int seq, @Param("requestId") String requestId,
                 @Param("source") String source, @Param("actor") Long actor);

    @Update("UPDATE news.NEWS_DAILY_BATCH SET STATUS='COMPLETED', PAYLOAD_HASH=#{hash}, "
          + "ARTICLE_COUNT=#{count}, FINISHED_DT=CURRENT_TIMESTAMP "
          + "WHERE YYYYMMDD=#{date} AND REQUEST_ID=#{requestId} AND STATUS='PENDING'")
    int complete(@Param("date") String date, @Param("requestId") String requestId,
                 @Param("hash") String hash, @Param("count") int count);

    @Update("UPDATE news.NEWS_DAILY_BATCH SET STATUS='FAILED', FAILURE_CODE=#{code}, FINISHED_DT=CURRENT_TIMESTAMP "
          + "WHERE YYYYMMDD=#{date} AND REQUEST_ID=#{requestId} AND STATUS='PENDING'")
    int fail(@Param("date") String date, @Param("requestId") String requestId, @Param("code") String code);

    @Insert("INSERT INTO news.NEWS_DAILY "
          + "(YYYYMMDD, SEQ, CATEGORY_NM, DISP_ORD, NEWS_CONTENT, REG_NO, UDT_NO) "
          + "VALUES (#{date}, #{seq}, #{article.categoryNm}, #{article.dispOrd}, "
          + "#{article.newsContent}, #{actor}, #{actor})")
    void insertArticle(@Param("date") String date, @Param("seq") int seq,
                       @Param("article") NewsDailyArticle article, @Param("actor") Long actor);

    @Select("SELECT " + COLUMNS + " FROM news.NEWS_DAILY_BATCH B "
          + "WHERE B.YYYYMMDD = #{date} AND B.STATUS='COMPLETED' "
          + "AND B.ARTICLE_COUNT = (SELECT COUNT(*) FROM news.NEWS_DAILY D WHERE D.YYYYMMDD=B.YYYYMMDD AND D.SEQ=B.SEQ) "
          + "ORDER BY B.SEQ DESC LIMIT 1")
    NewsDailyBatch latest(String date);

    @Select("SELECT CATEGORY_NM, DISP_ORD, NEWS_CONTENT FROM news.NEWS_DAILY "
          + "WHERE YYYYMMDD = #{date} AND SEQ = #{seq} ORDER BY DISP_ORD")
    List<NewsDailyArticle> articles(@Param("date") String date, @Param("seq") int seq);
}


