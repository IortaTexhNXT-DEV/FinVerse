package com.iortatechnxt.brokerverse.renewal.proposal.api;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalProposal;
import com.iortatechnxt.brokerverse.renewal.proposal.service.RenewalProposals;
import com.iortatechnxt.brokerverse.renewal.proposal.service.TsuRequests;
import com.iortatechnxt.brokerverse.renewal.service.RenewalRecords;
import com.iortatechnxt.brokerverse.security.service.UserDirectory;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Proposals and TSU requests of the renewal accounts (FRRN.017, FRRN.018): Quick and Full Proposals
 * with their signatories, their sending through CCM, and the quotation requests to TSU.
 */
@RestController
@RequestMapping("/api/v1/renewal")
public class RenewalProposalController {

  private static final String VIEW = "hasAuthority('RNW_VIEW')";
  private static final String MARKETING = "hasAnyAuthority('RNW_DISPOSE','RNW_REVIEW')";
  private static final String TSU = "hasAnyAuthority('TSU_PROCESS','TSU_APPROVE')";

  private static final Set<String> PICKABLE = Set.of("RNW_REVIEW", "TSU_PROCESS");

  private final RenewalProposals proposals;
  private final TsuRequests tsu;
  private final RenewalRecords records;
  private final UserDirectory users;

  /**
   * Creates the controller.
   *
   * @param proposals proposals
   * @param tsu TSU requests
   * @param records renewals
   * @param users signatories and TSU Officers
   */
  public RenewalProposalController(
      RenewalProposals proposals, TsuRequests tsu, RenewalRecords records, UserDirectory users) {
    this.proposals = proposals;
    this.tsu = tsu;
    this.records = records;
    this.users = users;
  }

  /**
   * The users who can sign a proposal (RNW_REVIEW) or process a TSU request (TSU_PROCESS).
   *
   * @param permission RNW_REVIEW or TSU_PROCESS
   * @return username and name
   */
  @GetMapping("/proposal-users")
  @PreAuthorize(VIEW)
  public List<UserOption> pickable(@RequestParam String permission) {
    if (!PICKABLE.contains(permission)) {
      throw new BusinessRuleException("RNW_USERS", "Unknown list of users");
    }
    return users.usersWithPermission(permission).stream()
        .map(u -> new UserOption(u, users.displayName(u)))
        .toList();
  }

  /**
   * A user.
   *
   * @param username username
   * @param displayName name
   */
  public record UserOption(String username, String displayName) {}

  /**
   * The proposals of an account and the signatories a Full Proposal needs.
   *
   * @param companyId company
   * @param ref renewal
   * @return proposals
   */
  @GetMapping("/candidates/{ref}/proposals")
  @PreAuthorize(VIEW)
  @Transactional(readOnly = true)
  public Proposals list(@RequestParam Long companyId, @PathVariable String ref) {
    return new Proposals(
        proposals.requiredSignatories(records.get(companyId, ref)),
        proposals.of(companyId, ref).stream().map(View::of).toList());
  }

  /**
   * Generates a Quick or Full Proposal.
   *
   * @param companyId company
   * @param ref renewal
   * @param body kind and signatories
   * @return the proposal
   */
  @PostMapping("/candidates/{ref}/proposals")
  @PreAuthorize(MARKETING)
  public View generate(
      @RequestParam Long companyId, @PathVariable String ref, @RequestBody Generate body) {
    return View.of(proposals.generate(companyId, ref, body.kind(), body.signatories()));
  }

  /**
   * Sends a proposal to the client through CCM.
   *
   * @param companyId company
   * @param ref renewal
   * @param proposalNo proposal
   * @param body recipients
   * @return the success message
   */
  @PostMapping("/candidates/{ref}/proposals/{proposalNo}/send")
  @PreAuthorize(MARKETING)
  public Map<String, String> send(
      @RequestParam Long companyId,
      @PathVariable String ref,
      @PathVariable String proposalNo,
      @RequestBody Send body) {
    return Map.of("message", proposals.send(companyId, ref, proposalNo, body.to(), body.cc()));
  }

  /**
   * The TSU requests of an account.
   *
   * @param companyId company
   * @param ref renewal
   * @return requests
   */
  @GetMapping("/candidates/{ref}/tsu-requests")
  @PreAuthorize(VIEW)
  public List<TsuRequests.View> tsuRequests(
      @RequestParam Long companyId, @PathVariable String ref) {
    return tsu.of(companyId, ref);
  }

  /**
   * Creates and submits the TSU request of an account For Quotation.
   *
   * @param companyId company
   * @param ref renewal
   * @param body remarks
   * @return the request
   */
  @PostMapping("/candidates/{ref}/tsu-requests")
  @PreAuthorize("hasAuthority('RNW_DISPOSE')")
  public TsuRequests.View createTsuRequest(
      @RequestParam Long companyId, @PathVariable String ref, @RequestBody Remarks body) {
    String no = tsu.create(companyId, ref, body.remarks()).getRequestNo();
    tsu.submit(companyId, no);
    return tsu.of(companyId, ref).get(0);
  }

  /**
   * Resubmits a request returned for revision.
   *
   * @param companyId company
   * @param requestNo request
   */
  @PostMapping("/tsu-requests/{requestNo}/submit")
  @PreAuthorize("hasAuthority('RNW_DISPOSE')")
  public void submit(@RequestParam Long companyId, @PathVariable String requestNo) {
    tsu.submit(companyId, requestNo);
  }

  /**
   * The Team Lead's decision.
   *
   * @param companyId company
   * @param requestNo request
   * @param body decision and remarks
   */
  @PostMapping("/tsu-requests/{requestNo}/decision")
  @PreAuthorize("hasAuthority('RNW_REVIEW')")
  public void decide(
      @RequestParam Long companyId, @PathVariable String requestNo, @RequestBody Decision body) {
    tsu.decide(companyId, requestNo, body.decision(), body.remarks());
  }

  /**
   * Assigns the TSU Officer.
   *
   * @param companyId company
   * @param requestNo request
   * @param body officer
   */
  @PostMapping("/tsu-requests/{requestNo}/assign")
  @PreAuthorize("hasAnyAuthority('RNW_REVIEW','RNW_DISPOSE')")
  public void assign(
      @RequestParam Long companyId, @PathVariable String requestNo, @RequestBody Officer body) {
    tsu.assign(companyId, requestNo, body.officer());
  }

  /**
   * Records an insurer quotation of the comparative table.
   *
   * @param companyId company
   * @param requestNo request
   * @param body quotation
   */
  @PostMapping("/tsu-requests/{requestNo}/quotes")
  @PreAuthorize(TSU)
  public void quote(
      @RequestParam Long companyId,
      @PathVariable String requestNo,
      @RequestBody TsuRequests.Quote body) {
    tsu.quote(companyId, requestNo, body);
  }

  /**
   * Marketing's choice of insurers.
   *
   * @param companyId company
   * @param requestNo request
   * @param body insurers
   */
  @PostMapping("/tsu-requests/{requestNo}/selection")
  @PreAuthorize("hasAuthority('RNW_DISPOSE')")
  public void select(
      @RequestParam Long companyId, @PathVariable String requestNo, @RequestBody Selection body) {
    tsu.select(companyId, requestNo, body.insurers());
  }

  /**
   * Completes a request with the proposal of TSU.
   *
   * @param companyId company
   * @param requestNo request
   * @param file proposal
   */
  @PostMapping("/tsu-requests/{requestNo}/complete")
  @PreAuthorize(TSU)
  public void complete(
      @RequestParam Long companyId,
      @PathVariable String requestNo,
      @RequestPart("file") MultipartFile file) {
    if (file.getOriginalFilename() == null) {
      throw new BusinessRuleException("RNW_TSU_FILE", "The file has no name");
    }
    try {
      tsu.complete(companyId, requestNo, file.getOriginalFilename(), file.getBytes());
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  /**
   * The requests of a status (TSU work queue, Team Lead approvals).
   *
   * @param companyId company
   * @param status status
   * @return requests
   */
  @GetMapping("/tsu-requests")
  @PreAuthorize("hasAnyAuthority('RNW_REVIEW','TSU_PROCESS','TSU_APPROVE')")
  public List<TsuRequests.View> inStatus(
      @RequestParam Long companyId, @RequestParam String status) {
    return tsu.inStatus(companyId, status);
  }

  /**
   * Proposals of an account.
   *
   * @param requiredSignatories signatories of a Full Proposal
   * @param proposals proposals
   */
  public record Proposals(int requiredSignatories, List<View> proposals) {}

  /**
   * A proposal.
   *
   * @param proposalNo number
   * @param kind QUICK, FULL or TSU
   * @param fileName file name
   * @param attachmentId stored file
   * @param signatories signatories
   * @param status GENERATED or SENT
   * @param messageNo CCM message
   * @param generatedBy user
   * @param generatedAt time
   */
  public record View(
      String proposalNo,
      String kind,
      String fileName,
      Long attachmentId,
      String signatories,
      String status,
      String messageNo,
      String generatedBy,
      Instant generatedAt) {

    static View of(RenewalProposal p) {
      return new View(
          p.getProposalNo(),
          p.getKind(),
          p.getFileName(),
          p.getAttachmentId(),
          p.getSignatories(),
          p.getStatus(),
          p.getMessageNo(),
          p.getCreatedBy(),
          p.getCreatedAt());
    }
  }

  /**
   * A generation.
   *
   * @param kind QUICK or FULL
   * @param signatories usernames
   */
  public record Generate(String kind, List<String> signatories) {}

  /**
   * A sending.
   *
   * @param to recipients
   * @param cc copy recipients
   */
  public record Send(List<String> to, List<String> cc) {}

  /**
   * Remarks.
   *
   * @param remarks text
   */
  public record Remarks(String remarks) {}

  /**
   * A Team Lead decision.
   *
   * @param decision APPROVED, REJECTED or RETURNED_FOR_REVISION
   * @param remarks remarks
   */
  public record Decision(String decision, String remarks) {}

  /**
   * The TSU Officer.
   *
   * @param officer username
   */
  public record Officer(String officer) {}

  /**
   * Insurers selected.
   *
   * @param insurers insurer codes
   */
  public record Selection(List<String> insurers) {}
}
