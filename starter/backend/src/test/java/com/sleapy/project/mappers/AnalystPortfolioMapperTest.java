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
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AnalystPortfolioMapperTest {
    private SqlSessionFactory factory;
    private JdbcTemplate jdbc;
    private final LocalDateTime first = LocalDateTime.parse("2026-10-22T12:00:00");

    @BeforeEach
    void setUp() throws Exception {
        var ds = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID()
                + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(ds);
        var migration = new ResourceDatabasePopulator(
                new ClassPathResource("db/migrations/004-add-portfolio-snapshots.sql"));
        migration.execute(ds);
        migration.execute(ds);
        jdbc.execute("CREATE TABLE clients (client_id BIGINT, cash_balance NUMERIC, is_admin BOOLEAN, is_analyst BOOLEAN)");
        jdbc.execute("CREATE TABLE holdings (client_id BIGINT, instrument_id BIGINT, quantity_shares NUMERIC)");
        jdbc.execute("CREATE TABLE instruments (instrument_id BIGINT, symbol TEXT, instrumenttype_id BIGINT)");
        jdbc.execute("CREATE TABLE instrument_type (instrumenttype_id BIGINT, instrumenttype TEXT)");
        jdbc.execute("CREATE TABLE market_prices (symbol TEXT, current_price NUMERIC)");
        jdbc.update("INSERT INTO clients VALUES (1, 100, FALSE, FALSE), (2, 200, NULL, NULL), "
                + "(3, 999, TRUE, FALSE), (4, 999, FALSE, TRUE), (5, 300, FALSE, FALSE)");
        jdbc.update("INSERT INTO instrument_type VALUES (1, 'Stock'), (2, 'ETF')");
        jdbc.update("INSERT INTO instruments VALUES (1, 'AAPL', 1), (2, 'SPY', 2), (3, 'MISSING', 1)");
        jdbc.update("INSERT INTO market_prices VALUES ('AAPL', 10), ('SPY', 20)");
        jdbc.update("INSERT INTO holdings VALUES (1, 1, 2), (1, 2, 3), (2, 3, 1), (3, 1, 100), (4, 1, 100)");
        var config = new Configuration(new Environment("test", new JdbcTransactionFactory(), ds));
        config.setMapUnderscoreToCamelCase(false);
        try (var stream = new ClassPathResource("mappers/AnalystPortfolioMapper.xml").getInputStream()) {
            new XMLMapperBuilder(stream, config, "mappers/AnalystPortfolioMapper.xml",
                    config.getSqlFragments()).parse();
        }
        factory = new SqlSessionFactoryBuilder().build(config);
    }

    @Test
    void capturesCustomersOnlyIncludingCashOnlyPortfoliosAndMissingPrices() {
        try (var session = factory.openSession(true)) {
            var mapper = session.getMapper(AnalystPortfolioMapper.class);
            assertEquals(4, mapper.capture(first));
            var rows = mapper.findSnapshot(null, first.minusDays(1), first.plusDays(1), true);
            assertEquals(4, rows.size());
            assertEquals(3, rows.stream().map(row -> row.getClientId()).distinct().count());
            var customer = mapper.findSnapshot(1L, first, first, true);
            assertEquals(2, customer.size());
            assertEquals(0, new BigDecimal("80").compareTo(customer.stream()
                    .map(row -> row.getHoldingsValue()).reduce(BigDecimal.ZERO, BigDecimal::add)));
            assertTrue(customer.stream().allMatch(row -> row.getCashBalance().compareTo(new BigDecimal("100")) == 0));
            assertEquals(1, mapper.findSnapshot(2L, first, first, true).getFirst().getMissingPrices());
            assertEquals(0, mapper.findSnapshot(5L, first, first, true).getFirst().getHoldingsValue().signum());
            assertTrue(mapper.findSnapshot(3L, first, first, true).isEmpty());
            assertTrue(mapper.findSnapshot(4L, first, first, true).isEmpty());
            assertEquals(first, mapper.findHistorySince(1L));
            assertNull(mapper.findHistorySince(3L));
        }
    }

    @Test
    void persistsImmutableHistoryAndSelectsFirstAndLatestWithinTheCalendarRange() {
        try (var session = factory.openSession(true)) {
            var mapper = session.getMapper(AnalystPortfolioMapper.class);
            mapper.capture(first);
            jdbc.update("UPDATE market_prices SET current_price = 15 WHERE symbol = 'AAPL'");
            jdbc.update("UPDATE clients SET cash_balance = 150 WHERE client_id = 1");
            var second = first.plusDays(1);
            mapper.capture(second);
        }
        try (var session = factory.openSession()) {
            var mapper = session.getMapper(AnalystPortfolioMapper.class);
            var old = mapper.findSnapshot(1L, first, first.plusDays(2), false);
            var latest = mapper.findSnapshot(1L, first, first.plusDays(2), true);
            assertEquals(first, old.getFirst().getSnapshotAt());
            assertEquals(first.plusDays(1), latest.getFirst().getSnapshotAt());
            assertEquals(0, new BigDecimal("100").compareTo(old.getFirst().getCashBalance()));
            assertEquals(0, new BigDecimal("150").compareTo(latest.getFirst().getCashBalance()));
            assertEquals(0, new BigDecimal("80").compareTo(old.stream()
                    .map(row -> row.getHoldingsValue()).reduce(BigDecimal.ZERO, BigDecimal::add)));
            assertEquals(0, new BigDecimal("90").compareTo(latest.stream()
                    .map(row -> row.getHoldingsValue()).reduce(BigDecimal.ZERO, BigDecimal::add)));
            assertTrue(mapper.findSnapshot(null, first.minusDays(3), first.minusDays(1), true).isEmpty());
            assertTrue(mapper.findSnapshot(99L, first, first.plusDays(2), true).isEmpty());
            assertEquals(first, mapper.findHistorySince(null));
        }
    }

    @Test
    void multipleCustomerSelectionRestrictsSnapshotRowsAndExcludesPrivilegedAccounts() {
        try (var session = factory.openSession(true)) {
            var mapper = session.getMapper(AnalystPortfolioMapper.class);
            mapper.capture(first);
            var ids = java.util.List.of(1L, 5L, 1L, 3L, 4L);
            var rows = mapper.findSnapshotForClients(ids, first, first.plusDays(1), true);
            assertEquals(3, rows.size());
            assertEquals(java.util.Set.of(1L, 5L), rows.stream()
                    .map(row -> row.getClientId()).collect(java.util.stream.Collectors.toSet()));
            assertEquals(first, mapper.findHistorySinceForClients(ids));
            assertTrue(mapper.findSnapshotForClients(java.util.List.of(3L, 4L), first, first, true).isEmpty());
        }
    }

    @Test
    void snapshotsRollBackAndCurrentPrivilegedAccountsRemainExcludedFromHistory() {
        try (var session = factory.openSession()) {
            session.getMapper(AnalystPortfolioMapper.class).capture(first);
            session.rollback();
        }
        try (var session = factory.openSession(true)) {
            var mapper = session.getMapper(AnalystPortfolioMapper.class);
            assertNull(mapper.findHistorySince(null));
            mapper.capture(first);
            jdbc.update("UPDATE clients SET is_analyst = TRUE WHERE client_id = 1");
            assertTrue(mapper.findSnapshot(1L, first, first.plusDays(1), true).isEmpty());
            assertNull(mapper.findHistorySince(1L));
            assertEquals(2, mapper.findSnapshot(null, first, first.plusDays(1), true).size());
        }
    }
}
