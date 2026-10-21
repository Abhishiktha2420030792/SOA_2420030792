package com.carebridge.appointment.service;

import com.carebridge.appointment.client.NotificationClient;
import com.carebridge.appointment.client.ProviderClient;
import com.carebridge.appointment.dto.AppointmentRequest;
import com.carebridge.appointment.dto.ReferralRequest;
import com.carebridge.appointment.model.Appointment;
import com.carebridge.appointment.model.Referral;
import com.carebridge.appointment.repository.AppointmentRepository;
import com.carebridge.appointment.repository.ReferralRepository;
import com.carebridge.common.exception.BadRequestException;
import com.carebridge.common.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class AppointmentService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentService.class);

    private final AppointmentRepository appointmentRepository;
    private final ReferralRepository referralRepository;
    private final ProviderClient providerClient;
    private final NotificationClient notificationClient;

    public AppointmentService(AppointmentRepository appointmentRepository,
                               ReferralRepository referralRepository,
                               ProviderClient providerClient,
                               NotificationClient notificationClient) {
        this.appointmentRepository = appointmentRepository;
        this.referralRepository = referralRepository;
        this.providerClient = providerClient;
        this.notificationClient = notificationClient;
    }

    /**
     * Books an appointment AND demonstrates the SOA request chain:
     * appointment-service -> provider-service (mark slot booked)
     *                     -> notification-service (send confirmation)
     * Both downstream calls are resilient: if notification fails, the booking
     * itself still succeeds (notification is best-effort, not transactional).
     */
    public Appointment bookAppointment(AppointmentRequest request) {
        if (request.getPatientId() == null || request.getDoctorId() == null || request.getSlotId() == null) {
            throw new BadRequestException("patientId, doctorId and slotId are all required");
        }

        Appointment appointment = new Appointment();
        appointment.setPatientId(request.getPatientId());
        appointment.setDoctorId(request.getDoctorId());
        appointment.setSlotId(request.getSlotId());
        appointment.setReasonForVisit(request.getReasonForVisit());
        appointment.setBookedBy(request.getBookedBy() == null ? "PATIENT" : request.getBookedBy());

        try {
            providerClient.markSlotBooked(request.getSlotId());
        } catch (Exception e) {
            log.error("Failed to mark slot {} as booked on provider-service", request.getSlotId(), e);
            throw new BadRequestException("Selected slot could not be booked (it may already be taken): " + e.getMessage());
        }

        appointment.setStatus("CONFIRMED");
        Appointment saved = appointmentRepository.save(appointment);

        try {
            notificationClient.send(Map.of(
                    "recipientId", request.getPatientId(),
                    "type", "APPOINTMENT_CONFIRMATION",
                    "channel", "EMAIL",
                    "message", "Your appointment has been confirmed for slot " + request.getSlotId()
            ));
        } catch (Exception e) {
            log.warn("Notification dispatch failed for appointment {} (non-fatal): {}", saved.getId(), e.getMessage());
        }

        return saved;
    }

    public List<Appointment> getByPatient(String patientId) {
        return appointmentRepository.findByPatientId(patientId);
    }

    public List<Appointment> getByDoctor(String doctorId) {
        return appointmentRepository.findByDoctorId(doctorId);
    }

    public Appointment updateStatus(String appointmentId, String status) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found: " + appointmentId));
        appointment.setStatus(status);
        appointment.setUpdatedAt(Instant.now());
        return appointmentRepository.save(appointment);
    }

    public Referral createReferral(ReferralRequest request) {
        Referral referral = new Referral();
        referral.setPatientId(request.getPatientId());
        referral.setFromDoctorId(request.getFromDoctorId());
        referral.setToSpecialty(request.getToSpecialty());
        referral.setReason(request.getReason());
        referral.setUrgency(request.getUrgency() == null ? "ROUTINE" : request.getUrgency());
        referral.setGeneratedBy(request.getGeneratedBy() == null ? "AI_AGENT" : request.getGeneratedBy());
        Referral saved = referralRepository.save(referral);

        try {
            notificationClient.send(Map.of(
                    "recipientId", request.getPatientId(),
                    "type", "REFERRAL_ALERT",
                    "channel", "EMAIL",
                    "message", "You have been referred to a " + request.getToSpecialty() + " specialist. Reason: " + request.getReason()
            ));
        } catch (Exception e) {
            log.warn("Notification dispatch failed for referral {} (non-fatal): {}", saved.getId(), e.getMessage());
        }

        return saved;
    }

    public List<Referral> getReferralsByPatient(String patientId) {
        return referralRepository.findByPatientId(patientId);
    }
}
