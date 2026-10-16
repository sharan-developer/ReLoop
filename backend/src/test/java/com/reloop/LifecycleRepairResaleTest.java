package com.reloop;

import com.reloop.entity.*;
import com.reloop.entity.Enums.*;
import com.reloop.repository.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LifecycleRepairResaleTest extends TestSupport {
    @Autowired PartnerProfileRepository partners;
    @Autowired AddressRepository addresses;
    @Autowired OwnedProductRepository owned;

    private long registerProduct(String auth) throws Exception {
        var r = mvc.perform(post("/api/my-products").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Test Phone\",\"category\":\"Smartphones\",\"purchaseDate\":\"2024-01-10\",\"warrantyStart\":\"2024-01-10\",\"warrantyEnd\":\"2030-01-10\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.warrantyStatus").value("ACTIVE")).andReturn();
        return idOf(r);
    }

    private long addr(User u) {
        Address a = new Address(); a.setUser(u); a.setLabel("H"); a.setLine1("l1"); a.setCity("c"); a.setState("s"); a.setPostalCode("500001"); a.setPhone("9999999999");
        return addresses.save(a).getId();
    }

    @Test
    void passportIsOwnerOnlyAndPublicViewIsSanitised() throws Exception {
        User owner = createUser(uniqueEmail("own"), Role.CUSTOMER);
        String ownerAuth = login(owner.getEmail());
        String other = login(createUser(uniqueEmail("oth"), Role.CUSTOMER).getEmail());
        long id = registerProduct(ownerAuth);
        mvc.perform(get("/api/my-products/" + id + "/passport").header("Authorization", ownerAuth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.events[0].type").value("PRODUCT_REGISTERED"))
                .andExpect(jsonPath("$.events[0].verification").value("SELF_REPORTED"));
        mvc.perform(get("/api/my-products/" + id + "/passport").header("Authorization", other)).andExpect(status().isNotFound());
        String token = owned.findById(id).orElseThrow().getPublicToken();
        String body = mvc.perform(get("/api/passports/public/" + token)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertFalse(body.contains(owner.getEmail()));
        assertFalse(body.contains("serial"));
        mvc.perform(get("/api/passports/public/" + token + "/qr")).andExpect(status().isOk()).andExpect(content().contentType("image/png"));
    }

    @Test
    void repairWorkflowEnforcesTransitionsAndPartnerAssignment() throws Exception {
        User customer = createUser(uniqueEmail("rc"), Role.CUSTOMER);
        String cAuth = login(customer.getEmail());
        User pu1 = createUser(uniqueEmail("p1"), Role.REPAIR_PARTNER);
        User pu2 = createUser(uniqueEmail("p2"), Role.REPAIR_PARTNER);
        PartnerProfile p1 = approved(pu1), p2 = approved(pu2);
        String p1Auth = login(pu1.getEmail()), p2Auth = login(pu2.getEmail());
        long product = registerProduct(cAuth);
        long rid = idOf(mvc.perform(post("/api/repairs").header("Authorization", cAuth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"ownedProductId\":" + product + ",\"partnerId\":" + p1.getId() + ",\"problem\":\"Screen cracked\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("REQUESTED")).andReturn());
        // wrong partner cannot see or act on it
        mvc.perform(get("/api/partner/repairs/" + rid).header("Authorization", p2Auth)).andExpect(status().isNotFound());
        mvc.perform(post("/api/partner/repairs/" + rid + "/quote").header("Authorization", p2Auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":1000,\"estimatedDays\":3}")).andExpect(status().isNotFound());
        // invalid jumps rejected
        mvc.perform(post("/api/partner/repairs/" + rid + "/start").header("Authorization", p1Auth)).andExpect(status().isConflict());
        mvc.perform(post("/api/partner/repairs/" + rid + "/complete").header("Authorization", p1Auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"workNotes\":\"x\",\"finalCost\":1}")).andExpect(status().isConflict());
        // happy path
        mvc.perform(post("/api/partner/repairs/" + rid + "/quote").header("Authorization", p1Auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":1500,\"estimatedDays\":3}")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("QUOTED"));
        long quoteId = json.readTree(mvc.perform(get("/api/repairs/" + rid).header("Authorization", cAuth)).andReturn().getResponse().getContentAsString()).get("quotes").get(0).get("id").asLong();
        // partner cannot approve on behalf of the customer
        mvc.perform(post("/api/repairs/" + rid + "/quotes/" + quoteId + "/accept").header("Authorization", p1Auth)).andExpect(status().isNotFound());
        mvc.perform(post("/api/repairs/" + rid + "/quotes/" + quoteId + "/accept").header("Authorization", cAuth)).andExpect(jsonPath("$.status").value("APPROVED"));
        mvc.perform(post("/api/partner/repairs/" + rid + "/start").header("Authorization", p1Auth)).andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        mvc.perform(post("/api/partner/repairs/" + rid + "/complete").header("Authorization", p1Auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"workNotes\":\"Replaced screen\",\"finalCost\":1500}")).andExpect(jsonPath("$.status").value("COMPLETED"));
        mvc.perform(get("/api/my-products/" + product + "/passport").header("Authorization", cAuth))
                .andExpect(jsonPath("$.events[?(@.type=='REPAIR_COMPLETED')].verification").value("PARTNER_CONFIRMED"));
        mvc.perform(post("/api/repairs/" + rid + "/cancel").header("Authorization", cAuth)).andExpect(status().isConflict());
    }

    private PartnerProfile approved(User u) {
        PartnerProfile p = new PartnerProfile(); p.setUser(u); p.setBusinessName("Shop " + u.getId()); p.setCity("Hyd"); p.setApproved(true);
        return partners.save(p);
    }

    @Test
    void resaleListingPurchaseTransfersOwnershipOnceAfterSellerConfirms() throws Exception {
        User seller = createUser(uniqueEmail("sl"), Role.CUSTOMER);
        User buyer = createUser(uniqueEmail("by"), Role.CUSTOMER);
        String sAuth = login(seller.getEmail()), bAuth = login(buyer.getEmail());
        long product = registerProduct(sAuth);
        long lid = idOf(mvc.perform(post("/api/my-listings").header("Authorization", sAuth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"ownedProductId\":" + product + ",\"title\":\"Phone\",\"condition\":\"USED\",\"price\":5000}")).andExpect(status().isCreated()).andReturn());
        mvc.perform(get("/api/marketplace/listings/" + lid)).andExpect(status().isNotFound()); // draft is private
        mvc.perform(post("/api/my-listings/" + lid + "/publish").header("Authorization", sAuth)).andExpect(jsonPath("$.status").value("PUBLISHED"));
        long sellerAddr = addr(seller), buyerAddr = addr(buyer);
        String buyBody = "{\"addressId\":%d,\"paymentMethod\":\"COD\"}";
        mvc.perform(post("/api/marketplace/listings/" + lid + "/buy").header("Authorization", sAuth).contentType(MediaType.APPLICATION_JSON)
                .content(String.format(buyBody, sellerAddr))).andExpect(status().isBadRequest()); // own listing
        long orderId = idOf(mvc.perform(post("/api/marketplace/listings/" + lid + "/buy").header("Authorization", bAuth).contentType(MediaType.APPLICATION_JSON)
                .content(String.format(buyBody, buyerAddr))).andExpect(status().isCreated()).andReturn());
        // ownership has NOT moved yet
        assertEquals(seller.getId(), owned.findById(product).orElseThrow().getOwner().getId());
        // buyer cannot self-confirm
        mvc.perform(post("/api/marketplace/orders/" + orderId + "/confirm-handover").header("Authorization", bAuth)).andExpect(status().isNotFound());
        mvc.perform(post("/api/marketplace/orders/" + orderId + "/confirm-handover").header("Authorization", sAuth)).andExpect(status().isOk());
        assertEquals(buyer.getId(), owned.findById(product).orElseThrow().getOwner().getId());
        mvc.perform(get("/api/my-products/" + product + "/passport").header("Authorization", bAuth))
                .andExpect(jsonPath("$.events[?(@.type=='OWNERSHIP_TRANSFERRED')]").isNotEmpty());
        mvc.perform(get("/api/my-products/" + product).header("Authorization", sAuth)).andExpect(status().isNotFound());
        mvc.perform(post("/api/marketplace/orders/" + orderId + "/confirm-handover").header("Authorization", sAuth)).andExpect(status().isConflict());
    }

    @Test
    void onlyOneOfManyConcurrentBuyersCanReserveAUniqueItem() throws Exception {
        User seller = createUser(uniqueEmail("csl"), Role.CUSTOMER);
        String sAuth = login(seller.getEmail());
        long product = registerProduct(sAuth);
        long lid = idOf(mvc.perform(post("/api/my-listings").header("Authorization", sAuth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"ownedProductId\":" + product + ",\"title\":\"Race phone\",\"condition\":\"USED\",\"price\":4000}")).andReturn());
        mvc.perform(post("/api/my-listings/" + lid + "/publish").header("Authorization", sAuth)).andExpect(status().isOk());
        int n = 6;
        String[] auths = new String[n];
        long[] addrs = new long[n];
        for (int i = 0; i < n; i++) {
            User b = createUser(uniqueEmail("cb"), Role.CUSTOMER);
            auths[i] = login(b.getEmail());
            addrs[i] = addr(b);
        }
        ExecutorService ex = Executors.newFixedThreadPool(n);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger created = new AtomicInteger();
        List<Future<?>> fs = new java.util.ArrayList<>();
        for (int i = 0; i < n; i++) {
            final int k = i;
            fs.add(ex.submit(() -> {
                go.await();
                int st = mvc.perform(post("/api/marketplace/listings/" + lid + "/buy").header("Authorization", auths[k]).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addressId\":" + addrs[k] + ",\"paymentMethod\":\"COD\"}")).andReturn().getResponse().getStatus();
                if (st == 201) created.incrementAndGet();
                return null;
            }));
        }
        go.countDown();
        for (Future<?> f : fs) f.get(30, TimeUnit.SECONDS);
        ex.shutdown();
        assertEquals(1, created.get(), "exactly one buyer may reserve a unique used item");
    }
}
