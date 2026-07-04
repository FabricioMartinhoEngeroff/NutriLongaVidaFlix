package com.dvFabricio.VidaLongaFlix.integration.security;

import com.dvFabricio.VidaLongaFlix.integration.base.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Regressão: POST /auth/register retornava 403 Forbidden para usuários novos
 * que nunca tinham feito GET na API (cookie XSRF-TOKEN não existia ainda).
 *
 * Raiz do problema: CSRF é necessário para endpoints autenticados, mas register,
 * login e recuperação de senha são acessados ANTES de qualquer sessão existir —
 * não há cookie de sessão que um atacante possa explorar via CSRF.
 *
 * Correção: endpoints públicos de auth adicionados ao ignoringRequestMatchers
 * no SecurityConfig. Logout permanece protegido (Logout CSRF é um ataque real).
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class CsrfPublicAuthExemptionIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("POST /auth/register sem CSRF não deve retornar 403")
    void registerSemCsrfNaoDeveRetornar403() throws Exception {
        String body = """
                {
                  "name": "Teste CSRF",
                  "email": "csrf-test-register@vidalongaflix.com",
                  "password": "Teste@1234",
                  "phone": "(11) 99999-9999"
                }""";

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(result -> assertNotEquals(403,
                        result.getResponse().getStatus(),
                        "POST /auth/register foi bloqueado por CSRF — endpoint público não deve exigir token CSRF"));
    }

    @Test
    @DisplayName("POST /auth/login sem CSRF não deve retornar 403")
    void loginSemCsrfNaoDeveRetornar403() throws Exception {
        String body = """
                {"email":"admin@vidalongaflix.com","password":"AdminTest@123"}""";

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(result -> assertNotEquals(403,
                        result.getResponse().getStatus(),
                        "POST /auth/login foi bloqueado por CSRF — endpoint público não deve exigir token CSRF"));
    }

    @Test
    @DisplayName("POST /auth/password-recovery sem CSRF não deve retornar 403")
    void passwordRecoverySemCsrfNaoDeveRetornar403() throws Exception {
        String body = """
                {"email":"admin@vidalongaflix.com"}""";

        mockMvc.perform(post("/auth/password-recovery")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(result -> assertNotEquals(403,
                        result.getResponse().getStatus(),
                        "POST /auth/password-recovery foi bloqueado por CSRF — endpoint público não deve exigir token CSRF"));
    }

    @Test
    @DisplayName("POST /auth/reset-password sem CSRF não deve retornar 403")
    void resetPasswordSemCsrfNaoDeveRetornar403() throws Exception {
        String body = """
                {"token":"token-invalido","newPassword":"NovaSenha@123"}""";

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(result -> assertNotEquals(403,
                        result.getResponse().getStatus(),
                        "POST /auth/reset-password foi bloqueado por CSRF — endpoint público não deve exigir token CSRF"));
    }
}
