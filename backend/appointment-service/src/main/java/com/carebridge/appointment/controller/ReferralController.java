package com.carebridge.appointment.controller;

import com.carebridge.appointment.dto.ReferralRequest;
import com.carebridge.appointment.model.Referral;
import com.carebridge.appointment.service.AppointmentService;
import com.carebridge.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/referrals")
public class ReferralController {

    private final AppointmentService appointmentService;

    public ReferralController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    /** Created either by a doctor after a consultation, or by ai-agent-service autonomously. */
    @PostMapping
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN','PATIENT')")
    public ResponseEntity<ApiResponse<Referral>> create(@Valid @RequestBody ReferralRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Referral created", appointmentService.createReferral(request)));
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR','ADMIN')")
    public ResponseEntity<ApiResponse<List<Referral>>> byPatient(@PathVariable String patientId) {
        return ResponseEntity.ok(ApiResponse.success("Referrals", appointmentService.getReferralsByPatient(patientId)));
    }
}
