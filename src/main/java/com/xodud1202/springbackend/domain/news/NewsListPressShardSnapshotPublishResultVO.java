package com.xodud1202.springbackend.domain.news;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
// 뉴스 메타+언론사 shard 스냅샷 업로드 결과 정보를 보관합니다.
public class NewsListPressShardSnapshotPublishResultVO {
	private String baseTargetPath;
	private String metaFileName;
	private Integer pressShardCount;
	private Integer shardSuccessCount;
	private Integer shardFailedCount;
	private Integer metaJsonByteSize;
	private Integer totalShardJsonByteSize;
}
