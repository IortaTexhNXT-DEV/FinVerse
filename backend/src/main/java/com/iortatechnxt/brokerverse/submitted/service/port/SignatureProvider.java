package com.iortatechnxt.brokerverse.submitted.service.port;

import java.time.Instant;

/**
 * Port: signs an approval level of an IAAF or a Terms of Reference (BRIDSP-07, 18; SP SQ07;
 * SUBMITTED_POLICIES_DESIGN section 2.2). The default {@code StampedSignature} stamps the
 * approver's name, position and time with a SHA-256 hash of the document facts; a qualified
 * e-signature adapter replaces it without changing the approval flow.
 */
public interface SignatureProvider {

  /**
   * Signs a level.
   *
   * @param request document and signer
   * @return the signature
   */
  Signature sign(SignRequest request);

  /**
   * What is signed and by whom.
   *
   * @param documentType IAAF or TOR
   * @param documentNo document number
   * @param level approval level
   * @param signer username
   * @param signerName display name
   * @param position signatory title
   * @param contentDigest digest of the facts of the document at signing
   */
  record SignRequest(
      String documentType,
      String documentNo,
      int level,
      String signer,
      String signerName,
      String position,
      String contentDigest) {}

  /**
   * A signature.
   *
   * @param method STAMPED or ESIG
   * @param signedAt time
   * @param hash hash of the signature
   */
  record Signature(String method, Instant signedAt, String hash) {}
}
