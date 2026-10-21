package com.carebridge.provider.service;

import com.carebridge.common.exception.BadRequestException;
import com.carebridge.common.exception.ResourceNotFoundException;
import com.carebridge.provider.dto.DoctorRequest;
import com.carebridge.provider.dto.SlotRequest;
import com.carebridge.provider.model.AvailabilitySlot;
import com.carebridge.provider.model.Doctor;
import com.carebridge.provider.repository.AvailabilitySlotRepository;
import com.carebridge.provider.repository.DoctorRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class ProviderService {

    private final DoctorRepository doctorRepository;
    private final AvailabilitySlotRepository slotRepository;

    public ProviderService(DoctorRepository doctorRepository, AvailabilitySlotRepository slotRepository) {
        this.doctorRepository = doctorRepository;
        this.slotRepository = slotRepository;
    }

    public Doctor createProfile(String userId, DoctorRequest request) {
        if (doctorRepository.findByUserId(userId).isPresent()) {
            throw new BadRequestException("Doctor profile already exists for this user");
        }
        Doctor doctor = new Doctor();
        doctor.setUserId(userId);
        applyRequest(doctor, request);
        return doctorRepository.save(doctor);
    }

    public Doctor getByUserId(String userId) {
        return doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found for user " + userId));
    }

    public Doctor getById(String id) {
        return doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found: " + id));
    }

    public Doctor updateProfile(String userId, DoctorRequest request) {
        Doctor doctor = getByUserId(userId);
        applyRequest(doctor, request);
        return doctorRepository.save(doctor);
    }

    private void applyRequest(Doctor doctor, DoctorRequest request) {
        doctor.setName(request.getName());
        doctor.setEmail(request.getEmail());
        doctor.setSpecialty(request.getSpecialty());
        doctor.setQualifications(request.getQualifications());
        doctor.setHospital(request.getHospital());
        doctor.setCity(request.getCity());
        doctor.setConsultationFee(request.getConsultationFee());
        doctor.setYearsOfExperience(request.getYearsOfExperience());
    }

    /**
     * Core search used both by the patient-facing UI and, critically, by the
     * AI Agent Orchestrator's "Doctor Search Tool Node" during an agentic workflow.
     */
    public List<Doctor> search(String specialty, String city) {
        if (specialty != null && city != null) {
            return doctorRepository.findBySpecialtyIgnoreCaseAndCityIgnoreCaseAndActiveTrue(specialty, city);
        } else if (specialty != null) {
            return doctorRepository.findBySpecialtyIgnoreCaseAndActiveTrue(specialty);
        }
        return doctorRepository.findByActiveTrue();
    }

    public AvailabilitySlot addSlot(String doctorId, SlotRequest request) {
        getById(doctorId); // validate exists
        AvailabilitySlot slot = new AvailabilitySlot(doctorId, request.getDate(), request.getStartTime(), request.getEndTime());
        return slotRepository.save(slot);
    }

    public List<AvailabilitySlot> getAvailableSlots(String doctorId) {
        return slotRepository.findByDoctorIdAndBookedFalseAndDateGreaterThanEqual(doctorId, LocalDate.now());
    }

    /** Called by appointment-service when a booking is confirmed. */
    public AvailabilitySlot markSlotBooked(String slotId) {
        AvailabilitySlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found: " + slotId));
        if (slot.isBooked()) {
            throw new BadRequestException("Slot is already booked");
        }
        slot.setBooked(true);
        return slotRepository.save(slot);
    }
}
