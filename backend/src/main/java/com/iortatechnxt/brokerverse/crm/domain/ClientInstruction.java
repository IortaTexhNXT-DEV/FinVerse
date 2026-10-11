package com.iortatechnxt.brokerverse.crm.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * A special instruction on a client (BRNB.091): type, text and effectivity, shown as a banner on
 * every screen of the client's quotations and accounts while in force.
 */
@Entity
@Table(name = "crm_client_instruction")
public class ClientInstruction extends BaseEntity {

  @Column(name = "client_id", nullable = false, updatable = false)
  private Long clientId;

  @Column(name = "instruction_type", nullable = false, length = 40)
  private String instructionType;

  @Column(name = "instruction_text", nullable = false, length = 1000)
  private String text;

  @Column(name = "effective_from", nullable = false)
  private LocalDate effectiveFrom;

  @Column(name = "effective_to")
  private LocalDate effectiveTo;

  @Column(nullable = false)
  private boolean active = true;

  protected ClientInstruction() {}

  /**
   * Creates an instruction.
   *
   * @param clientId client
   * @param content type, text and effectivity
   */
  public ClientInstruction(Long clientId, InstructionContent content) {
    this.clientId = clientId;
    change(content);
  }

  /**
   * Changes type, text or effectivity.
   *
   * @param content new content
   */
  public final void change(InstructionContent content) {
    if (!active) {
      throw new BusinessRuleException(
          "INSTRUCTION_ENDED", "An ended instruction cannot be changed");
    }
    if (content.effectiveTo() != null && content.effectiveTo().isBefore(content.effectiveFrom())) {
      throw new BusinessRuleException(
          "INSTRUCTION_DATES", "The end date must not be before the start date");
    }
    this.instructionType = content.type();
    this.text = content.text().trim();
    this.effectiveFrom = content.effectiveFrom();
    this.effectiveTo = content.effectiveTo();
  }

  /** Ends the instruction (kept for history). */
  public void end() {
    this.active = false;
  }

  /**
   * Whether the instruction applies on a date.
   *
   * @param date date
   * @return true when active and within its effectivity
   */
  public boolean inForceOn(LocalDate date) {
    return active
        && !date.isBefore(effectiveFrom)
        && (effectiveTo == null || !date.isAfter(effectiveTo));
  }

  /**
   * Content as a record (history from / to values).
   *
   * @return content
   */
  public InstructionContent content() {
    return new InstructionContent(instructionType, text, effectiveFrom, effectiveTo);
  }

  public Long getClientId() {
    return clientId;
  }

  public String getInstructionType() {
    return instructionType;
  }

  public String getText() {
    return text;
  }

  public LocalDate getEffectiveFrom() {
    return effectiveFrom;
  }

  public LocalDate getEffectiveTo() {
    return effectiveTo;
  }

  public boolean isActive() {
    return active;
  }
}
