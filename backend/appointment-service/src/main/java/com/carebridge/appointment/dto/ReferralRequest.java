package com.carebridge.appointment.dto;

public class ReferralRequest {
    private String patientId;
    private String fromDoctorId;
    private String toSpecialty;
    private String reason;
    private String urgency = "ROUTINE";
    private String generatedBy = "AI_AGENT";

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
}
