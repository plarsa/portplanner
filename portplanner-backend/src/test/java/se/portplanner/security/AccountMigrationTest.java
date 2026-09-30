package se.portplanner.security;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** V13 ska översätta gamla enkelroller till rolltilldelningar och ge alla konton metoden PASSWORD. */
class AccountMigrationTest {

    @Test
    void legacyRolesBecomeGrantsAndAccountsGetPasswordMethod() {
        var ds = new DriverManagerDataSource(
                "jdbc:h2:mem:migtest;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", "");
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").target("12").load().migrate();

        var jdbc = new JdbcTemplate(ds);
        jdbc.update("INSERT INTO app_users (username, password_hash, role) VALUES ('a', 'x', 'ADMIN')");
        jdbc.update("INSERT INTO app_users (username, password_hash, role) VALUES ('h', 'x', 'HARBOUR_MASTER')");
        jdbc.update("INSERT INTO app_users (username, password_hash, role) VALUES ('m', 'x', 'MEMBER')");

        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();

        List<Map<String, Object>> roles = jdbc.queryForList(
                "SELECT u.username, r.role_name, r.access_level FROM app_users u JOIN app_user_roles r ON r.user_id = u.id ORDER BY u.username");
        assertEquals(List.of(
                Map.of("username", "a", "role_name", "admin", "access_level", "RW"),
                Map.of("username", "h", "role_name", "admin", "access_level", "RW"),
                Map.of("username", "m", "role_name", "member", "access_level", "RW")), roles);

        assertEquals(3, jdbc.queryForObject(
                "SELECT COUNT(*) FROM app_user_login_methods WHERE method = 'PASSWORD'", Integer.class));
        assertEquals(3, jdbc.queryForObject(
                "SELECT COUNT(*) FROM app_users WHERE status = 'ACTIVE' AND email IS NULL", Integer.class));
    }
}
