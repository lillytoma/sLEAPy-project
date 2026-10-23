package com.sleapy.project.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnalystRoleMigrationTest {
    @Test
    void upgradesLegacyClientsWithoutChangingExistingData() {
        DriverManagerDataSource dataSource = dataSource();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE public.clients (client_id BIGINT PRIMARY KEY, username VARCHAR(25))");
        jdbc.update("INSERT INTO public.clients VALUES (1, 'existing-client')");

        migration().execute(dataSource);

        assertEquals("existing-client",
                jdbc.queryForObject("SELECT username FROM public.clients WHERE client_id = 1", String.class));
        assertEquals(false,
                jdbc.queryForObject("SELECT is_analyst FROM public.clients WHERE client_id = 1", Boolean.class));
        jdbc.update("INSERT INTO public.clients (client_id, username) VALUES (2, 'new-client')");
        assertEquals(false,
                jdbc.queryForObject("SELECT is_analyst FROM public.clients WHERE client_id = 2", Boolean.class));
    }

    @Test
    void rerunningMigrationPreservesAnalystAccess() {
        DriverManagerDataSource dataSource = dataSource();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE public.clients (client_id BIGINT PRIMARY KEY)");
        migration().execute(dataSource);
        jdbc.update("INSERT INTO public.clients (client_id, is_analyst) VALUES (1, TRUE)");

        migration().execute(dataSource);

        assertEquals(true,
                jdbc.queryForObject("SELECT is_analyst FROM public.clients WHERE client_id = 1", Boolean.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM public.clients", Integer.class));
    }

    @Test
    void upgradesLegacyProfileWithoutOverwritingExistingValues() {
        DriverManagerDataSource dataSource = dataSource();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE public.clients (client_id BIGINT PRIMARY KEY, address VARCHAR(255) NOT NULL)");
        jdbc.update("INSERT INTO public.clients VALUES (1, 'Existing address')");
        ResourceDatabasePopulator profileMigration = new ResourceDatabasePopulator(
                new ClassPathResource("db/migrations/002-add-client-profile.sql"));

        profileMigration.execute(dataSource);
        jdbc.update("UPDATE public.clients SET first_name = 'Existing name' WHERE client_id = 1");
        profileMigration.execute(dataSource);

        assertEquals("Existing address",
                jdbc.queryForObject("SELECT address FROM public.clients WHERE client_id = 1", String.class));
        assertEquals("Existing name",
                jdbc.queryForObject("SELECT first_name FROM public.clients WHERE client_id = 1", String.class));
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM public.clients WHERE last_name IS NULL AND dob IS NULL "
                        + "AND street_address IS NULL AND city IS NULL AND state_name IS NULL "
                        + "AND zip_code IS NULL AND region IS NULL", Integer.class));
    }

    private DriverManagerDataSource dataSource() {
        return new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID()
                + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
    }

    private ResourceDatabasePopulator migration() {
        return new ResourceDatabasePopulator(
                new ClassPathResource("db/migrations/001-add-analyst-role.sql"));
    }
}
