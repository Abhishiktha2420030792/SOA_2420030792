package com.carebridge.provider.repository;

import com.carebridge.provider.model.Doctor;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface DoctorRepository extends MongoRepository<Doctor, String> {
    Optional<Doctor> findByUserId(String userId);
    List<Doctor> findBySpecialtyIgnoreCaseAndCityIgnoreCaseAndActiveTrue(String specialty, String city);
    List<Doctor> findBySpecialtyIgnoreCaseAndActiveTrue(String specialty);
    List<Doctor> findByActiveTrue();
}
