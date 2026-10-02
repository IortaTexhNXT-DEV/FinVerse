package com.iortatechnxt.brokerverse.crm.api.dto;

import com.iortatechnxt.brokerverse.crm.domain.ClientInstruction;
import com.iortatechnxt.brokerverse.crm.domain.InstructionContent;
import com.iortatechnxt.brokerverse.crm.domain.NoteHistory;
import com.iortatechnxt.brokerverse.crm.service.ClientBanner;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Request and response records of client tags and special instructions (BRNB.091). */
public interface ClientNotesDtos {

  /**
   * Tag to add.
   *
   * @param tagCode tag (list of values CLIENT_TAG)
   */
  record TagRequest(@NotBlank @Size(max = 40) String tagCode) {}

  /**
   * New or changed instruction.
   *
   * @param type type (list of values INSTRUCTION_TYPE)
   * @param text text
   * @param effectiveFrom first day
   * @param effectiveTo last day, null when open-ended
   */
  record InstructionRequest(
      @NotBlank @Size(max = 40) String type,
      @NotBlank @Size(max = 1000) String text,
      @NotNull LocalDate effectiveFrom,
      LocalDate effectiveTo) {

    /**
     * Instruction content.
     *
     * @return content
     */
    public InstructionContent content() {
      return new InstructionContent(type, text.trim(), effectiveFrom, effectiveTo);
    }
  }

  /**
   * Banner content: active tags and instructions in force.
   *
   * @param clientId client
   * @param clientCode client code
   * @param tags tags
   * @param instructions instructions in force
   */
  record BannerResponse(
      Long clientId, String clientCode, List<TagItem> tags, List<BannerInstruction> instructions) {

    /**
     * Maps a banner.
     *
     * @param b banner
     * @return response
     */
    public static BannerResponse from(ClientBanner b) {
      return new BannerResponse(
          b.clientId(),
          b.clientCode(),
          b.tags().stream().map(t -> new TagItem(t.code(), t.label())).toList(),
          b.instructions().stream()
              .map(
                  i ->
                      new BannerInstruction(
                          i.id(),
                          i.type(),
                          i.typeLabel(),
                          i.text(),
                          i.effectiveFrom(),
                          i.effectiveTo()))
              .toList());
    }
  }

  /**
   * A tag.
   *
   * @param code code
   * @param label label
   */
  record TagItem(String code, String label) {}

  /**
   * An instruction in force.
   *
   * @param id id
   * @param type type code
   * @param typeLabel type label
   * @param text text
   * @param effectiveFrom first day
   * @param effectiveTo last day
   */
  record BannerInstruction(
      Long id,
      String type,
      String typeLabel,
      String text,
      LocalDate effectiveFrom,
      LocalDate effectiveTo) {}

  /**
   * An instruction, in force or not.
   *
   * @param id id
   * @param type type
   * @param text text
   * @param effectiveFrom first day
   * @param effectiveTo last day
   * @param active false once ended
   * @param createdBy author
   * @param createdAt creation time
   */
  record InstructionResponse(
      Long id,
      String type,
      String text,
      LocalDate effectiveFrom,
      LocalDate effectiveTo,
      boolean active,
      String createdBy,
      Instant createdAt) {

    /**
     * Maps an instruction.
     *
     * @param i instruction
     * @return response
     */
    public static InstructionResponse from(ClientInstruction i) {
      return new InstructionResponse(
          i.getId(),
          i.getInstructionType(),
          i.getText(),
          i.getEffectiveFrom(),
          i.getEffectiveTo(),
          i.isActive(),
          i.getCreatedBy(),
          i.getCreatedAt());
    }
  }

  /**
   * A change of a tag or instruction.
   *
   * @param id id
   * @param kind TAG or INSTRUCTION
   * @param ref tag code or instruction id
   * @param action ADDED, CHANGED or REMOVED
   * @param fromValue before
   * @param toValue after
   * @param actor user
   * @param occurredAt time
   */
  record HistoryResponse(
      Long id,
      String kind,
      String ref,
      String action,
      String fromValue,
      String toValue,
      String actor,
      Instant occurredAt) {

    /**
     * Maps a history entry.
     *
     * @param h entry
     * @return response
     */
    public static HistoryResponse from(NoteHistory h) {
      return new HistoryResponse(
          h.getId(),
          h.getItemKind(),
          h.getItemRef(),
          h.getAction(),
          h.getFromValue(),
          h.getToValue(),
          h.getActor(),
          h.getOccurredAt());
    }
  }

  /**
   * Every instruction and the change history of a client.
   *
   * @param instructions instructions, newest first
   * @param history changes, newest first
   */
  record NotesResponse(List<InstructionResponse> instructions, List<HistoryResponse> history) {}
}
