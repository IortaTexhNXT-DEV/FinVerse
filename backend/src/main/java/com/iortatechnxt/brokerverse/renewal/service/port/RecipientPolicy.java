package com.iortatechnxt.brokerverse.renewal.service.port;

import java.util.Optional;

/**
 * Port of Renewal: whether a client or insurer address may receive a renewal letter or file
 * (BRRN.010 negative scenario 6: approved domains and TLS, RQ16). The default adapter allows every
 * well-formed address; the protection of the files is always on. A messaging change implements
 * BDOI's rules once they are defined.
 */
public interface RecipientPolicy {

  /**
   * Why an address is refused.
   *
   * @param address e-mail address
   * @return the refusal, empty when the address may receive renewal mail
   */
  Optional<String> refusal(String address);
}
