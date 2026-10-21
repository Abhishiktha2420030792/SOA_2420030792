package com.carebridge.patient.service;

import com.carebridge.common.exception.BadRequestException;
import com.carebridge.common.exception.ResourceNotFoundException;
import com.carebridge.patient.dto.PatientRequest;
import com.carebridge.patient.dto.SymptomLogRequest;
import com.carebridge.patient.model.Patient;
import com.carebridge.patient.model.SymptomLog;
import com.carebridge.patient.repository.PatientRepository;
import com.carebridge.patient.repository.SymptomLogRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class PatientService {

    private final PatientRepository patientRepository;
    private final SymptomLogRepository symptomLogRepository;

    public PatientService(PatientRepository patientRepository, SymptomLogRepository symptomLogRepository) {
        this.patientRepository = patientRepository;
        this.symptomLogRepository = symptomLogRepository;
    }

    public Patient createProfile(String userId, PatientRequest request) {
        if (patientRepository.existsByUserId(userId)) {
            throw new BadRequestException("Patient profile already exists for this user");
        }
        Patient patient = new Patient();
        applyRequest(patient, request);
        patient.setUserId(userId);
        return patientRepository.save(patient);
    }

    public Patient getByUserId(String userId) {
        return patientRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found for user " + userId));
    }

    public Patient getById(String patientId) {
        return patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + patientId));
    }

    public Patient updateProfile(String userId, PatientRequest request) {
        Patient patient = getByUserId(userId);
        applyRequest(patient, request);
        patient.setUpdatedAt(Instant.now());
        return patientRepository.save(patient);
    }

    private void applyRequest(Patient patient, PatientRequest request) {
        patient.setName(request.getName());
        patient.setEmail(request.getEmail());
        patient.setDob(request.getDob());
        patient.setGender(request.getGender());
        patient.setBloodGroup(request.getBloodGroup());
        if (request.getAllergies() != null) patient.setAllergies(request.getAllergies());
        if (request.getChronicConditions() != null) patient.setChronicConditions(request.getChronicConditions());
        if (request.getCurrentMedications() != null) patient.setCurrentMedications(request.getCurrentMedications());
        Patient.Address address = new Patient.Address();
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPincode(request.getPincode());
        patient.setAddress(address);
    }

    public SymptomLog addSymptomLog(String patientId, SymptomLogRequest request) {
        // ensure patient exists
        getById(patientId);
        SymptomLog log = new SymptomLog(patientId, request.getSymptoms(), request.getSeverity(), request.getReportedVia());
        return symptomLogRepository.save(log);
    }

    public List<SymptomLog> getSymptomHistory(String patientId) {
        return symptomLogRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
    }
}
