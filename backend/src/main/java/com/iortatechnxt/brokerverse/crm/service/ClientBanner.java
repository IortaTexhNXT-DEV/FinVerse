package com.iortatechnxt.brokerverse.crm.service;

import java.time.LocalDate;
import java.util.List;

/**
 * What every screen of a client's records shows in its banner (BRNB.091): active tags and the
 * special instructions in force today.
 *
 * @param clientId client
 * @param clientCode client or prospect code
 * @param tags active tags
 * @param instructions instructions in force
 */
public record ClientBanner(
    Long clientId, String clientCode, List<Tag> tags, List<Instruction> instructions) {

  /** Defensive copies. */
  public ClientBanner {
    tags = List.copyOf(tags);
    instructions = List.copyOf(instructions);
  }

  /**
   * An active tag.
   *
   * @param code tag code
   * @param label tag label
   */
  public record Tag(String code, String label) {}

  /**
   * An instruction in force.
   *
   * @param id instruction
   * @param type type code
   * @param typeLabel type label
   * @param text text
   * @param effectiveFrom first day
   * @param effectiveTo last day, null when open-ended
   */
  public record Instruction(
      Long id,
      String type,
      String typeLabel,
      String text,
      LocalDate effectiveFrom,
      LocalDate effectiveTo) {}
}
