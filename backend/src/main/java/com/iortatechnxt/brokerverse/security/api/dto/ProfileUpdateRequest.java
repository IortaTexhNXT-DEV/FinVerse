package com.iortatechnxt.brokerverse.security.api.dto;

import jakarta.validation.constraints.Size;

/**
 * The signed-in user's contact details (UQ17): e-mail address (required) and mobile number (blank
 * clears it). Checked by {@code AuthProfileService} with field errors.
 *
 * @param email e-mail address
 * @param mobileNo mobile number
 */
public record ProfileUpdateRequest(
    @Size(max = 120) String email, @Size(max = 30) String mobileNo) {}
