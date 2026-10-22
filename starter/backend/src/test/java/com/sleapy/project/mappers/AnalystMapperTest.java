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
        jdbc.execute("CREATE TABLE public.clients "
                + "(client_id BIGINT, username TEXT, email TEXT, is_admin BOOLEAN, is_analyst BOOLEAN)");
        jdbc.update("INSERT INTO public.clients VALUES "
                + "(1, 'customer-one', 'first@example.com', FALSE, FALSE), "
                + "(2, 'second', 'customer-two@example.com', FALSE, FALSE), "
                + "(3, 'customer-admin', 'admin@example.com', TRUE, FALSE), "
                + "(4, 'admin', 'customer-admin@example.com', TRUE, FALSE), "
                + "(5, 'customer-analyst', 'analyst@example.com', FALSE, TRUE), "
                + "(6, 'analyst', 'customer-analyst@example.com', FALSE, TRUE), "
                + "(7, 'customer-both', 'customer-both@example.com', TRUE, TRUE), "
                + "(8, 'customer-legacy', 'legacy@example.com', NULL, NULL), "
                + "(9, 'customer-null-admin', 'null-admin@example.com', NULL, TRUE), "
                + "(10, 'customer-null-analyst', 'null-analyst@example.com', TRUE, NULL)");
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
    void searchExcludesPrivilegedUsersForBothUsernameAndEmailMatches() {
        try (var session = sessionFactory.openSession()) {
            var results = session.getMapper(AnalystMapper.class).searchClients("CuStOmEr");
            assertEquals(java.util.List.of(8L, 1L, 2L),
                    results.stream().map(client -> client.getId()).toList());
            assertEquals("customer-legacy", results.getFirst().getUsername());
            assertEquals("legacy@example.com", results.getFirst().getEmail());
        }
    }

    @Test
    void exactPrivilegedEmailOrUsernameReturnsNoResults() {
        try (var session = sessionFactory.openSession()) {
            var mapper = session.getMapper(AnalystMapper.class);
            for (String search : java.util.List.of("admin", "analyst", "customer-both")) {
                assertTrue(mapper.searchClients(search).isEmpty(), search);
            }
        }
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
