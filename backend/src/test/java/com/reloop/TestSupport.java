package com.reloop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reloop.entity.*;
import com.reloop.entity.Enums.*;
import com.reloop.repository.*;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class TestSupport {
    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper json;
    @Autowired protected UserRepository users;
    @Autowired protected CategoryRepository categories;
    @Autowired protected ProductRepository products;
    @Autowired protected InventoryRepository inventory;
    @Autowired protected PasswordEncoder encoder;

    protected static int seq = 0;
    protected synchronized String uniqueEmail(String p) { return p + (++seq) + "-" + System.nanoTime() + "@test.local"; }

    protected User createUser(String email, Role role) {
        User u = new User();
        u.setEmail(email); u.setFullName("Test User"); u.setPasswordHash(encoder.encode("Passw0rd!")); u.setRole(role);
        return users.save(u);
    }

    protected String login(String email) throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"Passw0rd!\"}")).andReturn();
        JsonNode n = json.readTree(r.getResponse().getContentAsString());
        return "Bearer " + n.get("accessToken").asText();
    }

    protected Product createProduct(String name, int stock, String price) {
        Category c = categories.findBySlug("test-cat").orElseGet(() -> { Category x = new Category(); x.setName("Test"); x.setSlug("test-cat"); return categories.save(x); });
        Product p = new Product();
        p.setCategory(c); p.setName(name); p.setItemCondition(ItemCondition.NEW); p.setPrice(new BigDecimal(price));
        p = products.save(p);
        Inventory i = new Inventory(); i.setProduct(p); i.setQuantity(stock); inventory.save(i);
        return p;
    }

    protected long idOf(MvcResult r) throws Exception { return json.readTree(r.getResponse().getContentAsString()).get("id").asLong(); }
}
