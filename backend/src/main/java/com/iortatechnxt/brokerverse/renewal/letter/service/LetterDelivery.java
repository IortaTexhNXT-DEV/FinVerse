package com.iortatechnxt.brokerverse.renewal.letter.service;

import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.messaging.service.OutboundEmail;
import com.iortatechnxt.brokerverse.renewal.channel.domain.ChannelMessage;
import com.iortatechnxt.brokerverse.renewal.channel.domain.ChannelStatus;
import com.iortatechnxt.brokerverse.renewal.channel.service.ChannelGateways;
import com.iortatechnxt.brokerverse.renewal.channel.service.ChannelService;
import com.iortatechnxt.brokerverse.renewal.channel.service.PasswordConvention;
import com.iortatechnxt.brokerverse.renewal.domain.LetterBatch;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalLetter;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * The delivery of a letter (FRRN.022.02, FRRN.023.02, FRRN.024.01): its file name, the password of
 * the protected file by the client's convention, and the hand-off to CCM when the client documents
 * go through CCM (else the protected e-mail with the password in a separate e-mail).
 */
@Component
public class LetterDelivery {

  private final LetterFileNames names;
  private final ChannelService channels;
  private final PasswordConvention passwords;

  /**
   * Creates the delivery.
   *
   * @param names file names
   * @param channels CCM
   * @param passwords password convention
   */
  public LetterDelivery(
      LetterFileNames names, ChannelService channels, PasswordConvention passwords) {
    this.names = names;
    this.channels = channels;
    this.passwords = passwords;
  }

  /**
   * The file name of a letter.
   *
   * @param c renewal
   * @param letter letter
   * @return file name
   */
  public String fileName(RenewalCandidate c, RenewalLetter letter) {
    return names.of(
        c,
        letter.getType(),
        letter.getNotice(),
        letter.getLetterNo(),
        BusinessClock.dateOf(letter.getGeneratedAt()),
        letter.getTemplateCode());
  }

  /**
   * Whether the letters go through CCM.
   *
   * @return true for CCM
   */
  public boolean ccm() {
    return channels.ccm();
  }

  /**
   * The protection of the e-mail of a letter: the client's password and its hint, else a password
   * chosen by the messaging service, sent in a separate e-mail.
   *
   * @param clientId client, may be null
   * @return protection
   */
  public OutboundEmail.Protection protection(Long clientId) {
    return passwords
        .of(clientId)
        .map(p -> new OutboundEmail.Protection(p.value(), true, p.hint()))
        .orElse(new OutboundEmail.Protection(null, true, null));
  }

  /**
   * Hands a letter to CCM.
   *
   * @param c renewal
   * @param letter letter, stored
   * @param mail recipient, subject and text
   * @return the CCM message, or the failure message of the client
   */
  public Handoff toCcm(RenewalCandidate c, RenewalLetter letter, Mail mail) {
    ChannelService.Sent sent =
        channels.send(
            c.getCompanyId(),
            new ChannelService.Outbound(
                ChannelGateways.CCM,
                new ChannelMessage.Document(
                    letter.getType().name(),
                    letter.getLetterNo(),
                    c.getId(),
                    c.getRenewalRef(),
                    fileName(c, letter),
                    letter.getAttachmentId()),
                new ChannelMessage.Address(
                    mail.to(),
                    null,
                    mail.subject(),
                    mail.body(),
                    c.getSnapshot().client() == null
                        ? null
                        : c.getSnapshot().client().clientId())));
    boolean failed = sent.message().getStatus() == ChannelStatus.FAILED;
    return new Handoff(
        sent.message().getMessageNo(),
        failed
            ? Optional.of(
                LetterWriter.label(new LetterBatch.Kind(letter.getType(), letter.getNotice()))
                    + " generated but transmission to CCM failed. Please review the communication"
                    + " details and retry. "
                    + sent.error())
            : Optional.empty());
  }

  /**
   * A letter by e-mail.
   *
   * @param to recipient
   * @param subject subject
   * @param body text
   */
  public record Mail(String to, String subject, String body) {}

  /**
   * The hand-off of a letter to CCM.
   *
   * @param messageNo CCM message
   * @param failure failure message, empty when submitted or pending
   */
  public record Handoff(String messageNo, Optional<String> failure) {}
}
