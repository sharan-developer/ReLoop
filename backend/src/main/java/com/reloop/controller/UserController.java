package com.reloop.controller;

import com.reloop.dto.Dtos.*;
import com.reloop.security.CurrentUser;
import com.reloop.service.AuthService;
import com.reloop.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService users;

    @GetMapping("/api/users/me")
    public UserDto me() { return AuthService.toDto(CurrentUser.get()); }

    @PutMapping("/api/users/me")
    public UserDto update(@Valid @RequestBody UpdateProfileRequest r) { return users.updateProfile(CurrentUser.get().getId(), r); }

    @PostMapping("/api/users/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void password(@Valid @RequestBody ChangePasswordRequest r) { users.changePassword(CurrentUser.get().getId(), r); }

    @GetMapping("/api/addresses")
    public List<AddressDto> addresses() { return users.listAddresses(CurrentUser.get().getId()); }

    @PostMapping("/api/addresses")
    @ResponseStatus(HttpStatus.CREATED)
    public AddressDto addAddress(@Valid @RequestBody AddressRequest r) { return users.addAddress(CurrentUser.get().getId(), r); }

    @PutMapping("/api/addresses/{id}")
    public AddressDto updateAddress(@PathVariable Long id, @Valid @RequestBody AddressRequest r) { return users.updateAddress(CurrentUser.get().getId(), id, r); }

    @DeleteMapping("/api/addresses/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAddress(@PathVariable Long id) { users.deleteAddress(CurrentUser.get().getId(), id); }

    @GetMapping("/api/partners")
    public List<PartnerDto> partners() { return users.approvedPartners(); }

    @PostMapping("/api/partners/apply")
    @ResponseStatus(HttpStatus.CREATED)
    public PartnerDto apply(@Valid @RequestBody PartnerApplyRequest r) { return users.applyAsPartner(CurrentUser.get().getId(), r); }
}
