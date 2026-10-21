package com.carebridge.provider.controller;

import com.carebridge.common.dto.ApiResponse;
import com.carebridge.provider.dto.DoctorRequest;
import com.carebridge.provider.dto.SlotRequest;
import com.carebridge.provider.model.AvailabilitySlot;
import com.carebridge.provider.model.Doctor;
import com.carebridge.provider.service.ProviderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/doctors")
public class DoctorController {

    private final ProviderService providerService;

    public DoctorController(ProviderService providerService) {
        this.providerService = providerService;
    }

    @PostMapping
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ApiResponse<Doctor>> createProfile(@RequestAttribute("userId") String userId,
                                                               @Valid @RequestBody DoctorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Doctor profile created", providerService.createProfile(userId, request)));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ApiResponse<Doctor>> getMyProfile(@RequestAttribute("userId") String userId) {
        return ResponseEntity.ok(ApiResponse.success("Profile fetched", providerService.getByUserId(userId)));
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ApiResponse<Doctor>> updateMyProfile(@RequestAttribute("userId") String userId,
                                                                 @Valid @RequestBody DoctorRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Profile updated", providerService.updateProfile(userId, request)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Doctor>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Doctor fetched", providerService.getById(id)));
    }

    /**
     * Open search endpoint - consumed by the patient dashboard AND (most importantly)
     * by the AI Agent Orchestrator's Doctor Search Tool Node during an agentic workflow.
     * Available to any authenticated role.
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<Doctor>>> search(@RequestParam(required = false) String specialty,
                                                              @RequestParam(required = false) String city) {
        return ResponseEntity.ok(ApiResponse.success("Search results", providerService.search(specialty, city)));
    }

    @PostMapping("/me/slots")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ApiResponse<AvailabilitySlot>> addSlot(@RequestAttribute("userId") String userId,
                                                                   @Valid @RequestBody SlotRequest request) {
        Doctor doctor = providerService.getByUserId(userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Slot added", providerService.addSlot(doctor.getId(), request)));
    }

    @GetMapping("/{id}/slots")
    public ResponseEntity<ApiResponse<List<AvailabilitySlot>>> availableSlots(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Available slots", providerService.getAvailableSlots(id)));
    }

    /** Called internally by appointment-service once a booking is confirmed. */
    @PatchMapping("/slots/{slotId}/book")
    public ResponseEntity<ApiResponse<AvailabilitySlot>> bookSlot(@PathVariable String slotId) {
        return ResponseEntity.ok(ApiResponse.success("Slot marked booked", providerService.markSlotBooked(slotId)));
    }
}
