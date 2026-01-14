package com.survey.integration;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Base para testes de integração.
 *
 * Importante: as entidades principais usam soft delete (deleted_at) e/ou @SQLDelete,
 * então repository.deleteAll() não garante remoção física e pode quebrar testes por
 * restrições únicas (username/titulo). Aqui fazemos limpeza física via SQL.
 */
abstract class AbstractIntegrationTest {

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @BeforeEach
    void hardCleanDatabase() {
        // Ordem importa por FKs
        jdbcTemplate.execute("DELETE FROM votes");
        jdbcTemplate.execute("DELETE FROM response_sessions");
        jdbcTemplate.execute("DELETE FROM options");
        jdbcTemplate.execute("DELETE FROM questions");
        jdbcTemplate.execute("DELETE FROM surveys");
        jdbcTemplate.execute("DELETE FROM users");
    }
}

