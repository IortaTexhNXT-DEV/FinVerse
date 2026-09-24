package com.iortatechnxt.brokerverse.messaging.service;

/**
 * Port that delivers e-mails. {@link SmtpMailTransport} is used when {@code
 * brokerverse.mail.enabled=true}; otherwise {@link SimulatedMailTransport} records the message as
 * sent without contacting any server (demo, test and non-production environments).
 */
public interface MailTransport {

  /**
   * Delivers an e-mail.
   *
   * @param envelope message
   * @return true when the delivery was simulated
   * @throws MailDeliveryException when the message could not be delivered
   */
  boolean send(MailEnvelope envelope) throws MailDeliveryException;
}
