package com.reloop;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthAndSecurityTest extends TestSupport {

    @Test
    void registerThenLoginReturnsTokenAndIgnoresClientRole() throws Exception {
        String email = uniqueEmail("reg");
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"fullName\":\"Asha\",\"email\":\"" + email + "\",\"password\":\"Passw0rd!\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("CUSTOMER"))
                .andExpect(cookie().httpOnly("reloop_refresh", true));
        assertTrue(users.findByEmailIgnoreCase(email).orElseThrow().getPasswordHash().startsWith("$2"), "BCrypt hash expected");
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"Passw0rd!\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void duplicateEmailAndWrongPasswordAreRejected() throws Exception {
        String email = uniqueEmail("dup");
        createUser(email, com.reloop.entity.Enums.Role.CUSTOMER);
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"fullName\":\"X\",\"email\":\"" + email + "\",\"password\":\"Passw0rd!\"}")).andExpect(status().isConflict());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"wrong-password\"}")).andExpect(status().isUnauthorized());
    }

    @Test
    void validationErrorsReturn400WithFieldErrors() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"fullName\":\"\",\"email\":\"not-an-email\",\"password\":\"123\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    void protectedAndAdminRoutesRejectUnauthorisedCallers() throws Exception {
        mvc.perform(get("/api/cart")).andExpect(status().isUnauthorized());
        String customer = login(createUser(uniqueEmail("cust"), com.reloop.entity.Enums.Role.CUSTOMER).getEmail());
        mvc.perform(get("/api/admin/stats").header("Authorization", customer)).andExpect(status().isForbidden());
        mvc.perform(get("/api/partner/repairs").header("Authorization", customer)).andExpect(status().isForbidden());
        String admin = login(createUser(uniqueEmail("adm"), com.reloop.entity.Enums.Role.ADMIN).getEmail());
        mvc.perform(get("/api/admin/stats").header("Authorization", admin)).andExpect(status().isOk());
    }

    @Test
    void tamperedTokenIsRejected() throws Exception {
        String t = login(createUser(uniqueEmail("tam"), com.reloop.entity.Enums.Role.CUSTOMER).getEmail());
        mvc.perform(get("/api/cart").header("Authorization", t + "x")).andExpect(status().isUnauthorized());
    }
}
