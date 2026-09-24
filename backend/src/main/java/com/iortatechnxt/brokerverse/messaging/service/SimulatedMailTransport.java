package com.iortatechnxt.brokerverse.messaging.service;

import java.util.Locale;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Default transport when no mail server is configured: logs the message and reports it as sent
 * (simulated). Addresses in the reserved {@code .invalid} top-level domain (RFC 2606) are rejected,
 * so failed deliveries can be demonstrated and tested.
 */
@Component
@ConditionalOnProperty(
    name = "brokerverse.mail.enabled",
    havingValue = "false",
    matchIfMissing = true)
public class SimulatedMailTransport implements MailTransport {

  private static final Logger LOG = LoggerFactory.getLogger(SimulatedMailTransport.class);

  @Override
  public boolean send(MailEnvelope envelope) throws MailDeliveryException {
    String rejected =
        Stream.concat(envelope.to().stream(), envelope.cc().stream())
            .filter(a -> a.toLowerCase(Locale.ROOT).endsWith(".invalid"))
            .findFirst()
            .orElse(null);
    if (rejected != null) {
      throw new MailDeliveryException("Mailbox unavailable: " + rejected, null);
    }
    if (LOG.isInfoEnabled()) {
      LOG.info(
          "Simulated e-mail to {} ({} attachments): {}",
          envelope.to().size(),
          envelope.attachments().size(),
          envelope.subject());
    }
    return true;
  }
}
