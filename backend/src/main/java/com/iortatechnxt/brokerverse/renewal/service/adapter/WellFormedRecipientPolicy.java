package com.iortatechnxt.brokerverse.renewal.service.adapter;

import com.iortatechnxt.brokerverse.renewal.service.port.RecipientPolicy;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * The default {@link RecipientPolicy}: any well-formed e-mail address may receive renewal mail (the
 * attachments are protected in every case).
 */
public class WellFormedRecipientPolicy implements RecipientPolicy {

  private static final Pattern ADDRESS = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");

  @Override
  public Optional<String> refusal(String address) {
    if (address == null || !ADDRESS.matcher(address.strip()).matches()) {
      return Optional.of(address + " is not a valid e-mail address");
    }
    return Optional.empty();
  }
}
