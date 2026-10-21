package com.carebridge.patient.dto;

import java.util.List;

public class SymptomLogRequest {
    private List<String> symptoms;
    private String severity;
    private String reportedVia = "MANUAL";

    public List<String> getSymptoms() { return symptoms; }
    public void setSymptoms(List<String> symptoms) { this.symptoms = symptoms; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getReportedVia() { return reportedVia; }
    public void setReportedVia(String reportedVia) { this.reportedVia = reportedVia; }
}
