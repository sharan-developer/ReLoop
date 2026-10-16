package com.reloop.service;

import com.reloop.dto.Dtos.*;
import com.reloop.entity.Address;
import com.reloop.entity.PartnerProfile;
import com.reloop.entity.User;
import com.reloop.exception.ApiException;
import com.reloop.repository.*;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository users;
    private final AddressRepository addresses;
    private final PartnerProfileRepository partners;
    private final PasswordEncoder encoder;

    @Transactional
    public UserDto updateProfile(Long userId, UpdateProfileRequest r) {
        User u = users.findById(userId).orElseThrow(() -> ApiException.notFound("User"));
        u.setFullName(r.fullName().trim());
        u.setPhone(r.phone());
        return AuthService.toDto(users.save(u));
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest r) {
        User u = users.findById(userId).orElseThrow(() -> ApiException.notFound("User"));
        if (!encoder.matches(r.currentPassword(), u.getPasswordHash())) throw ApiException.badRequest("Current password is incorrect");
        u.setPasswordHash(encoder.encode(r.newPassword()));
        users.save(u);
    }

    // ---- addresses (always scoped to the caller) ----
    public List<AddressDto> listAddresses(Long userId) {
        return addresses.findByUserIdAndDeletedFalseOrderByIdAsc(userId).stream().map(UserService::toDto).toList();
    }

    @Transactional
    public AddressDto addAddress(Long userId, AddressRequest r) {
        Address a = new Address();
        a.setUser(users.getReferenceById(userId));
        apply(a, r);
        boolean first = addresses.findByUserIdAndDeletedFalseOrderByIdAsc(userId).isEmpty();
        if (first) a.setDefaultAddress(true);
        a = addresses.save(a);
        if (a.isDefaultAddress()) clearOtherDefaults(userId, a.getId());
        return toDto(a);
    }

    @Transactional
    public AddressDto updateAddress(Long userId, Long id, AddressRequest r) {
        Address a = addresses.findByIdAndUserIdAndDeletedFalse(id, userId).orElseThrow(() -> ApiException.notFound("Address"));
        apply(a, r);
        a = addresses.save(a);
        if (a.isDefaultAddress()) clearOtherDefaults(userId, a.getId());
        return toDto(a);
    }

    /** Soft delete: past orders keep their own copy of the shipping address. */
    @Transactional
    public void deleteAddress(Long userId, Long id) {
        Address a = addresses.findByIdAndUserIdAndDeletedFalse(id, userId).orElseThrow(() -> ApiException.notFound("Address"));
        a.setDeleted(true);
        a.setDefaultAddress(false);
        addresses.save(a);
    }

    private void clearOtherDefaults(Long userId, Long keepId) {
        for (Address o : addresses.findByUserIdAndDeletedFalseOrderByIdAsc(userId)) {
            if (!o.getId().equals(keepId) && o.isDefaultAddress()) { o.setDefaultAddress(false); addresses.save(o); }
        }
    }

    private void apply(Address a, AddressRequest r) {
        a.setLabel(r.label().trim());
        a.setLine1(r.line1().trim());
        a.setLine2(r.line2());
        a.setCity(r.city().trim());
        a.setState(r.state().trim());
        a.setPostalCode(r.postalCode());
        a.setPhone(r.phone().trim());
        a.setDefaultAddress(r.defaultAddress());
    }

    public static AddressDto toDto(Address a) {
        return new AddressDto(a.getId(), a.getLabel(), a.getLine1(), a.getLine2(), a.getCity(), a.getState(),
                a.getPostalCode(), a.getPhone(), a.isDefaultAddress());
    }

    // ---- partners ----
    @Transactional
    public PartnerDto applyAsPartner(Long userId, PartnerApplyRequest r) {
        if (partners.findByUserId(userId).isPresent()) throw ApiException.conflict("You have already applied");
        PartnerProfile p = new PartnerProfile();
        p.setUser(users.getReferenceById(userId));
        p.setBusinessName(r.businessName().trim());
        p.setCity(r.city().trim());
        p.setApproved(false);
        p = partners.save(p);
        return new PartnerDto(p.getId(), p.getBusinessName(), p.getCity());
    }

    public List<PartnerDto> approvedPartners() {
        return partners.findByApprovedTrueOrderByBusinessNameAsc().stream()
                .map(p -> new PartnerDto(p.getId(), p.getBusinessName(), p.getCity())).toList();
    }
}
