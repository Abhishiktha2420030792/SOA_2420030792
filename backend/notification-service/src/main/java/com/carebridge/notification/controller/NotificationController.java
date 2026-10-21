package com.carebridge.notification.controller;

import com.carebridge.common.dto.ApiResponse;
import com.carebridge.notification.dto.NotificationRequest;
import com.carebridge.notification.model.Notification;
import com.carebridge.notification.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /** Called by appointment-service and ai-agent-service (service-to-service). */
    @PostMapping
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR','ADMIN')")
    public ResponseEntity<ApiResponse<Notification>> send(@RequestBody NotificationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Notification dispatched", notificationService.send(request)));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR','ADMIN')")
    public ResponseEntity<ApiResponse<List<Notification>>> myNotifications(@RequestAttribute("userId") String userId) {
        return ResponseEntity.ok(ApiResponse.success("Notifications", notificationService.getByRecipient(userId)));
    }
}
