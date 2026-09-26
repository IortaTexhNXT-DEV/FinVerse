package com.iortatechnxt.brokerverse.crm.api;

import com.iortatechnxt.brokerverse.crm.api.dto.ClientNotesDtos.BannerResponse;
import com.iortatechnxt.brokerverse.crm.api.dto.ClientNotesDtos.HistoryResponse;
import com.iortatechnxt.brokerverse.crm.api.dto.ClientNotesDtos.InstructionRequest;
import com.iortatechnxt.brokerverse.crm.api.dto.ClientNotesDtos.InstructionResponse;
import com.iortatechnxt.brokerverse.crm.api.dto.ClientNotesDtos.NotesResponse;
import com.iortatechnxt.brokerverse.crm.api.dto.ClientNotesDtos.TagRequest;
import com.iortatechnxt.brokerverse.crm.service.ClientNotesService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Client tags and special instructions (BRNB.091): the banner shown on quotation and account
 * screens, maintenance and change history.
 */
@RestController
@RequestMapping("/api/v1/crm/clients/{id}")
public class ClientNotesController {

  private static final String VIEW = "hasAuthority('CLIENT_VIEW')";
  private static final String MAINTAIN = "hasAuthority('CLIENT_MAINTAIN')";

  private final ClientNotesService notes;

  /**
   * Creates the controller.
   *
   * @param notes tags and instructions
   */
  public ClientNotesController(ClientNotesService notes) {
    this.notes = notes;
  }

  /**
   * Active tags and instructions in force (banners).
   *
   * @param id client
   * @return banner
   */
  @GetMapping("/instructions")
  @PreAuthorize(VIEW)
  public BannerResponse banner(@PathVariable Long id) {
    return BannerResponse.from(notes.banner(id));
  }

  /**
   * Every instruction and the change history.
   *
   * @param id client
   * @return instructions and history
   */
  @GetMapping("/notes")
  @PreAuthorize(VIEW)
  public NotesResponse notes(@PathVariable Long id) {
    return new NotesResponse(
        notes.instructions(id).stream().map(InstructionResponse::from).toList(),
        notes.history(id).stream().map(HistoryResponse::from).toList());
  }

  /**
   * Tags the client.
   *
   * @param id client
   * @param request tag
   * @return banner
   */
  @PostMapping("/tags")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(MAINTAIN)
  public BannerResponse addTag(@PathVariable Long id, @Valid @RequestBody TagRequest request) {
    notes.addTag(id, request.tagCode());
    return BannerResponse.from(notes.banner(id));
  }

  /**
   * Removes a tag.
   *
   * @param id client
   * @param code tag
   * @return banner
   */
  @PostMapping("/tags/{code}/remove")
  @PreAuthorize(MAINTAIN)
  public BannerResponse removeTag(@PathVariable Long id, @PathVariable String code) {
    notes.removeTag(id, code);
    return BannerResponse.from(notes.banner(id));
  }

  /**
   * Adds a special instruction.
   *
   * @param id client
   * @param request instruction
   * @return instruction
   */
  @PostMapping("/instructions")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(MAINTAIN)
  public InstructionResponse addInstruction(
      @PathVariable Long id, @Valid @RequestBody InstructionRequest request) {
    return InstructionResponse.from(notes.addInstruction(id, request.content()));
  }

  /**
   * Changes a special instruction.
   *
   * @param id client
   * @param instructionId instruction
   * @param request new content
   * @return instruction
   */
  @PutMapping("/instructions/{instructionId}")
  @PreAuthorize(MAINTAIN)
  public InstructionResponse changeInstruction(
      @PathVariable Long id,
      @PathVariable Long instructionId,
      @Valid @RequestBody InstructionRequest request) {
    return InstructionResponse.from(notes.changeInstruction(id, instructionId, request.content()));
  }

  /**
   * Ends a special instruction.
   *
   * @param id client
   * @param instructionId instruction
   * @return instruction
   */
  @PostMapping("/instructions/{instructionId}/end")
  @PreAuthorize(MAINTAIN)
  public InstructionResponse endInstruction(
      @PathVariable Long id, @PathVariable Long instructionId) {
    return InstructionResponse.from(notes.endInstruction(id, instructionId));
  }
}
