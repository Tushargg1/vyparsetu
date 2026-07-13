package com.vyaparsetu.notification.controller;

import com.vyaparsetu.common.response.ApiResponse;
import com.vyaparsetu.common.response.PageResponse;
import com.vyaparsetu.common.security.CurrentUser;
import com.vyaparsetu.notification.entity.Notification;
import com.vyaparsetu.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "In-app notifications")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List my notifications")
    public ApiResponse<PageResponse<Notification>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(PageResponse.from(
                service.list(CurrentUser.id(), PageRequest.of(page, size))));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Long>> unreadCount() {
        return ApiResponse.ok(Map.of("count", service.unreadCount(CurrentUser.id())));
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<Void> markRead(@PathVariable Long id) {
        service.markRead(id, CurrentUser.id());
        return ApiResponse.ok(null);
    }
}
