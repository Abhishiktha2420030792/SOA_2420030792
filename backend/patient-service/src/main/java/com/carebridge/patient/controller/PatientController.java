package com.carebridge.patient.controller;

import com.carebridge.common.dto.ApiResponse;
import com.carebridge.patient.dto.PatientRequest;
import com.carebridge.patient.dto.SymptomLogRequest;
import com.carebridge.patient.model.Patient;
import com.carebridge.patient.model.SymptomLog;
import com.carebridge.patient.service.PatientService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @PostMapping
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<Patient>> createProfile(@RequestAttribute("userId") String userId,
                                                                @Valid @RequestBody PatientRequest request) {
        Patient patient = patientService.createProfile(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Profile created", patient));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<Patient>> getMyProfile(@RequestAttribute("userId") String userId) {
        return ResponseEntity.ok(ApiResponse.success("Profile fetched", patientService.getByUserId(userId)));
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<Patient>> updateMyProfile(@RequestAttribute("userId") String userId,
                                                                  @Valid @RequestBody PatientRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Profile updated", patientService.updateProfile(userId, request)));
    }

    // DOCTOR/ADMIN viewing a specific patient record (e.g. during a referral or consultation)
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Patient>> getPatientById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Patient fetched", patientService.getById(id)));
    }

    @PostMapping("/me/symptoms")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<SymptomLog>> logSymptoms(@RequestAttribute("userId") String userId,
                                                                 @Valid @RequestBody SymptomLogRequest request) {
        Patient patient = patientService.getByUserId(userId);
        SymptomLog log = patientService.addSymptomLog(patient.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Symptom log recorded", log));
    }

    @GetMapping("/me/symptoms")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<List<SymptomLog>>> mySymptomHistory(@RequestAttribute("userId") String userId) {
        Patient patient = patientService.getByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success("Symptom history", patientService.getSymptomHistory(patient.getId())));
    }

    /**
     * Called by ai-agent-service and appointment-service (service-to-service, forwarding
     * the originating patient's JWT) to pull clinical context needed for decisioning.
     */
    @GetMapping("/{id}/symptoms")
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR','ADMIN')")
    public ResponseEntity<ApiResponse<List<SymptomLog>>> symptomHistory(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Symptom history", patientService.getSymptomHistory(id)));
    }
}
