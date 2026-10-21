package com.carebridge.patient.repository;

import com.carebridge.patient.model.SymptomLog;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface SymptomLogRepository extends MongoRepository<SymptomLog, String> {
    List<SymptomLog> findByPatientIdOrderByCreatedAtDesc(String patientId);
}
