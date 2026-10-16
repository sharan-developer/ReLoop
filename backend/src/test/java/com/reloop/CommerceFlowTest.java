package com.reloop;

import com.reloop.entity.Product;
import com.reloop.entity.Enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CommerceFlowTest extends TestSupport {

    private long address(String auth) throws Exception {
        var r = mvc.perform(post("/api/addresses").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"label\":\"Home\",\"line1\":\"1 Main St\",\"city\":\"Hyderabad\",\"state\":\"TS\",\"postalCode\":\"500001\",\"phone\":\"9999999999\"}"))
                .andExpect(status().isCreated()).andReturn();
        return idOf(r);
    }

    @Test
    void productSearchFiltersOnBackendData() throws Exception {
        createProduct("Zeta Searchable Phone", 5, "999.00");
        mvc.perform(get("/api/products").param("q", "zeta search")).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/products").param("q", "Zeta Searchable").param("minPrice", "500").param("maxPrice", "1500"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].name").value("Zeta Searchable Phone"));
        mvc.perform(get("/api/products").param("q", "Zeta Searchable").param("minPrice", "5000")).andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void cartRecalculatesPricesAndEnforcesStock() throws Exception {
        Product p = createProduct("Cart Widget", 3, "100.50");
        String auth = login(createUser(uniqueEmail("cart"), Role.CUSTOMER).getEmail());
        mvc.perform(post("/api/cart/items").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\":" + p.getId() + ",\"quantity\":2,\"price\":1}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.subtotal").value(201.00));
        mvc.perform(post("/api/cart/items").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\":" + p.getId() + ",\"quantity\":2}")).andExpect(status().isConflict());
    }

    @Test
    void checkoutSnapshotsPriceDecrementsStockAndCancelRestores() throws Exception {
        Product p = createProduct("Order Widget", 5, "250.00");
        String auth = login(createUser(uniqueEmail("ord"), Role.CUSTOMER).getEmail());
        long addr = address(auth);
        mvc.perform(post("/api/cart/items").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\":" + p.getId() + ",\"quantity\":2}")).andExpect(status().isOk());
        var res = mvc.perform(post("/api/orders").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"addressId\":" + addr + ",\"paymentMethod\":\"COD\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.total").value(500.00))
                .andExpect(jsonPath("$.status").value("PLACED")).andReturn();
        long orderId = idOf(res);
        assertEquals(3, inventory.findByProductId(p.getId()).orElseThrow().getQuantity());
        mvc.perform(get("/api/cart").header("Authorization", auth)).andExpect(jsonPath("$.itemCount").value(0));
        mvc.perform(post("/api/orders/" + orderId + "/cancel").header("Authorization", auth)).andExpect(status().isOk());
        assertEquals(5, inventory.findByProductId(p.getId()).orElseThrow().getQuantity());
        mvc.perform(post("/api/orders/" + orderId + "/cancel").header("Authorization", auth)).andExpect(status().isConflict());
    }

    @Test
    void otherUsersCannotSeeAnOrderAndRazorpayRequiresConfiguration() throws Exception {
        Product p = createProduct("Private Widget", 5, "10.00");
        String a = login(createUser(uniqueEmail("oa"), Role.CUSTOMER).getEmail());
        String b = login(createUser(uniqueEmail("ob"), Role.CUSTOMER).getEmail());
        long addr = address(a);
        mvc.perform(post("/api/cart/items").header("Authorization", a).contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\":" + p.getId() + ",\"quantity\":1}")).andExpect(status().isOk());
        mvc.perform(post("/api/orders").header("Authorization", a).contentType(MediaType.APPLICATION_JSON)
                .content("{\"addressId\":" + addr + ",\"paymentMethod\":\"RAZORPAY\"}")).andExpect(status().isBadRequest());
        long id = idOf(mvc.perform(post("/api/orders").header("Authorization", a).contentType(MediaType.APPLICATION_JSON)
                .content("{\"addressId\":" + addr + ",\"paymentMethod\":\"COD\"}")).andReturn());
        mvc.perform(get("/api/orders/" + id).header("Authorization", b)).andExpect(status().isNotFound());
    }

    @Test
    void cannotOrderMoreThanStockWhenStockChangesAfterCarting() throws Exception {
        Product p = createProduct("Race Widget", 2, "10.00");
        String auth = login(createUser(uniqueEmail("race"), Role.CUSTOMER).getEmail());
        long addr = address(auth);
        mvc.perform(post("/api/cart/items").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\":" + p.getId() + ",\"quantity\":2}")).andExpect(status().isOk());
        inventory.decrement(p.getId(), 1);
        mvc.perform(post("/api/orders").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"addressId\":" + addr + ",\"paymentMethod\":\"COD\"}")).andExpect(status().isConflict());
        assertEquals(1, inventory.findByProductId(p.getId()).orElseThrow().getQuantity());
    }
}
