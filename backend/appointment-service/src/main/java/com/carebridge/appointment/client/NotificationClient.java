package com.carebridge.appointment.client;

import com.carebridge.appointment.config.FeignConfig;
import com.carebridge.common.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "notification-service", configuration = FeignConfig.class)
public interface NotificationClient {

    @PostMapping("/api/notifications")
    ApiResponse<Map<String, Object>> send(@RequestBody Map<String, Object> notificationRequest);
}
