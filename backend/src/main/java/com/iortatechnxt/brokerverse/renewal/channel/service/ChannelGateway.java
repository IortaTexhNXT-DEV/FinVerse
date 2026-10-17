package com.iortatechnxt.brokerverse.renewal.channel.service;

import com.iortatechnxt.brokerverse.renewal.channel.domain.ChannelMessage;
import com.iortatechnxt.brokerverse.renewal.channel.domain.ChannelStatus;
import java.util.List;
import java.util.Optional;

/** A delivery channel connection: the live interface or its simulator. */
public interface ChannelGateway {

  /** Error code: the channel cannot be reached. */
  String UNAVAILABLE = "UNAVAILABLE";

  /** Error code: a recipient address is invalid. */
  String INVALID_RECIPIENT = "INVALID_RECIPIENT";

  /** Error code: the channel refused the message. */
  String REJECTED = "REJECTED";

  /**
   * The channel.
   *
   * @return CCM or MFT
   */
  String channel();

  /**
   * Whether this is the live interface (not the simulator).
   *
   * @return true for the live interface
   */
  boolean live();

  /**
   * Transmits a message.
   *
   * @param t message, recipients and file
   * @return the answer of the channel
   */
  Reply transmit(Transmission t);

  /**
   * The delivery status of a transmitted message.
   *
   * @param message message with its external reference
   * @return status, empty when unchanged or unknown
   */
  Optional<Report> status(ChannelMessage message);

  /**
   * Checks the connection.
   *
   * @return the answer of the channel
   */
  Reply check();

  /**
   * A message to transmit.
   *
   * @param messageNo message number
   * @param to recipients (CCM) or the folder (MFT)
   * @param cc copy recipients
   * @param subject subject
   * @param body text of the communication
   * @param fileName file name
   * @param content file, may be null
   * @param password password of the protected file, may be null
   */
  record Transmission(
      String messageNo,
      List<String> to,
      List<String> cc,
      String subject,
      String body,
      String fileName,
      byte[] content,
      String password) {

    /** Defensive copies. */
    public Transmission {
      to = List.copyOf(to);
      cc = cc == null ? List.of() : List.copyOf(cc);
      content = content == null ? null : content.clone();
    }

    @Override
    public byte[] content() {
      return content == null ? null : content.clone();
    }

    @Override
    public String toString() {
      return "Transmission[" + messageNo + "]";
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof Transmission t && t.messageNo.equals(messageNo);
    }

    @Override
    public int hashCode() {
      return messageNo.hashCode();
    }
  }

  /**
   * The answer of a channel.
   *
   * @param accepted whether accepted
   * @param reference transaction reference of the channel
   * @param errorCode UNAVAILABLE, INVALID_RECIPIENT or REJECTED when refused
   * @param error error text
   */
  record Reply(boolean accepted, String reference, String errorCode, String error) {

    /**
     * An acceptance.
     *
     * @param reference transaction reference
     * @return reply
     */
    public static Reply accepted(String reference) {
      return new Reply(true, reference, null, null);
    }

    /**
     * A refusal.
     *
     * @param code error code
     * @param error text
     * @return reply
     */
    public static Reply refused(String code, String error) {
      return new Reply(false, null, code, error);
    }
  }

  /**
   * A delivery status reported by the channel.
   *
   * @param status status
   * @param detail detail, may be null
   */
  record Report(ChannelStatus status, String detail) {}
}
