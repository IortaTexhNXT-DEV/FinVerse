package com.iortatechnxt.brokerverse.eb.programme.api;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.eb.home.service.EbHomeService;
import com.iortatechnxt.brokerverse.eb.programme.api.dto.ProgrammeRequests.ContactRequest;
import com.iortatechnxt.brokerverse.eb.programme.api.dto.ProgrammeRequests.LineRequest;
import com.iortatechnxt.brokerverse.eb.programme.api.dto.ProgrammeRequests.ProfileRequest;
import com.iortatechnxt.brokerverse.eb.programme.api.dto.ProgrammeRequests.ProgrammeFilter;
import com.iortatechnxt.brokerverse.eb.programme.api.dto.ProgrammeRequests.ProgrammeRequest;
import com.iortatechnxt.brokerverse.eb.programme.api.dto.ProgrammeRequests.SendRaRequest;
import com.iortatechnxt.brokerverse.eb.programme.service.ProgrammeQuery;
import com.iortatechnxt.brokerverse.eb.programme.service.ProgrammeRow;
import com.iortatechnxt.brokerverse.eb.programme.service.ProgrammeService;
import com.iortatechnxt.brokerverse.eb.programme.service.ProgrammeView;
import com.iortatechnxt.brokerverse.eb.programme.service.ProgrammeViewService;
import com.iortatechnxt.brokerverse.eb.programme.service.ProgrammeViewService.ActivityRow;
import com.iortatechnxt.brokerverse.eb.renewal.service.RenewalAdviceBatch;
import com.iortatechnxt.brokerverse.security.service.UserDirectory;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
 * EB programmes (BRID-001, 006, 022.01; FR-EB-021, FR-EB-022; design 10.1): the Programmes work
 * list with Send RA, the New Programme screen, the programme page (profile, lines, contacts,
 * cycles, activity) and the EB Home counts.
 */
@RestController
@RequestMapping("/api/v1/eb")
public class ProgrammeController {

  private static final String VIEW = "hasAuthority('EB_VIEW')";
  private static final String MARKET = "hasAuthority('EB_MARKET')";

  private final ProgrammeService programmes;
  private final ProgrammeViewService views;
  private final ProgrammeQuery query;
  private final RenewalAdviceBatch sendRa;
  private final EbHomeService home;
  private final UserDirectory users;

  /**
   * Creates the controller.
   *
   * @param programmes programme maintenance
   * @param views programme page
   * @param query work list
   * @param sendRa Send RA
   * @param home EB Home counts
   * @param users EB account officers
   */
  public ProgrammeController(
      ProgrammeService programmes,
      ProgrammeViewService views,
      ProgrammeQuery query,
      RenewalAdviceBatch sendRa,
      EbHomeService home,
      UserDirectory users) {
    this.programmes = programmes;
    this.views = views;
    this.query = query;
    this.sendRa = sendRa;
    this.home = home;
    this.users = users;
  }

  /**
   * The EB account officers (users holding EB_MARKET) for the AO pick lists.
   *
   * @return user names, sorted
   */
  @GetMapping("/account-officers")
  @PreAuthorize(VIEW)
  public List<String> accountOfficers() {
    return users.usersWithPermission("EB_MARKET");
  }

  /**
   * The EB Home tile counts.
   *
   * @param companyId company
   * @return counts by tile id
   */
  @GetMapping("/home")
  @PreAuthorize(VIEW)
  public Map<String, Long> home(@RequestParam Long companyId) {
    return home.counts(companyId);
  }

  /**
   * The Programmes work list.
   *
   * @param companyId company
   * @param filter tab (RENEWAL_DUE, IN_PROGRESS, WITH_CLIENT, IN_PLACEMENT, PLACED, LOST, ALL),
   *     stage, AO, team, search text, page and size
   * @return programmes
   */
  @GetMapping("/programmes")
  @PreAuthorize(VIEW)
  public PageResponse<ProgrammeRow> search(@RequestParam Long companyId, ProgrammeFilter filter) {
    return query.search(companyId, filter.criteria(), filter.pageNo(), filter.pageSize());
  }

  /**
   * Creates a programme.
   *
   * @param companyId company
   * @param request client, profile, lines and contacts
   * @return the programme page
   */
  @PostMapping("/programmes")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(MARKET)
  public ProgrammeView create(@RequestParam Long companyId, @RequestBody ProgrammeRequest request) {
    Long id = programmes.create(companyId, request.toInput()).getId();
    return views.view(companyId, id);
  }

  /**
   * A programme page.
   *
   * @param id programme
   * @param companyId company
   * @return the view
   */
  @GetMapping("/programmes/{id}")
  @PreAuthorize(VIEW)
  public ProgrammeView get(@PathVariable Long id, @RequestParam Long companyId) {
    return views.view(companyId, id);
  }

  /**
   * The activity log of a programme (History tab).
   *
   * @param id programme
   * @param companyId company
   * @return stamps, newest first
   */
  @GetMapping("/programmes/{id}/activity")
  @PreAuthorize(VIEW)
  public List<ActivityRow> activity(@PathVariable Long id, @RequestParam Long companyId) {
    return views.activity(companyId, id);
  }

  /**
   * Changes the profile of a programme.
   *
   * @param id programme
   * @param companyId company
   * @param request profile
   * @return the programme page
   */
  @PutMapping("/programmes/{id}")
  @PreAuthorize(MARKET)
  public ProgrammeView update(
      @PathVariable Long id, @RequestParam Long companyId, @RequestBody ProfileRequest request) {
    programmes.update(companyId, id, request.toProfile());
    return views.view(companyId, id);
  }

  /**
   * Adds a benefit line.
   *
   * @param id programme
   * @param companyId company
   * @param request line
   * @return the programme page
   */
  @PostMapping("/programmes/{id}/lines")
  @PreAuthorize(MARKET)
  public ProgrammeView addLine(
      @PathVariable Long id, @RequestParam Long companyId, @RequestBody LineRequest request) {
    programmes.addLine(companyId, id, request.toData());
    return views.view(companyId, id);
  }

  /**
   * Changes a benefit line.
   *
   * @param id programme
   * @param lineNo line number
   * @param companyId company
   * @param request line
   * @return the programme page
   */
  @PutMapping("/programmes/{id}/lines/{lineNo}")
  @PreAuthorize(MARKET)
  public ProgrammeView updateLine(
      @PathVariable Long id,
      @PathVariable int lineNo,
      @RequestParam Long companyId,
      @RequestBody LineRequest request) {
    programmes.updateLine(companyId, id, lineNo, request.toData());
    return views.view(companyId, id);
  }

  /**
   * Removes a benefit line (kept for history).
   *
   * @param id programme
   * @param lineNo line number
   * @param companyId company
   * @return the programme page
   */
  @DeleteMapping("/programmes/{id}/lines/{lineNo}")
  @PreAuthorize(MARKET)
  public ProgrammeView removeLine(
      @PathVariable Long id, @PathVariable int lineNo, @RequestParam Long companyId) {
    programmes.deactivateLine(companyId, id, lineNo);
    return views.view(companyId, id);
  }

  /**
   * Adds an HR contact.
   *
   * @param id programme
   * @param companyId company
   * @param request contact
   * @return the programme page
   */
  @PostMapping("/programmes/{id}/contacts")
  @PreAuthorize(MARKET)
  public ProgrammeView addContact(
      @PathVariable Long id, @RequestParam Long companyId, @RequestBody ContactRequest request) {
    programmes.addContact(companyId, id, request.toData());
    return views.view(companyId, id);
  }

  /**
   * Changes an HR contact.
   *
   * @param id programme
   * @param contactId contact
   * @param companyId company
   * @param request contact
   * @return the programme page
   */
  @PutMapping("/programmes/{id}/contacts/{contactId}")
  @PreAuthorize(MARKET)
  public ProgrammeView updateContact(
      @PathVariable Long id,
      @PathVariable Long contactId,
      @RequestParam Long companyId,
      @RequestBody ContactRequest request) {
    programmes.updateContact(companyId, id, contactId, request.toData());
    return views.view(companyId, id);
  }

  /**
   * Removes an HR contact (kept for history).
   *
   * @param id programme
   * @param contactId contact
   * @param companyId company
   * @return the programme page
   */
  @DeleteMapping("/programmes/{id}/contacts/{contactId}")
  @PreAuthorize(MARKET)
  public ProgrammeView removeContact(
      @PathVariable Long id, @PathVariable Long contactId, @RequestParam Long companyId) {
    programmes.deactivateContact(companyId, id, contactId);
    return views.view(companyId, id);
  }

  /**
   * Sends the renewal advice of the selected programmes (Send RA).
   *
   * @param companyId company
   * @param request programmes
   * @return one result per programme
   */
  @PostMapping("/programmes/send-ra")
  @PreAuthorize(MARKET)
  public List<RenewalAdviceBatch.Result> sendRa(
      @RequestParam Long companyId, @RequestBody SendRaRequest request) {
    return sendRa.send(companyId, request.programmeIds());
  }
}
