package com.carebridge.common.security;

/**
 * Roles recognized platform-wide. Embedded as a claim inside the JWT
 * issued by auth-service and re-validated by every downstream service.
 */
public enum Role {
    PATIENT,
    DOCTOR,
    ADMIN
}
