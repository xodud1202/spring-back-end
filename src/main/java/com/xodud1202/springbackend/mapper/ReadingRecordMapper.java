package com.xodud1202.springbackend.mapper;

import com.xodud1202.springbackend.domain.work.reading.ReadingRecord;
import com.xodud1202.springbackend.domain.work.reading.ReadingRecordRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReadingRecordMapper {
	List<ReadingRecord> selectByOwner(@Param("owner") Long owner);

	int insert(@Param("owner") Long owner, @Param("record") ReadingRecordRequest record);
}