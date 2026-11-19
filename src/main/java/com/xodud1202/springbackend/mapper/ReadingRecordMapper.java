package com.xodud1202.springbackend.mapper;

import com.xodud1202.springbackend.domain.work.reading.ReadingRecord;
import com.xodud1202.springbackend.domain.work.reading.ReadingRecordRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface ReadingRecordMapper {
	List<ReadingRecord> selectByOwner(@Param("owner") Long owner, @Param("title") String title,
		@Param("author") String author, @Param("from") LocalDate from, @Param("to") LocalDate to, @Param("reread") Boolean reread);

	ReadingRecord selectOne(@Param("owner") Long owner, @Param("id") long id);

	int update(@Param("owner") Long owner, @Param("id") long id, @Param("record") ReadingRecordRequest record);

	int delete(@Param("owner") Long owner, @Param("id") long id);

	int insert(@Param("owner") Long owner, @Param("record") ReadingRecordRequest record);
}