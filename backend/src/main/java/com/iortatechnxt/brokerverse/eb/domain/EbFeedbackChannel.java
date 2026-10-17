package com.iortatechnxt.brokerverse.eb.domain;

/**
 * How the client's feedback reached BDOI (FR-EB-023). BDOI Drop 2 has no partner portal: the client
 * answers by e-mail, telephone, meeting or letter, or through the AO.
 */
public enum EbFeedbackChannel {
  /** Given to the AO directly. */
  AO,
  /** By e-mail. */
  EMAIL,
  /** By telephone. */
  PHONE,
  /** In a meeting. */
  MEETING,
  /** By letter. */
  LETTER
}
