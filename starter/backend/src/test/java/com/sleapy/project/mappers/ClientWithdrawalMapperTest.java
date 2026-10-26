package com.sleapy.project.mappers;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientWithdrawalMapperTest {
    @Test
    void withdrawalsPersistCentsAllowFullBalanceAndNeverOverdraw() throws Exception {
        var dataSource = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID()
                + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        var jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE Clients (Client_ID BIGINT PRIMARY KEY, cash_balance NUMERIC(15,2))");
        jdbc.update("INSERT INTO Clients VALUES (1, 125.50), (2, 75.00)");
        var configuration = new Configuration(
                new Environment("test", new JdbcTransactionFactory(), dataSource));
        try (var stream = new ClassPathResource("mappers/ClientMapper.xml").getInputStream()) {
            new XMLMapperBuilder(stream, configuration, "mappers/ClientMapper.xml",
                    configuration.getSqlFragments()).parse();
        }
        var factory = new SqlSessionFactoryBuilder().build(configuration);
        try (var session = factory.openSession()) {
            var mapper = session.getMapper(ClientMapper.class);
            assertEquals(1, mapper.withdrawCash(1L, new BigDecimal("25.51")));
            assertEquals(0, mapper.withdrawCash(1L, new BigDecimal("100")));
            assertEquals(1, mapper.withdrawCash(1L, new BigDecimal("99.99")));
            assertEquals(0, mapper.withdrawCash(1L, new BigDecimal("0.01")));
            session.commit();
        }
        assertEquals(new BigDecimal("0.00"),
                jdbc.queryForObject("SELECT cash_balance FROM Clients WHERE Client_ID = 1", BigDecimal.class));
        assertEquals(new BigDecimal("75.00"),
                jdbc.queryForObject("SELECT cash_balance FROM Clients WHERE Client_ID = 2", BigDecimal.class));
    }
}
