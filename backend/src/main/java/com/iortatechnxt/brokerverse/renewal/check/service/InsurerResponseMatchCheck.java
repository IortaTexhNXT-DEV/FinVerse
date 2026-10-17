package com.iortatechnxt.brokerverse.renewal.check.service;

import com.iortatechnxt.brokerverse.renewal.domain.InsurerResponse;
import com.iortatechnxt.brokerverse.renewal.domain.MatchOutcome;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * {@code INSURER_RESPONSE_MATCH} (BRRN.035): the latest insurer response of the renewal is matched,
 * on time and not conflicting. A mismatched, late or conflicting latest response fails (Exception)
 * and never progresses the renewal; only an override with remarks lets it proceed (the check keeps
 * failing, never Clean).
 */
@Component
public class InsurerResponseMatchCheck implements RenewalCheck {

  /** Check code. */
  public static final String CODE = "INSURER_RESPONSE_MATCH";

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public Verdict evaluate(CheckContext context) {
    InsurerResponse latest = context.responses().stream().findFirst().orElse(null);
    if (latest == null) {
      return Verdict.notApplicable("No insurer response yet");
    }
    Verdict problem = problem(latest);
    return problem != null
        ? problem
        : Verdict.pass(
            "Insurer response "
                + latest.getResponse().name().replace('_', ' ').toLowerCase(Locale.ROOT)
                + " matched");
  }

  private static Verdict problem(InsurerResponse latest) {
    if (latest.getMatchOutcome() != MatchOutcome.MATCHED) {
      return Verdict.fail(
          "The insurer response does not match the renewal", latest.getMatchOutcome().name());
    }
    if (latest.isConflicting()) {
      return Verdict.fail("The insurer sent conflicting responses", "CONFLICTING");
    }
    return latest.isLate()
        ? Verdict.fail("The insurer response came after the reply date", "LATE")
        : null;
  }
}
