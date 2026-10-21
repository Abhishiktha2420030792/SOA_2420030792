package com.carebridge.appointment.repository;

import com.carebridge.appointment.model.Referral;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ReferralRepository extends MongoRepository<Referral, String> {
    List<Referral> findByPatientId(String patientId);
}
