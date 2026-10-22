package com.sleapy.project.mappers;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AnalystMapperTest {
    private SqlSessionFactory sessionFactory;

    @BeforeEach
    void setUp() throws IOException {
        var dataSource = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID()
                + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        var jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE SCHEMA sleapy_analytics");
        jdbc.execute("CREATE TABLE sleapy_analytics.expensive_trades "
                + "(client_id BIGINT, symbol TEXT, symbol_name TEXT, total_value TEXT, time_purchased TEXT)");
        jdbc.execute("CREATE TABLE public.instrument_type (instrumenttype_id BIGINT, instrumenttype TEXT)");
        jdbc.execute("CREATE TABLE public.instruments (symbol TEXT, instrumenttype_id BIGINT)");
        jdbc.update("INSERT INTO public.instrument_type VALUES (1, 'Stock')");
        jdbc.update("INSERT INTO public.instruments VALUES ('AAPL', 1)");
        jdbc.update("INSERT INTO sleapy_analytics.expensive_trades VALUES "
                + "(1, 'AAPL', 'Apple', '100.50', '2026-10-01 12:00:00'), "
                + "(2, 'AAPL', 'Apple', '200.25', '2026-10-02 12:00:00'), "
                + "(1, 'UNKNOWN', 'Unlisted instrument', '50.00', '2026-10-03 12:00:00')");

        var configuration = new Configuration(
                new Environment("test", new JdbcTransactionFactory(), dataSource));
        configuration.setMapUnderscoreToCamelCase(false);
        try (var stream = new ClassPathResource("mappers/AnalystMapper.xml").getInputStream()) {
            new XMLMapperBuilder(stream, configuration, "mappers/AnalystMapper.xml",
                    configuration.getSqlFragments()).parse();
        }
        sessionFactory = new SqlSessionFactoryBuilder().build(configuration);
    }

    @Test
    void summaryQueriesWorkWithEtlSchemaWithoutInstrumentTypeColumn() {
        try (var session = sessionFactory.openSession()) {
            var mapper = session.getMapper(AnalystMapper.class);
            assertEquals(3, mapper.countTrades(null));
            assertEquals(0, new BigDecimal("350.75").compareTo(mapper.findTradeVolume(null)));
            assertNotNull(mapper.findLatestTradeTime(null));

            var instruments = mapper.findMostActiveInstruments(null);
            assertEquals(2, instruments.size());
            assertEquals("AAPL", instruments.getFirst().getSymbol());
            assertEquals("Apple", instruments.getFirst().getSymbolName());
            assertEquals("Stock", instruments.getFirst().getInstrumentType());
            assertEquals(2, instruments.getFirst().getTotalTrades());
            assertEquals(0, new BigDecimal("300.75").compareTo(instruments.getFirst().getTradeVolume()));
            assertEquals("Other", instruments.get(1).getInstrumentType());
            assertEquals("Unlisted instrument", instruments.get(1).getSymbolName());
        }
    }

    @Test
    void clientFilterKeepsUnmatchedInstruments() {
        try (var session = sessionFactory.openSession()) {
            var mapper = session.getMapper(AnalystMapper.class);
            var instruments = mapper.findMostActiveInstruments(1L);
            assertEquals(2, mapper.countTrades(1L));
            assertEquals(0, new BigDecimal("150.50").compareTo(mapper.findTradeVolume(1L)));
            assertEquals(2, instruments.size());
            assertEquals(1, instruments.getFirst().getTotalTrades());
            assertEquals(0, new BigDecimal("100.50").compareTo(instruments.getFirst().getTradeVolume()));
            assertEquals("Other", instruments.get(1).getInstrumentType());
        }
    }

    @Test
    void unknownClientHasEmptySummary() {
        try (var session = sessionFactory.openSession()) {
            var mapper = session.getMapper(AnalystMapper.class);
            assertEquals(0, mapper.countTrades(999L));
            assertEquals(0, BigDecimal.ZERO.compareTo(mapper.findTradeVolume(999L)));
            assertNull(mapper.findLatestTradeTime(999L));
            assertTrue(mapper.findMostActiveInstruments(999L).isEmpty());
        }
    }
}
