package com.carebridge.patient.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document(collection = "symptomLogs")
public class SymptomLog {

    @Id
    private String id;

    private String patientId;
    private List<String> symptoms;
    private String severity;       // LOW | MEDIUM | HIGH | EMERGENCY
    private String reportedVia;    // AI_AGENT | MANUAL
    private Instant createdAt = Instant.now();

    public SymptomLog() {}

    public SymptomLog(String patientId, List<String> symptoms, String severity, String reportedVia) {
        this.patientId = patientId;
        this.symptoms = symptoms;
        this.severity = severity;
        this.reportedVia = reportedVia;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public List<String> getSymptoms() { return symptoms; }
    public void setSymptoms(List<String> symptoms) { this.symptoms = symptoms; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getReportedVia() { return reportedVia; }
    public void setReportedVia(String reportedVia) { this.reportedVia = reportedVia; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
