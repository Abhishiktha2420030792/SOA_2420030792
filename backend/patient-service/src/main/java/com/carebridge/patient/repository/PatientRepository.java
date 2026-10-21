package com.carebridge.patient.repository;

import com.carebridge.patient.model.Patient;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface PatientRepository extends MongoRepository<Patient, String> {
    Optional<Patient> findByUserId(String userId);
    boolean existsByUserId(String userId);
}
