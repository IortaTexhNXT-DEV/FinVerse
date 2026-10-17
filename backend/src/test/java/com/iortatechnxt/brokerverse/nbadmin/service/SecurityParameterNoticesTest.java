package com.iortatechnxt.brokerverse.nbadmin.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** The subject of a security-setting notice is the beginning of the setting's description. */
class SecurityParameterNoticesTest {

  @Test
  void theSubjectIsTheBeginningOfALongDescription() {
    String description =
        "true: no forgotten-password link on the Login page, and a password is reset only by"
            + " another System Administrator, with single sign-on only for the break-glass accounts"
            + " (the bank's process); false: the self-service reset link for local accounts";
    String subject = SecurityParameterNotices.summary(description);
    assertThat(subject)
        .hasSizeLessThanOrEqualTo(SecurityParameterNotices.SUBJECT_LENGTH)
        .endsWith("…");
    assertThat(SecurityParameterNotices.summary("Lock-out after failed sign-ins"))
        .isEqualTo("Lock-out after failed sign-ins");
    assertThat(SecurityParameterNotices.summary(null)).isEmpty();
  }
}
