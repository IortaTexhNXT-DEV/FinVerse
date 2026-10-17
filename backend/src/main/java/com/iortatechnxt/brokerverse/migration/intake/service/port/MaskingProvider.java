package com.iortatechnxt.brokerverse.migration.intake.service.port;

/**
 * Port of the deterministic masking of personal data outside production (DATA_MIGRATION_DESIGN
 * sections 5.3 and 23): the same input always gives the same keyed digest, so the same person gets
 * the same substitute in every file and duplicate detection still works. The default adapter is a
 * keyed HMAC whose key comes from the secrets store of the environment.
 */
public interface MaskingProvider {

  /**
   * Whether a key is configured.
   *
   * @return true when masking can run
   */
  boolean configured();

  /**
   * A keyed digest of a value.
   *
   * @param purpose what is masked (keeps digests of different fields apart)
   * @param value normalised value
   * @return digest bytes
   */
  byte[] digest(String purpose, String value);
}
