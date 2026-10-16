package com.reloop.config;

import com.reloop.entity.Enums.*;
import com.reloop.entity.*;
import com.reloop.repository.*;
import com.reloop.service.LifecycleService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 1) First admin: created ONLY from RELOOP_ADMIN_EMAIL / RELOOP_ADMIN_PASSWORD, and only if no admin exists yet.
 *    There is no public admin registration endpoint.
 * 2) Demo data (local development only): enabled by reloop.seed-demo-data=true (SEED_DEMO_DATA, default true).
 */
@Component
public class BootstrapRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(BootstrapRunner.class);
    private final AppProperties props;
    private final UserRepository users;
    private final PartnerProfileRepository partners;
    private final AddressRepository addresses;
    private final OwnedProductRepository owned;
    private final WarrantyRepository warranties;
    private final ResaleListingRepository listings;
    private final LifecycleService lifecycle;
    private final PasswordEncoder encoder;

    public BootstrapRunner(AppProperties props, UserRepository users, PartnerProfileRepository partners, AddressRepository addresses,
                           OwnedProductRepository owned, WarrantyRepository warranties, ResaleListingRepository listings,
                           LifecycleService lifecycle, PasswordEncoder encoder) {
        this.props = props; this.users = users; this.partners = partners; this.addresses = addresses; this.owned = owned;
        this.warranties = warranties; this.listings = listings; this.lifecycle = lifecycle; this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String email = props.getAdmin().getEmail();
        String pw = props.getAdmin().getPassword();
        if (email != null && !email.isBlank() && pw != null && pw.length() >= 8 && !users.existsByRole(Role.ADMIN)) {
            newUser(email, "ReLoop Admin", pw, Role.ADMIN);
            log.info("Created initial administrator {}", email);
        }
        if (props.isSeedDemoData()) seedDemo();
        if (!users.existsByRole(Role.ADMIN)) {
            log.warn("No administrator exists. Set RELOOP_ADMIN_EMAIL and RELOOP_ADMIN_PASSWORD (min 8 chars) and restart.");
        }
    }

    private User newUser(String email, String name, String pw, Role role) {
        User u = new User();
        u.setEmail(email.toLowerCase());
        u.setFullName(name);
        u.setPasswordHash(encoder.encode(pw));
        u.setRole(role);
        return users.save(u);
    }

    private void seedDemo() {
        if (users.existsByEmailIgnoreCase("customer@reloop.local")) return;
        log.warn("Seeding DEMO accounts (customer/buyer/partner/admin @reloop.local). Disable with SEED_DEMO_DATA=false.");
        if (!users.existsByRole(Role.ADMIN)) newUser("admin@reloop.local", "Demo Admin", "Admin@12345", Role.ADMIN);
        User customer = newUser("customer@reloop.local", "Asha Customer", "Customer@123", Role.CUSTOMER);
        User buyer = newUser("buyer@reloop.local", "Ravi Buyer", "Buyer@1234", Role.CUSTOMER);
        User partnerUser = newUser("partner@reloop.local", "Fixit Partner", "Partner@123", Role.REPAIR_PARTNER);
        PartnerProfile pp = new PartnerProfile();
        pp.setUser(partnerUser); pp.setBusinessName("FixIt Electronics Care"); pp.setCity("Hyderabad"); pp.setApproved(true);
        partners.save(pp);
        for (User u : new User[]{customer, buyer}) {
            Address a = new Address();
            a.setUser(u); a.setLabel("Home"); a.setLine1("12-3-45, Road No. 7, Banjara Hills"); a.setCity("Hyderabad");
            a.setState("Telangana"); a.setPostalCode("500034"); a.setPhone("9876543210"); a.setDefaultAddress(true);
            addresses.save(a);
        }
        OwnedProduct phone = ownedProduct(customer, "Samsung Galaxy S21", "Smartphones", "Samsung", "SM-G991B", LocalDate.now().minusMonths(20));
        warranty(phone, LocalDate.now().minusMonths(20), LocalDate.now().minusMonths(8), "Samsung India");
        OwnedProduct laptop = ownedProduct(customer, "Dell Inspiron 15", "Laptops", "Dell", "3511", LocalDate.now().minusMonths(6));
        warranty(laptop, LocalDate.now().minusMonths(6), LocalDate.now().plusMonths(18), "Dell India");
        lifecycle.record(phone, EventType.REPAIR_REQUESTED, Verification.SELF_REPORTED, customer.getId(), "Owner", "Repair requested from FixIt Electronics Care: battery drains fast");
        lifecycle.record(phone, EventType.REPAIR_COMPLETED, Verification.PARTNER_CONFIRMED, partnerUser.getId(), "Repair partner: FixIt Electronics Care", "Repair completed. Work performed: battery replaced (final cost INR 2499)");
        OwnedProduct tablet = ownedProduct(customer, "iPad 8th Gen 32GB", "Tablets", "Apple", "A2270", LocalDate.now().minusMonths(30));
        ResaleListing l = new ResaleListing();
        l.setOwnedProduct(tablet); l.setSeller(customer); l.setTitle("iPad 8th Gen 32GB, good condition");
        l.setDescription("Light use, screen guard since day one. Charger included.");
        l.setItemCondition(ItemCondition.USED); l.setPrice(new BigDecimal("17500")); l.setImageUrl("/images/products/ipad-9.svg");
        l.setStatus(ListingStatus.PUBLISHED);
        listings.save(l);
        tablet.setStatus(OwnedStatus.LISTED);
        owned.save(tablet);
        lifecycle.record(tablet, EventType.RESALE_LISTED, Verification.SELF_REPORTED, customer.getId(), "Owner", "Listed for resale on the ReLoop marketplace");
    }

    private OwnedProduct ownedProduct(User owner, String name, String cat, String brand, String model, LocalDate bought) {
        OwnedProduct op = new OwnedProduct();
        op.setOwner(owner); op.setName(name); op.setCategory(cat); op.setBrand(brand); op.setModel(model);
        op.setPurchaseDate(bought); op.setPublicToken(java.util.UUID.randomUUID().toString().replace("-", ""));
        op = owned.save(op);
        lifecycle.record(op, EventType.PRODUCT_REGISTERED, Verification.SELF_REPORTED, owner.getId(), "Owner", "Product registered by its owner (self-reported)");
        lifecycle.record(op, EventType.PURCHASE_RECORDED, Verification.SELF_REPORTED, owner.getId(), "Owner", "Purchase date recorded: " + bought);
        return op;
    }

    private void warranty(OwnedProduct op, LocalDate s, LocalDate e, String provider) {
        Warranty w = new Warranty();
        w.setOwnedProduct(op); w.setStartDate(s); w.setEndDate(e); w.setProvider(provider);
        warranties.save(w);
        lifecycle.record(op, EventType.WARRANTY_UPDATED, Verification.SELF_REPORTED, op.getOwner().getId(), "Owner", "Warranty " + s + " to " + e + " (" + provider + ") (self-reported)");
    }
}
