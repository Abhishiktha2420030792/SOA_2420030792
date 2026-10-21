package com.carebridge.appointment.client;

import com.carebridge.appointment.config.FeignConfig;
import com.carebridge.common.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * Declarative REST client to provider-service, resolved dynamically through
 * Eureka ("provider-service" is the registered application name, not a hardcoded
 * host:port). This is the concrete "service-to-service communication" example:
 * when an appointment is confirmed, appointment-service calls provider-service
 * to flip the slot's `booked` flag.
 */
@FeignClient(name = "provider-service", configuration = FeignConfig.class)
public interface ProviderClient {

    @PatchMapping("/api/doctors/slots/{slotId}/book")
    ApiResponse<Map<String, Object>> markSlotBooked(@PathVariable("slotId") String slotId);

    @GetMapping("/api/doctors/{id}")
    ApiResponse<Map<String, Object>> getDoctor(@PathVariable("id") String id);
}
