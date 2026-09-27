package com.iortatechnxt.brokerverse.eb.member.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.eb.document.api.EbUploads;
import com.iortatechnxt.brokerverse.eb.domain.EbMemberChange;
import com.iortatechnxt.brokerverse.eb.member.api.dto.MemberDtos.MemberChangeRequest;
import com.iortatechnxt.brokerverse.eb.member.api.dto.MemberDtos.MemberChangeResponse;
import com.iortatechnxt.brokerverse.eb.member.api.dto.MemberDtos.MemberResponse;
import com.iortatechnxt.brokerverse.eb.member.api.dto.MemberDtos.ReasonRequest;
import com.iortatechnxt.brokerverse.eb.member.api.dto.MemberDtos.RosterResponse;
import com.iortatechnxt.brokerverse.eb.member.service.MemberChangeInput;
import com.iortatechnxt.brokerverse.eb.member.service.MemberChangeQuery;
import com.iortatechnxt.brokerverse.eb.member.service.MemberChangeService;
import com.iortatechnxt.brokerverse.eb.member.service.RosterService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** The member roster and the member changes (FR-EB-054 to 056). */
@RestController
@RequestMapping("/api/v1/eb")
@Transactional
public class MemberController {

  private static final String VIEW = "hasAuthority('EB_VIEW')";
  private static final String MARKET = "hasAuthority('EB_MARKET')";
  private static final String PROCESS = "hasAuthority('EB_PROCESS')";
  private static final String EITHER = "hasAnyAuthority('EB_MARKET', 'EB_PROCESS')";
  private static final int MAX_PAGE = 200;

  private final RosterService roster;
  private final MemberChangeService changes;
  private final MemberChangeQuery query;
  private final ObjectMapper json;

  /**
   * Creates the controller.
   *
   * @param roster roster versions
   * @param changes member changes
   * @param query work list
   * @param json JSON field of the member change form
   */
  public MemberController(
      RosterService roster,
      MemberChangeService changes,
      MemberChangeQuery query,
      ObjectMapper json) {
    this.roster = roster;
    this.changes = changes;
    this.query = query;
    this.json = json;
  }

  /**
   * The roster versions of a programme.
   *
   * @param id programme
   * @param companyId company
   * @return versions, latest first
   */
  @GetMapping("/programmes/{id}/roster")
  @PreAuthorize(VIEW)
  public List<RosterResponse> versions(@PathVariable Long id, @RequestParam Long companyId) {
    return roster.versions(companyId, id).stream().map(RosterResponse::from).toList();
  }

  /**
   * Members of a roster version.
   *
   * @param versionId version
   * @param companyId company
   * @param q employee number or name fragment
   * @param page page
   * @param size size
   * @return members
   */
  @GetMapping("/roster/{versionId}/members")
  @PreAuthorize(VIEW)
  public PageResponse<MemberResponse> members(
      @PathVariable Long versionId,
      @RequestParam Long companyId,
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    return PageResponse.of(
        roster.members(companyId, versionId, q, PageRequest.of(page, Math.min(size, MAX_PAGE))),
        MemberResponse::from);
  }

  /**
   * The differences of a version with the accepted roster.
   *
   * @param versionId version
   * @param companyId company
   * @return counts
   */
  @GetMapping("/roster/{versionId}/differences")
  @PreAuthorize(VIEW)
  public RosterService.Differences differences(
      @PathVariable Long versionId, @RequestParam Long companyId) {
    return roster.differences(companyId, versionId);
  }

  /**
   * Accepts a staged version.
   *
   * @param versionId version
   * @param companyId company
   * @return version
   */
  @PostMapping("/roster/{versionId}/accept")
  @PreAuthorize(MARKET)
  public RosterResponse accept(@PathVariable Long versionId, @RequestParam Long companyId) {
    return RosterResponse.from(roster.accept(companyId, versionId));
  }

  /**
   * Rejects a staged version.
   *
   * @param versionId version
   * @param companyId company
   * @param request reason
   * @return version
   */
  @PostMapping("/roster/{versionId}/reject")
  @PreAuthorize(MARKET)
  public RosterResponse reject(
      @PathVariable Long versionId,
      @RequestParam Long companyId,
      @RequestBody ReasonRequest request) {
    return RosterResponse.from(roster.reject(companyId, versionId, request.reason()));
  }

  /**
   * The Member Changes work list.
   *
   * @param companyId company
   * @param status OPEN, a status or empty
   * @param programmeId programme
   * @param q change number, programme number or client
   * @param page page
   * @param size size
   * @return changes
   */
  @GetMapping("/member-changes")
  @PreAuthorize(VIEW)
  @SuppressWarnings("java:S107") // one parameter per filter
  public PageResponse<MemberChangeResponse> list(
      @RequestParam Long companyId,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Long programmeId,
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "25") int size) {
    return PageResponse.of(
        query.search(
            companyId,
            new MemberChangeQuery.Filter(status, programmeId, q),
            PageRequest.of(page, Math.min(size, MAX_PAGE))),
        c -> MemberChangeResponse.from(c, query.programmeOf(c)));
  }

  /**
   * A member change.
   *
   * @param id change
   * @param companyId company
   * @return change
   */
  @GetMapping("/member-changes/{id}")
  @PreAuthorize(VIEW)
  public MemberChangeResponse get(@PathVariable Long id, @RequestParam Long companyId) {
    return map(changes.require(companyId, id));
  }

  /**
   * Captures a member change.
   *
   * @param id programme
   * @param companyId company
   * @param change the change as JSON
   * @param files the client's request
   * @return change
   */
  @PostMapping(
      value = "/programmes/{id}/member-changes",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(MARKET)
  public MemberChangeResponse capture(
      @PathVariable Long id,
      @RequestParam Long companyId,
      @RequestParam String change,
      @RequestParam(required = false) List<MultipartFile> files) {
    MemberChangeRequest body = EbUploads.read(json, change, MemberChangeRequest.class);
    return map(
        changes.capture(
            companyId,
            id,
            new MemberChangeInput(
                body.lineNo(),
                body.policyYear(),
                body.source(),
                body.financial(),
                body.description(),
                body.lines(),
                EbUploads.files(files))));
  }

  /**
   * Relays a change to the insurer.
   *
   * @param id change
   * @param companyId company
   * @return change
   */
  @PostMapping("/member-changes/{id}/relay")
  @PreAuthorize(MARKET)
  public MemberChangeResponse relay(@PathVariable Long id, @RequestParam Long companyId) {
    return map(changes.relay(companyId, id));
  }

  /**
   * Records the insurer's billing.
   *
   * @param id change
   * @param companyId company
   * @param billedOn billing date
   * @param reference billing reference
   * @param amount amount billed
   * @param direct direct billing
   * @param files direct billing files
   * @return change
   */
  @PostMapping(value = "/member-changes/{id}/bill", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize(EITHER)
  @SuppressWarnings("java:S107") // one parameter per form field
  public MemberChangeResponse bill(
      @PathVariable Long id,
      @RequestParam Long companyId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate billedOn,
      @RequestParam(required = false) String reference,
      @RequestParam(required = false) BigDecimal amount,
      @RequestParam(defaultValue = "false") boolean direct,
      @RequestParam(required = false) List<MultipartFile> files) {
    return map(
        changes.bill(
            companyId,
            id,
            new EbMemberChange.Billing(billedOn, reference, amount, direct),
            EbUploads.files(files)));
  }

  /**
   * Validates a billed change (Processing).
   *
   * @param id change
   * @param companyId company
   * @return change
   */
  @PostMapping("/member-changes/{id}/validate")
  @PreAuthorize(PROCESS)
  public MemberChangeResponse validate(@PathVariable Long id, @RequestParam Long companyId) {
    return map(changes.validate(companyId, id));
  }

  /**
   * Closes a validated change: its lines are applied to the roster.
   *
   * @param id change
   * @param companyId company
   * @return change
   */
  @PostMapping("/member-changes/{id}/close")
  @PreAuthorize(EITHER)
  public MemberChangeResponse close(@PathVariable Long id, @RequestParam Long companyId) {
    return map(changes.close(companyId, id));
  }

  private MemberChangeResponse map(EbMemberChange c) {
    return MemberChangeResponse.from(c, query.programmeOf(c));
  }
}
