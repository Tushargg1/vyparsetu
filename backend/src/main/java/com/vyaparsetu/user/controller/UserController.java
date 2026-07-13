package com.vyaparsetu.user.controller;

import com.vyaparsetu.common.response.ApiResponse;
import com.vyaparsetu.user.dto.UserResponse;
import com.vyaparsetu.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "Current user and profile")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get the currently authenticated user")
    public ApiResponse<UserResponse> me() {
        return ApiResponse.ok(userService.currentUser());
    }
}
