package com.iortatechnxt.brokerverse.security.api.dto;

/**
 * How users sign in on this deployment (shown by the sign-in page before anyone is signed in).
 *
 * @param mode LOCAL, DIRECTORY, OIDC or SAML
 * @param singleSignOn whether the users sign in at an identity provider
 * @param providerLabel name of the identity provider on the button, null without single sign-on
 * @param passwordSignIn whether the password form is the main sign-in (LOCAL, DIRECTORY); in single
 *     sign-on mode the form stays available for the break-glass administrators
 * @param passwordReset whether "Forgot password?" is offered
 */
public record SignInOptionsResponse(
    String mode,
    boolean singleSignOn,
    String providerLabel,
    boolean passwordSignIn,
    boolean passwordReset) {}
