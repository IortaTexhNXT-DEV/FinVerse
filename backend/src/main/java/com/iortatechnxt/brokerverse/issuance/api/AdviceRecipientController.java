package com.iortatechnxt.brokerverse.issuance.api;

import com.iortatechnxt.brokerverse.issuance.domain.AdviceRecipient;
import com.iortatechnxt.brokerverse.issuance.service.AdviceRecipientService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The Insurance Advice recipient set-up (FR-NB-107): recipients and automatic sending per mortgagee
 * bank, maker-checker. Maintained by the Business Administrator, authorized by a second user.
 */
@RestController
@RequestMapping("/api/v1/issuance/advice-recipients")
public class AdviceRecipientController {

  private static final String VIEW =
      "hasAnyAuthority('MASTER_VIEW', 'LOV_MANAGE', 'MASTER_AUTHORIZE', 'EPOLICY_MANAGE',"
          + " 'EPOLICY_SEND')";
  private static final String MAINTAIN = "hasAuthority('LOV_MANAGE')";
  private static final String AUTHORIZE = "hasAnyAuthority('LOV_MANAGE', 'MASTER_AUTHORIZE')";

  private final AdviceRecipientService recipients;

  /**
   * Creates the controller.
   *
   * @param recipients recipient set-up
   */
  public AdviceRecipientController(AdviceRecipientService recipients) {
    this.recipients = recipients;
  }

  /**
   * The set-ups of a company.
   *
   * @param companyId company
   * @return set-ups
   */
  @GetMapping
  @PreAuthorize(VIEW)
  public List<RecipientView> list(@RequestParam Long companyId) {
    return recipients.list(companyId).stream().map(RecipientView::of).toList();
  }

  /**
   * Adds a set-up.
   *
   * @param companyId company
   * @param data values
   * @return set-up
   */
  @PostMapping
  @PreAuthorize(MAINTAIN)
  public RecipientView create(
      @RequestParam Long companyId, @RequestBody AdviceRecipient.Data data) {
    return RecipientView.of(recipients.create(companyId, data));
  }

  /**
   * Changes a set-up.
   *
   * @param companyId company
   * @param id set-up
   * @param data new values
   * @return set-up
   */
  @PutMapping("/{id}")
  @PreAuthorize(MAINTAIN)
  public RecipientView update(
      @RequestParam Long companyId, @PathVariable Long id, @RequestBody AdviceRecipient.Data data) {
    return RecipientView.of(recipients.update(companyId, id, data));
  }

  /**
   * Authorizes a set-up.
   *
   * @param companyId company
   * @param id set-up
   * @return set-up
   */
  @PostMapping("/{id}/authorize")
  @PreAuthorize(AUTHORIZE)
  public RecipientView authorize(@RequestParam Long companyId, @PathVariable Long id) {
    return RecipientView.of(recipients.authorize(companyId, id));
  }

  /**
   * Deactivates a set-up.
   *
   * @param companyId company
   * @param id set-up
   * @return set-up
   */
  @PostMapping("/{id}/deactivate")
  @PreAuthorize(MAINTAIN)
  public RecipientView deactivate(@RequestParam Long companyId, @PathVariable Long id) {
    return RecipientView.of(recipients.deactivate(companyId, id));
  }

  /**
   * A recipient set-up.
   *
   * @param id id
   * @param mortgageeBank mortgagee bank
   * @param marketSegment market segment, null for the whole bank
   * @param to recipients
   * @param cc copy
   * @param autoSend enrolled for automatic sending
   * @param effectiveFrom first day
   * @param effectiveTo last day
   * @param recordStatus maker-checker status
   * @param maker maker
   * @param authorizedBy checker
   * @param authorizedAt authorization time
   */
  public record RecipientView(
      Long id,
      String mortgageeBank,
      String marketSegment,
      List<String> to,
      List<String> cc,
      boolean autoSend,
      LocalDate effectiveFrom,
      LocalDate effectiveTo,
      String recordStatus,
      String maker,
      String authorizedBy,
      Instant authorizedAt) {

    /**
     * Maps a set-up.
     *
     * @param r set-up
     * @return view
     */
    public static RecipientView of(AdviceRecipient r) {
      return new RecipientView(
          r.getId(),
          r.getMortgageeBank(),
          r.getMarketSegment(),
          r.getTo(),
          r.getCc(),
          r.isAutoSend(),
          r.getEffectiveFrom(),
          r.getEffectiveTo(),
          r.getRecordStatus().name(),
          r.getMaker(),
          r.getAuthorizedBy(),
          r.getAuthorizedAt());
    }
  }
}
