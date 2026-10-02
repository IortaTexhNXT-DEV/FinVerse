package com.iortatechnxt.brokerverse.submitted.service.adapter;

import com.iortatechnxt.brokerverse.common.util.Sha256;
import com.iortatechnxt.brokerverse.submitted.service.port.SignatureProvider;
import java.nio.charset.StandardCharsets;
import java.time.Clock;

/**
 * Default {@link SignatureProvider} while no qualified e-signature is connected (SP SQ07): the
 * approver's name, position and time are stamped with a SHA-256 hash of the document facts, the
 * level, the signer and the time.
 */
public class StampedSignature implements SignatureProvider {

  /** Method of a stamped signature. */
  public static final String METHOD = "STAMPED";

  private final Clock clock;

  /**
   * Creates the adapter.
   *
   * @param clock clock
   */
  public StampedSignature(Clock clock) {
    this.clock = clock;
  }

  @Override
  public Signature sign(SignRequest r) {
    var at = clock.instant();
    String facts =
        String.join(
            "|",
            r.documentType(),
            r.documentNo(),
            String.valueOf(r.level()),
            r.signer(),
            r.position(),
            at.toString(),
            r.contentDigest() == null ? "" : r.contentDigest());
    return new Signature(METHOD, at, Sha256.hex(facts.getBytes(StandardCharsets.UTF_8)));
  }
}
