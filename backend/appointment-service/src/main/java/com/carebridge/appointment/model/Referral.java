package com.carebridge.appointment.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "referrals")
public class Referral {

    @Id
    private String id;

    private String patientId;
    private String fromDoctorId;    // nullable - null when generated directly by the AI agent
    private String toSpecialty;
    private String reason;
    private String urgency = "ROUTINE"; // ROUTINE | URGENT | EMERGENCY
    private String generatedBy = "AI_AGENT"; // AI_AGENT | DOCTOR
    private Instant createdAt = Instant.now();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public String getFromDoctorId() { return fromDoctorId; }
    public void setFromDoctorId(String fromDoctorId) { this.fromDoctorId = fromDoctorId; }
    public String getToSpecialty() { return toSpecialty; }
    public void setToSpecialty(String toSpecialty) { this.toSpecialty = toSpecialty; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getUrgency() { return urgency; }
    public void setUrgency(String urgency) { this.urgency = urgency; }
    public String getGeneratedBy() { return generatedBy; }
    public void setGeneratedBy(String generatedBy) { this.generatedBy = generatedBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
