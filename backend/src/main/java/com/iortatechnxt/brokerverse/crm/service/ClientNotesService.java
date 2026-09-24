package com.iortatechnxt.brokerverse.crm.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.domain.ClientInstruction;
import com.iortatechnxt.brokerverse.crm.domain.ClientInstructionRepository;
import com.iortatechnxt.brokerverse.crm.domain.ClientTag;
import com.iortatechnxt.brokerverse.crm.domain.ClientTagRepository;
import com.iortatechnxt.brokerverse.crm.domain.InstructionContent;
import com.iortatechnxt.brokerverse.crm.domain.NoteChange;
import com.iortatechnxt.brokerverse.crm.domain.NoteHistory;
import com.iortatechnxt.brokerverse.crm.domain.NoteHistoryRepository;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Client tags and special instructions (BRNB.091). Tags come from the controlled list CLIENT_TAG,
 * instructions have a type (INSTRUCTION_TYPE), a text and effective dates. Every change is kept in
 * the change history (who, when, from and to) and audited.
 */
@Service
@Transactional
public class ClientNotesService {

  private static final String TAG_LIST = "CLIENT_TAG";
  private static final String INSTRUCTION_LIST = "INSTRUCTION_TYPE";
  private static final String TAG = "TAG";
  private static final String INSTRUCTION = "INSTRUCTION";

  private final ClientService clients;
  private final ClientTagRepository tags;
  private final ClientInstructionRepository instructions;
  private final NoteHistoryRepository history;
  private final LovService lovs;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param clients clients
   * @param tags tags
   * @param instructions instructions
   * @param history change history
   * @param lovs lists of values
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public ClientNotesService(
      ClientService clients,
      ClientTagRepository tags,
      ClientInstructionRepository instructions,
      NoteHistoryRepository history,
      LovService lovs,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.clients = clients;
    this.tags = tags;
    this.instructions = instructions;
    this.history = history;
    this.lovs = lovs;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Active tags and instructions in force today, for banners on quotation and account screens.
   *
   * @param clientId client
   * @return banner content
   */
  @Transactional(readOnly = true)
  public ClientBanner banner(Long clientId) {
    Client client = clients.get(clientId);
    LocalDate today = LocalDate.now(clock);
    List<ClientBanner.Tag> activeTags =
        tags.findByClientIdAndActiveTrueOrderByTagCode(clientId).stream()
            .map(t -> new ClientBanner.Tag(t.getTagCode(), lovs.label(TAG_LIST, t.getTagCode())))
            .toList();
    List<ClientBanner.Instruction> inForce =
        instructions.findByClientIdOrderByIdDesc(clientId).stream()
            .filter(i -> i.inForceOn(today))
            .map(this::toBanner)
            .toList();
    return new ClientBanner(clientId, client.getCode(), activeTags, inForce);
  }

  /**
   * Every instruction of a client, including ended and future ones.
   *
   * @param clientId client
   * @return instructions, newest first
   */
  @Transactional(readOnly = true)
  public List<ClientInstruction> instructions(Long clientId) {
    clients.get(clientId);
    return instructions.findByClientIdOrderByIdDesc(clientId);
  }

  /**
   * Change history of tags and instructions.
   *
   * @param clientId client
   * @return changes, newest first
   */
  @Transactional(readOnly = true)
  public List<NoteHistory> history(Long clientId) {
    clients.get(clientId);
    return history.findByClientIdOrderByIdDesc(clientId);
  }

  /**
   * Tags a client.
   *
   * @param clientId client
   * @param tagCode tag (list of values CLIENT_TAG)
   * @return the tag
   */
  public ClientTag addTag(Long clientId, String tagCode) {
    Client client = clients.requireUsable(clientId);
    lovs.requireValid(TAG_LIST, tagCode, LocalDate.now(clock));
    if (tags.findByClientIdAndTagCodeAndActiveTrue(clientId, tagCode).isPresent()) {
      throw new BusinessRuleException(
          "CLIENT_TAG_EXISTS", "The client is already tagged " + lovs.label(TAG_LIST, tagCode));
    }
    ClientTag tag = tags.save(new ClientTag(clientId, tagCode));
    log(client, new NoteChange(TAG, tagCode, "ADDED", null, lovs.label(TAG_LIST, tagCode)));
    return tag;
  }

  /**
   * Removes a tag (history is kept).
   *
   * @param clientId client
   * @param tagCode tag
   */
  public void removeTag(Long clientId, String tagCode) {
    Client client = clients.requireUsable(clientId);
    ClientTag tag =
        tags.findByClientIdAndTagCodeAndActiveTrue(clientId, tagCode)
            .orElseThrow(() -> new ResourceNotFoundException("Client tag", tagCode));
    tag.remove(currentUser.username(), clock.instant());
    log(client, new NoteChange(TAG, tagCode, "REMOVED", lovs.label(TAG_LIST, tagCode), null));
  }

  /**
   * Adds a special instruction.
   *
   * @param clientId client
   * @param content type, text and effectivity
   * @return the instruction
   */
  public ClientInstruction addInstruction(Long clientId, InstructionContent content) {
    Client client = clients.requireUsable(clientId);
    validate(content);
    ClientInstruction saved = instructions.save(new ClientInstruction(clientId, content));
    log(
        client,
        new NoteChange(
            INSTRUCTION, String.valueOf(saved.getId()), "ADDED", null, content.describe()));
    return saved;
  }

  /**
   * Changes a special instruction.
   *
   * @param clientId client
   * @param instructionId instruction
   * @param content new content
   * @return the instruction
   */
  public ClientInstruction changeInstruction(
      Long clientId, Long instructionId, InstructionContent content) {
    Client client = clients.requireUsable(clientId);
    validate(content);
    ClientInstruction instruction = instruction(clientId, instructionId);
    String before = instruction.content().describe();
    instruction.change(content);
    log(
        client,
        new NoteChange(
            INSTRUCTION, String.valueOf(instructionId), "CHANGED", before, content.describe()));
    return instruction;
  }

  /**
   * Ends a special instruction (history is kept).
   *
   * @param clientId client
   * @param instructionId instruction
   * @return the instruction
   */
  public ClientInstruction endInstruction(Long clientId, Long instructionId) {
    Client client = clients.requireUsable(clientId);
    ClientInstruction instruction = instruction(clientId, instructionId);
    instruction.end();
    log(
        client,
        new NoteChange(
            INSTRUCTION,
            String.valueOf(instructionId),
            "REMOVED",
            instruction.content().describe(),
            null));
    return instruction;
  }

  private ClientInstruction instruction(Long clientId, Long instructionId) {
    return instructions
        .findById(instructionId)
        .filter(i -> i.getClientId().equals(clientId))
        .orElseThrow(() -> new ResourceNotFoundException("Client instruction", instructionId));
  }

  private void validate(InstructionContent content) {
    lovs.requireValid(INSTRUCTION_LIST, content.type(), LocalDate.now(clock));
    if (content.text() == null || content.text().isBlank()) {
      throw new BusinessRuleException("INSTRUCTION_TEXT_REQUIRED", "Enter the instruction");
    }
  }

  private ClientBanner.Instruction toBanner(ClientInstruction i) {
    return new ClientBanner.Instruction(
        i.getId(),
        i.getInstructionType(),
        lovs.label(INSTRUCTION_LIST, i.getInstructionType()),
        i.getText(),
        i.getEffectiveFrom(),
        i.getEffectiveTo());
  }

  private void log(Client client, NoteChange change) {
    history.save(new NoteHistory(client.getId(), change, currentUser.username(), clock.instant()));
    String summary =
        (TAG.equals(change.kind()) ? "Tag " : "Instruction ")
            + switch (change.action()) {
              case "ADDED" -> "added";
              case "CHANGED" -> "changed";
              default -> "removed";
            }
            + ": "
            + (change.to() != null ? change.to() : change.from());
    audit.record(ClientService.ENTITY, client.getProspectCode(), AuditAction.UPDATE, summary);
  }
}
