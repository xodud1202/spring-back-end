package com.xodud1202.springbackend.mapper;

import com.xodud1202.springbackend.domain.work.reading.ReadingRecordRequest;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ReadingRecordMapperTests {
	@Test
	void persistsKoreanTextAndHalfPointAndSeparatesOwners() throws Exception {
		JdbcDataSource dataSource = new JdbcDataSource();
		dataSource.setURL("jdbc:h2:mem:reading-records;MODE=MySQL;DB_CLOSE_DELAY=-1");
		try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
			statement.execute(Files.readString(Path.of("docs/work-reading-record-schema.sql")));
		}
		Configuration config = new Configuration(new Environment("test", new JdbcTransactionFactory(), dataSource));
		String resource = "mapper/ReadingRecordMapper.xml";
		try (var stream = getClass().getClassLoader().getResourceAsStream(resource)) {
			new XMLMapperBuilder(stream, config, resource, config.getSqlFragments()).parse();
		}
		try (var session = new SqlSessionFactoryBuilder().build(config).openSession()) {
			var mapper = session.getMapper(ReadingRecordMapper.class);
			var date = LocalDate.of(2026, 11, 19);
			var content = "가".repeat(200);
			mapper.insert(1L, new ReadingRecordRequest("책", "작가", "역자", date, date, content, new BigDecimal("4.5"), "소감", null, null));
			mapper.insert(2L, new ReadingRecordRequest("다른 독서", "다른 작가", null, null, null, null, null, null, null, null));
			var rows = mapper.selectByOwner(1L, null, null, null, null, null);
			assertEquals(1, rows.size());
			assertNotNull(rows.getFirst().id());
			assertEquals("책", rows.getFirst().title());
			assertEquals(1, rows.getFirst().readingRound());
			assertFalse(rows.getFirst().reread());
			assertEquals(date, rows.getFirst().startedOn());
			assertEquals(content, rows.getFirst().content());
			assertEquals(new BigDecimal("4.5"), rows.getFirst().rating());
			assertEquals("다른 독서", mapper.selectByOwner(2L, null, null, null, null, null).getFirst().title());
			assertTrue(mapper.selectByOwner(3L, null, null, null, null, null).isEmpty());
			long id = rows.getFirst().id();
			assertNull(mapper.selectOne(2L, id));
			assertEquals(0, mapper.delete(2L, id));
			var changed = new ReadingRecordRequest("50%_책", "AUTHOR", null, date.minusDays(3), date, content, null, null, BigDecimal.TWO, true);
			assertEquals(0, mapper.update(2L, id, changed));
			assertEquals(1, mapper.update(1L, id, changed));
			assertEquals("50%_책", mapper.selectOne(1L, id).title());
			assertEquals(2, mapper.selectOne(1L, id).readingRound());
			assertTrue(mapper.selectOne(1L, id).reread());
			assertEquals(1, mapper.selectByOwner(1L, "%_", "author", date, date, true).size());
			assertTrue(mapper.selectByOwner(2L, null, null, null, null, true).isEmpty());
			assertEquals(1, mapper.selectByOwner(2L, null, null, null, null, false).size());
			var legacyUpdate = new ReadingRecordRequest("50%_책", "AUTHOR", null, date.minusDays(3), date, content, null, null, null, null);
			mapper.update(1L, id, legacyUpdate);
			assertEquals(2, mapper.selectOne(1L, id).readingRound());
			assertTrue(mapper.selectOne(1L, id).reread());
			var clearFlag = new ReadingRecordRequest("50%_책", "AUTHOR", null, date.minusDays(3), date, content, null, null, null, false);
			mapper.update(1L, id, clearFlag);
			assertFalse(mapper.selectOne(1L, id).reread());
			assertTrue(mapper.selectByOwner(1L, null, null, null, null, true).isEmpty());
			assertEquals(1, mapper.selectByOwner(1L, "%_", "author", date, date, null).size());
			assertTrue(mapper.selectByOwner(1L, "missing%", null, null, null, null).isEmpty());
			assertTrue(mapper.selectByOwner(1L, null, null, date.plusDays(1), null, null).isEmpty());
			assertTrue(mapper.selectByOwner(2L, null, null, date, null, null).isEmpty());
			mapper.insert(1L, new ReadingRecordRequest("진행 중", "작가", null, date, null, null, null, null, null, null));
			mapper.insert(1L, new ReadingRecordRequest("완료만", "작가", null, null, date, null, null, null, null, null));
			assertEquals(3, mapper.selectByOwner(1L, null, null, date, date, null).size());
			assertEquals(1, mapper.selectByOwner(1L, null, null, date.plusDays(1), null, null).size());
			assertEquals(1, mapper.delete(1L, id));
			assertNull(mapper.selectOne(1L, id));

		}
	}
}