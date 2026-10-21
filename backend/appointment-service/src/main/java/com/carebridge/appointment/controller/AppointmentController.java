package com.carebridge.appointment.controller;

import com.carebridge.appointment.dto.AppointmentRequest;
import com.carebridge.appointment.model.Appointment;
import com.carebridge.appointment.service.AppointmentService;
import com.carebridge.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    /**
     * Callable by a patient booking directly through the UI, OR by the
     * ai-agent-service's "Action Execution Node" once the user has confirmed
     * a proposed slot in the agentic chat flow (bookedBy="AI_AGENT").
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('PATIENT','ADMIN')")
    public ResponseEntity<ApiResponse<Appointment>> book(@Valid @RequestBody AppointmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Appointment booked", appointmentService.bookAppointment(request)));
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR','ADMIN')")
    public ResponseEntity<ApiResponse<List<Appointment>>> byPatient(@PathVariable String patientId) {
        return ResponseEntity.ok(ApiResponse.success("Appointments", appointmentService.getByPatient(patientId)));
    }

    @GetMapping("/doctor/{doctorId}")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public ResponseEntity<ApiResponse<List<Appointment>>> byDoctor(@PathVariable String doctorId) {
        return ResponseEntity.ok(ApiResponse.success("Appointments", appointmentService.getByDoctor(doctorId)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN','PATIENT')")
    public ResponseEntity<ApiResponse<Appointment>> updateStatus(@PathVariable String id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.success("Status updated", appointmentService.updateStatus(id, body.get("status"))));
    }
}
