package com.vyaparsetu.admin.controller;

import com.vyaparsetu.admin.service.AdminService;
import com.vyaparsetu.common.enums.Enums;
import com.vyaparsetu.common.response.ApiResponse;
import com.vyaparsetu.common.response.PageResponse;
import com.vyaparsetu.user.dto.UserResponse;
import com.vyaparsetu.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin", description = "Admin dashboard and user management")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Platform-wide counts")
    public ApiResponse<Map<String, Long>> dashboard() {
        return ApiResponse.ok(adminService.dashboard());
    }

    @GetMapping("/users")
    public ApiResponse<PageResponse<UserResponse>> users(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(PageResponse.from(
                adminService.users(PageRequest.of(page, size)).map(UserResponse::from)));
    }

    @PatchMapping("/users/{id}/status")
    @Operation(summary = "Activate or suspend a user")
    public ApiResponse<Void> setStatus(@PathVariable Long id, @RequestParam Enums.UserStatus status) {
        adminService.setUserStatus(id, status);
        return ApiResponse.ok(null);
    }
}
