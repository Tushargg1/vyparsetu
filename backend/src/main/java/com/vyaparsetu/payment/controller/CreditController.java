package com.vyaparsetu.payment.controller;

import com.vyaparsetu.common.response.ApiResponse;
import com.vyaparsetu.common.security.CurrentUser;
import com.vyaparsetu.payment.entity.CreditAccount;
import com.vyaparsetu.payment.service.CreditService;
import com.vyaparsetu.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/credit")
@Tag(name = "Credit", description = "Credit limit and approval management")
public class CreditController {

    private final CreditService creditService;
    private final UserService userService;

    public CreditController(CreditService creditService, UserService userService) {
        this.creditService = creditService;
        this.userService = userService;
    }

    public record SetLimitRequest(@NotNull Long retailerId, Long supplierId,
                                  @NotNull @PositiveOrZero BigDecimal creditLimit) {
    }

    @PostMapping("/limit")
    @PreAuthorize("hasAnyRole('SUPPLIER','ADMIN')")
    @Operation(summary = "Set or update a retailer's credit limit with a supplier")
    public ApiResponse<CreditAccount> setLimit(@RequestBody SetLimitRequest req) {
        Long supplierId = effectiveSupplierId(req.supplierId());
        return ApiResponse.ok(creditService.setCreditLimit(
                req.retailerId(), supplierId, req.creditLimit(), CurrentUser.id()));
    }

    @PostMapping("/approve/{retailerId}")
    @PreAuthorize("hasAnyRole('SUPPLIER','ADMIN')")
    @Operation(summary = "Approve (or revoke) a retailer for credit")
    public ApiResponse<Void> approve(@PathVariable Long retailerId,
                                    @RequestParam(defaultValue = "true") boolean approved) {
        creditService.approveRetailer(retailerId, approved);
        return ApiResponse.ok(null);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPPLIER','ADMIN')")
    @Operation(summary = "Get a credit account")
    public ApiResponse<CreditAccount> get(@RequestParam Long retailerId,
                                         @RequestParam(required = false) Long supplierId) {
        return ApiResponse.ok(creditService.get(retailerId, effectiveSupplierId(supplierId)));
    }

    /**
     * SECURITY: a SUPPLIER may only act on their own supplier id; only an ADMIN may
     * target an arbitrary supplier id.
     */
    private Long effectiveSupplierId(Long requested) {
        if (CurrentUser.get().roles().contains("ADMIN")) {
            if (requested == null) {
                throw new com.vyaparsetu.common.exception.BusinessException(
                        "SUPPLIER_ID_REQUIRED", org.springframework.http.HttpStatus.BAD_REQUEST,
                        "supplierId is required for admin");
            }
            return requested;
        }
        return userService.currentSupplierId();
    }
}
