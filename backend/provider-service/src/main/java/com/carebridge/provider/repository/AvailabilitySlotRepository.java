package com.carebridge.provider.repository;

import com.carebridge.provider.model.AvailabilitySlot;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;

public interface AvailabilitySlotRepository extends MongoRepository<AvailabilitySlot, String> {
    List<AvailabilitySlot> findByDoctorIdAndBookedFalseAndDateGreaterThanEqual(String doctorId, LocalDate fromDate);
    List<AvailabilitySlot> findByDoctorId(String doctorId);
}
