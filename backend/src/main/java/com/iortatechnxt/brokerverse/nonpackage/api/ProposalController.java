package com.iortatechnxt.brokerverse.nonpackage.api;

import com.iortatechnxt.brokerverse.account.api.dto.CommentRequest;
import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.nonpackage.api.dto.ProposalBody;
import com.iortatechnxt.brokerverse.nonpackage.api.dto.ProposalDtos.AcceptBody;
import com.iortatechnxt.brokerverse.nonpackage.api.dto.ProposalDtos.ChecklistView;
import com.iortatechnxt.brokerverse.nonpackage.api.dto.ProposalDtos.ListItem;
import com.iortatechnxt.brokerverse.nonpackage.api.dto.ProposalDtos.SendBody;
import com.iortatechnxt.brokerverse.nonpackage.api.dto.ProposalResponse;
import com.iortatechnxt.brokerverse.nonpackage.domain.ProposalRequest;
import com.iortatechnxt.brokerverse.nonpackage.domain.ProposalStatus;
import com.iortatechnxt.brokerverse.nonpackage.service.ProposalAcceptanceService;
import com.iortatechnxt.brokerverse.nonpackage.service.ProposalQueryService;
import com.iortatechnxt.brokerverse.nonpackage.service.ProposalService;
import com.iortatechnxt.brokerverse.nonpackage.service.ProposalSlipService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Proposal Request Forms, Marketing side (BRNB.005/006/014/017/045): list, detail, document
 * checklist, create and update, submit, approve, send the released proposal to the client, record
 * the acceptance and create the accounts. TSU actions are in {@link ProposalTsuController}; the
 * generic actions (TSU accept, return, decline, void) run from the workflow panel.
 */
@RestController
@RequestMapping("/api/v1/proposals")
public class ProposalController {

  /** Anyone working on PRFs (Marketing, approvers, TSU). */
  static final String VIEW =
      "hasAnyAuthority('PROPOSAL_REQUEST', 'PROPOSAL_APPROVE', 'TSU_PROCESS', 'TSU_APPROVE')";

  private static final String REQUEST = "hasAuthority('PROPOSAL_REQUEST')";
  private static final int MAX_PAGE = 200;

  private final ProposalService proposals;
  private final ProposalQueryService queries;
  private final ProposalSlipService slips;
  private final ProposalAcceptanceService acceptance;
  private final CurrentUser currentUser;

  /**
   * Creates the controller.
   *
   * @param proposals PRF commands
   * @param queries PRF reads
   * @param slips proposal slip dispatch
   * @param acceptance acceptance and accounts
   * @param currentUser current user
   */
  public ProposalController(
      ProposalService proposals,
      ProposalQueryService queries,
      ProposalSlipService slips,
      ProposalAcceptanceService acceptance,
      CurrentUser currentUser) {
    this.proposals = proposals;
    this.queries = queries;
    this.slips = slips;
    this.acceptance = acceptance;
    this.currentUser = currentUser;
  }

  /**
   * Searches PRFs, newest first.
   *
   * @param companyId company
   * @param text PRF, ARN, slip number, client code or name
   * @param status statuses
   * @param mine only the current user's PRFs
   * @param clientId client
   * @param page page
   * @param size size
   * @return page of PRFs
   */
  @GetMapping
  @PreAuthorize(VIEW)
  public PageResponse<ListItem> search(
      @RequestParam Long companyId,
      @RequestParam(required = false) String text,
      @RequestParam(required = false) List<ProposalStatus> status,
      @RequestParam(required = false) Boolean mine,
      @RequestParam(required = false) Long clientId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    PageRequest pageable =
        PageRequest.of(
            Math.max(page, 0),
            Math.min(Math.max(size, 1), MAX_PAGE),
            Sort.by(Sort.Direction.DESC, "createdAt", "id"));
    ProposalQueryService.Search search =
        new ProposalQueryService.Search(
            companyId,
            text,
            status,
            Boolean.TRUE.equals(mine) ? currentUser.username() : null,
            clientId);
    return PageResponse.of(queries.search(search, pageable), ListItem::from);
  }

  /**
   * One PRF.
   *
   * @param id PRF
   * @return PRF with its details
   */
  @GetMapping("/{id}")
  @PreAuthorize(VIEW)
  public ProposalResponse get(@PathVariable Long id) {
    return reload(id);
  }

  /**
   * The mandatory-document checklist of the product (BRNB.005).
   *
   * @param id PRF
   * @return required documents and whether they are attached
   */
  @GetMapping("/{id}/checklist")
  @PreAuthorize(VIEW)
  public List<ChecklistView> checklist(@PathVariable Long id) {
    return proposals.checklist(id).stream().map(ChecklistView::from).toList();
  }

  /**
   * Creates a PRF with its marketing reference and ARN.
   *
   * @param body PRF data
   * @return the PRF
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(REQUEST)
  public ProposalResponse create(@Valid @RequestBody ProposalBody body) {
    return reload(proposals.create(body.companyId(), body.draft()));
  }

  /**
   * Changes a PRF (Marketing while in draft, TSU while in the TSU queue).
   *
   * @param id PRF
   * @param body PRF data
   * @return the PRF
   */
  @PutMapping("/{id}")
  @PreAuthorize("hasAnyAuthority('PROPOSAL_REQUEST', 'TSU_PROCESS')")
  public ProposalResponse update(@PathVariable Long id, @Valid @RequestBody ProposalBody body) {
    return reload(proposals.update(id, body.draft()));
  }

  /**
   * Submits the PRF for Marketing approval.
   *
   * @param id PRF
   * @param body comment
   * @return the PRF
   */
  @PostMapping("/{id}/submit")
  @PreAuthorize(REQUEST)
  public ProposalResponse submit(@PathVariable Long id, @Valid @RequestBody CommentRequest body) {
    return reload(proposals.submit(id, body.text()));
  }

  /**
   * Marketing approval: the PRF goes to the TSU queue.
   *
   * @param id PRF
   * @param body comment
   * @return the PRF
   */
  @PostMapping("/{id}/approve")
  @PreAuthorize("hasAuthority('PROPOSAL_APPROVE')")
  public ProposalResponse approve(@PathVariable Long id, @Valid @RequestBody CommentRequest body) {
    return reload(proposals.approve(id, body.text()));
  }

  /**
   * Sends the released proposal slip and comparative table to the client.
   *
   * @param id PRF
   * @param body e-mail
   * @return the PRF
   */
  @PostMapping("/{id}/send")
  @PreAuthorize(REQUEST)
  public ProposalResponse send(@PathVariable Long id, @Valid @RequestBody SendBody body) {
    return reload(slips.sendToClient(id, body.email()));
  }

  /**
   * Records the client's acceptance (the acceptance e-mail must be attached).
   *
   * @param id PRF
   * @param body accepted groups and comment
   * @return the PRF
   */
  @PostMapping("/{id}/accept")
  @PreAuthorize(REQUEST)
  public ProposalResponse accept(@PathVariable Long id, @Valid @RequestBody AcceptBody body) {
    return reload(acceptance.accept(id, body.groups(), body.comment()));
  }

  /**
   * Creates the accounts of the accepted risk groups with the chosen insurer.
   *
   * @param id PRF
   * @param body comment
   * @return the PRF with its account ARNs
   */
  @PostMapping("/{id}/create-accounts")
  @PreAuthorize("hasAuthority('PROPOSAL_REQUEST') and hasAuthority('ACCOUNT_MAINTAIN')")
  public ProposalResponse createAccounts(
      @PathVariable Long id, @Valid @RequestBody CommentRequest body) {
    return reload(acceptance.createAccounts(id, body.text()));
  }

  private ProposalResponse reload(ProposalRequest p) {
    return reload(p.getId());
  }

  private ProposalResponse reload(Long id) {
    ProposalRequest p = queries.get(id);
    return ProposalResponse.from(p, queries.details(p));
  }
}
