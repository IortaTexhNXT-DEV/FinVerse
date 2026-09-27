package com.iortatechnxt.brokerverse.common.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;

/** The response to an unexpected failure: a clean message and the reference in its own field. */
class UnexpectedErrorTest {

  @Test
  void theSupportReferenceIsASeparateFieldAndNeverInTheMessage() {
    ProblemDetail pd =
        new GlobalExceptionHandler().handleUnexpected(new IllegalStateException("boom"));
    assertThat(pd.getStatus()).isEqualTo(500);
    assertThat(pd.getProperties()).containsKey("reference").containsEntry("code", "INTERNAL_ERROR");
    String reference = String.valueOf(pd.getProperties().get("reference"));
    assertThat(reference).matches("[0-9a-f-]{36}");
    assertThat(pd.getDetail()).doesNotContain(reference).doesNotContain("boom");
  }
}
