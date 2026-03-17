package com.sentinel.cloud_optimizer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifica se o contexto Spring sobe sem erros.
 * Usa o perfil "test" para conectar ao H2 em vez do PostgreSQL.
 */
@SpringBootTest
@ActiveProfiles("test")
class CloudOptimizerApplicationTests {

    @Test
    void contextLoads() {
        // Se o contexto falhar ao inicializar, este teste falha automaticamente.
        // Útil para detectar problemas de configuração, beans faltando, etc.
    }
}
