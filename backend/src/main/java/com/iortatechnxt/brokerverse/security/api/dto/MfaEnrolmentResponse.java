package com.iortatechnxt.brokerverse.security.api.dto;

import java.util.List;

/**
 * The outcome of a confirmed enrolment of an authenticator app.
 *
 * @param recoveryCodes the ten single-use recovery codes, shown once
 * @param signIn the open session when the enrolment completed a sign-in, otherwise null
 */
public record MfaEnrolmentResponse(List<String> recoveryCodes, LoginResponse signIn) {}
