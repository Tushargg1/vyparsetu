package com.vyaparsetu.auth.dto;

import com.vyaparsetu.common.enums.Enums;
import com.vyaparsetu.common.enums.RoleName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank String name,
        @NotBlank @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid Indian mobile number") String phone,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotNull RoleName role,
        Enums.Language preferredLanguage,
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
        String inviteCode
) {
}