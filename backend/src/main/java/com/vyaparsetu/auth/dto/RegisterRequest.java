package com.vyaparsetu.auth.dto;

import com.vyaparsetu.common.enums.Enums;
import com.vyaparsetu.common.enums.RoleName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record RegisterRequest(
        @NotBlank String name,
        @NotBlank @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid Indian mobile number") String phone,
        @Email String email,
        @NotNull RoleName role,
        Enums.Language preferredLanguage,
        // role profile fields (used based on role)
        String shopName,
        String businessName,
        Enums.SupplierType supplierType,
        String gstNumber,
        String address,
        String city,
        String state,
        String pincode,
        String altPhones,
        String locationUrl,
        // retailers may supply a distributor invite code to link at signup
        String inviteCode
) {
}
