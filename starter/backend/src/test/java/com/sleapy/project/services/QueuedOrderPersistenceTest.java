package com.sleapy.project.services;

import com.sleapy.project.mappers.OrderMapper;
import com.sleapy.project.mappers.OrderStatusMapper;
import com.sleapy.project.models.entities.*;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class QueuedOrderPersistenceTest {
    private SqlSessionFactory factory;

    @BeforeEach
    void database() throws Exception {
        var dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        var jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("""
            CREATE TABLE Order_Status (OrderStatus_ID SERIAL PRIMARY KEY, OrderStatus_Name VARCHAR(100));
            CREATE TABLE Clients (Client_ID BIGINT PRIMARY KEY, email VARCHAR(100), cash_balance NUMERIC,
                password_hash VARCHAR(100), street_address VARCHAR(100), phone_number VARCHAR(100), ssn VARCHAR(100));
            CREATE TABLE Instruments (Instrument_ID BIGINT PRIMARY KEY, Symbol VARCHAR(20), Symbol_Name VARCHAR(100));
            CREATE TABLE Orders (Order_ID SERIAL PRIMARY KEY, Client_ID BIGINT REFERENCES Clients(Client_ID),
                Instrument_ID BIGINT REFERENCES Instruments(Instrument_ID), Quantity NUMERIC(15,4),
                Time_purchased TIMESTAMP, Time_filled TIMESTAMP, Purchase_Price NUMERIC(15,2),
                trade_type VARCHAR(4), Status BIGINT REFERENCES Order_Status(OrderStatus_ID));
            INSERT INTO Clients (Client_ID) VALUES (1);
            INSERT INTO Instruments VALUES (2, 'AAPL', 'Apple');
            INSERT INTO Order_Status (OrderStatus_Name) VALUES ('Pending'), ('Filled');
            INSERT INTO Orders (Client_ID, Instrument_ID, Quantity, trade_type, Status)
                VALUES (1, 2, 1, 'BUY', 1);
            """);
        var bean = new SqlSessionFactoryBean();
        bean.setDataSource(dataSource);
        bean.setTransactionFactory(new org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory());
        bean.setMapperLocations(new ClassPathResource("mappers/OrderMapper.xml"));
        factory = bean.getObject();
        factory.getConfiguration().addMapper(OrderStatusMapper.class);
    }

    @Test
    void queueSurvivesNewSessionAndCannotBeClaimedTwice() {
        Long id;
        try (var session = factory.openSession()) {
            var statuses = session.getMapper(OrderStatusMapper.class);
            statuses.ensureStatus("Queued");
            statuses.ensureStatus("Queued");
            OrderEntity order = new OrderEntity();
            ClientEntity client = new ClientEntity();
            client.setId(1L);
            InstrumentEntity instrument = new InstrumentEntity();
            instrument.setId(2L);
            order.setClient(client);
            order.setInstrument(instrument);
            order.setQuantity(BigDecimal.ONE);
            order.setTimeOfPurchase(LocalDate.now());
            order.setTradeType("BUY");
            order.setStatus(statuses.findByName("Queued").orElseThrow());
            session.getMapper(OrderMapper.class).save(order);
            id = order.getId();
            session.commit();
        }
        try (var first = factory.openSession(); var second = factory.openSession()) {
            var orders = first.getMapper(OrderMapper.class);
            assertEquals(java.util.List.of(id), orders.findQueuedIds());
            assertEquals(id, orders.lockQueued(id));
            assertNull(second.getMapper(OrderMapper.class).lockQueued(id));
            var order = orders.findById(id);
            assertEquals("BUY", order.getTradeType());
            assertNull(order.getTimeFilled());
            assertNull(order.getPurchasePrice());
            order.setStatus(first.getMapper(OrderStatusMapper.class).findByName("Filled").orElseThrow());
            order.setPurchasePrice(new BigDecimal("120"));
            order.setTimeFilled(LocalDate.now());
            orders.update(order);
            first.commit();
        }
        try (var session = factory.openSession()) {
            var orders = session.getMapper(OrderMapper.class);
            assertTrue(orders.findQueuedIds().isEmpty());
            assertNull(orders.lockQueued(id));
            assertEquals(2, orders.findAll().size());
            assertEquals(new BigDecimal("120.00"), orders.findById(id).getPurchasePrice());
            assertEquals("BUY", orders.findTransactionsByClientId(1L).get(0).getTradeType());
        }
    }
}
