package com.iortatechnxt.brokerverse.renewal.processing.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountPremium;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSnapshot.SnapshotPremium;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalAssignment;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalAssignmentRepository;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalStage;
import com.iortatechnxt.brokerverse.renewal.marketing.service.RemarkService;
import com.iortatechnxt.brokerverse.renewal.service.BatchOutcome;
import com.iortatechnxt.brokerverse.renewal.service.RenewalBatch;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import com.iortatechnxt.brokerverse.renewal.service.RenewalFlow;
import com.iortatechnxt.brokerverse.renewal.service.RenewalNotices;
import com.iortatechnxt.brokerverse.renewal.service.RenewalRecords;
import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.domain.AppUserRepository;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import com.iortatechnxt.brokerverse.security.service.UserDirectory;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Processing (FR-RN-061, 064, 065): the Processing TL assigns renewals to Processing Officers (or
 * to himself), Processing returns renewals to the Marketing AO or TL with a reason and remarks, and
 * the Computations tab compares the renewal account with the expiring invoice.
 */
@Service
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class ProcessingService {

  private final RenewalRecords records;
  private final RenewalAssignmentRepository assignments;
  private final RenewalAccountService renewalAccounts;
  private final RemarkService remarks;
  private final RenewalFlow flow;
  private final RenewalNotices notices;
  private final RenewalBatch batch;
  private final AppUserRepository users;
  private final LovService lovs;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;
  private final UserDirectory directory;

  /**
   * Creates the service.
   *
   * @param records renewals
   * @param assignments assignment history
   * @param renewalAccounts renewal accounts
   * @param remarks remarks
   * @param flow workflow
   * @param notices notifications
   * @param batch batch runner
   * @param users users
   * @param lovs lists of values
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   * @param directory user directory (display names in texts)
   */
  @SuppressWarnings("java:S107") // constructor injection
  public ProcessingService(
      RenewalRecords records,
      RenewalAssignmentRepository assignments,
      RenewalAccountService renewalAccounts,
      RemarkService remarks,
      RenewalFlow flow,
      RenewalNotices notices,
      RenewalBatch batch,
      AppUserRepository users,
      LovService lovs,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock,
      UserDirectory directory) {
    this.records = records;
    this.assignments = assignments;
    this.renewalAccounts = renewalAccounts;
    this.remarks = remarks;
    this.flow = flow;
    this.notices = notices;
    this.batch = batch;
    this.users = users;
    this.lovs = lovs;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
    this.directory = directory;
  }

  /**
   * The Processing Officers (users with {@code RNW_PROCESS}).
   *
   * @return officers
   */
  @Transactional(readOnly = true)
  public List<Officer> officers() {
    return users.findUsernamesWithPermission(Permission.RNW_PROCESS).stream()
        .map(u -> users.findByUsernameIgnoreCase(u).filter(AppUser::isEnabled))
        .flatMap(java.util.Optional::stream)
        .map(u -> new Officer(u.getUsername(), u.getFullName()))
        .toList();
  }

  /**
   * Assigns or re-assigns renewals to a Processing Officer; with no officer, to the current user.
   *
   * @param companyId company
   * @param refs renewals
   * @param po Processing Officer, null for the current user (Assign to Me)
   * @return assigned and refused renewals
   */
  public BatchOutcome assign(Long companyId, List<String> refs, String po) {
    String officer = po == null || po.isBlank() ? currentUser.username() : po.strip();
    if (!users.findUsernamesWithPermission(Permission.RNW_PROCESS).contains(officer)) {
      throw new BusinessRuleException("RNW_PO_INVALID", officer + " is not a Processing Officer");
    }
    return batch.run(
        refs,
        ref -> {
          RenewalCandidate c = records.get(companyId, ref);
          RenewalRecords.requireStage(c, RenewalStage.FOR_PROCESSING, RenewalStage.IN_PROCESSING);
          String previous = c.getAssignedPo();
          c.assignPo(officer);
          assignments.save(
              new RenewalAssignment(c.getId(), RenewalAssignment.Role.PO, officer, previous, null));
          if (c.getStage() == RenewalStage.FOR_PROCESSING) {
            flow.act(
                c,
                "assign_po",
                TransitionNote.comment("Assigned to " + directory.displayName(officer)));
          }
          flow.assign(c, officer);
          audit.record(
              RenewalCodes.ENTITY,
              ref,
              AuditAction.UPDATE,
              "Processing Officer " + directory.displayName(officer));
          notices.users(
              List.of(officer),
              RenewalCodes.EVENT_ASSIGNED,
              c,
              new RenewalNotices.Text(
                  ref + " assigned to you for processing", c.getSnapshot().clientName()));
        });
  }

  /**
   * Returns renewals to Marketing (AO or TL).
   *
   * @param companyId company
   * @param refs renewals
   * @param toLeader true to return to the Marketing TL, false to the AO
   * @param reasonCode reason (list RNW_RETURN_REASON)
   * @param remarksText remarks
   * @return returned and refused renewals
   */
  public BatchOutcome returnToMarketing(
      Long companyId, List<String> refs, boolean toLeader, String reasonCode, String remarksText) {
    if (reasonCode == null || reasonCode.isBlank()) {
      throw new BusinessRuleException(
          "WORKFLOW_REASON_REQUIRED", "Select a reason for 'Return to Marketing'");
    }
    lovs.requireValid(RenewalCodes.LOV_RETURN_REASON, reasonCode, BusinessClock.today(clock));
    String text = RemarkService.requireText(remarksText, "Enter the remarks");
    String label = lovs.label(RenewalCodes.LOV_RETURN_REASON, reasonCode);
    return batch.run(
        refs,
        ref -> {
          RenewalCandidate c = records.get(companyId, ref);
          RenewalRecords.requireStage(c, RenewalStage.FOR_PROCESSING, RenewalStage.IN_PROCESSING);
          flow.act(
              c, toLeader ? "return_to_tl" : "return_to_ao", new TransitionNote(reasonCode, text));
          c.getFlags().setReturned(true);
          remarks.add(c, "Returned by Processing (" + label + "): " + text);
          audit.record(RenewalCodes.ENTITY, ref, AuditAction.REJECT, label + ": " + text);
          if (toLeader && c.getOwnerUnit() != null) {
            notices.teamLeaders(
                companyId,
                c.getOwnerUnit(),
                RenewalCodes.EVENT_RETURNED,
                c,
                new RenewalNotices.Text(ref + " returned by Processing", label + ": " + text));
          } else {
            notices.users(
                Collections.singletonList(c.getAssignedAo()),
                RenewalCodes.EVENT_RETURNED,
                c,
                new RenewalNotices.Text(ref + " returned by Processing", label + ": " + text));
          }
        });
  }

  /**
   * The Computations tab: the renewal account's premium next to the expiring invoice.
   *
   * @param companyId company
   * @param renewalRef renewal
   * @return computations
   */
  @Transactional(readOnly = true)
  public Computations computations(Long companyId, String renewalRef) {
    RenewalCandidate c = records.get(companyId, renewalRef);
    SnapshotPremium expiring = c.getSnapshot().premium();
    Terms before =
        expiring == null
            ? Terms.EMPTY
            : new Terms(
                expiring.currency(),
                expiring.basicPremium(),
                null,
                expiring.grossPremium(),
                expiring.totalSumInsured(),
                expiring.commissionRate(),
                null);
    Terms after = renewalAccounts.of(c).map(ProcessingService::terms).orElse(Terms.EMPTY);
    return new Computations(c.getRenewalArn(), before, after);
  }

  private static Terms terms(Account a) {
    AccountPremium p = a.getPremium();
    return new Terms(
        a.getCurrency(),
        p.netPremium(),
        p.totalCharges(),
        p.grossPremium(),
        a.getTotalSumInsured(),
        p.commissionRate(),
        p.commission());
  }

  /**
   * A Processing Officer.
   *
   * @param username user name
   * @param fullName name
   */
  public record Officer(String username, String fullName) {}

  /**
   * The comparison of the Computations tab.
   *
   * @param renewalArn renewal account, null when not created
   * @param expiring expiring invoice
   * @param renewal renewal account
   */
  public record Computations(String renewalArn, Terms expiring, Terms renewal) {}

  /**
   * Premium terms.
   *
   * @param currency currency
   * @param netPremium basic (net) premium
   * @param charges taxes and charges
   * @param grossPremium gross premium
   * @param sumInsured total sum insured
   * @param commissionRate commission rate
   * @param commission commission
   */
  public record Terms(
      String currency,
      BigDecimal netPremium,
      BigDecimal charges,
      BigDecimal grossPremium,
      BigDecimal sumInsured,
      BigDecimal commissionRate,
      BigDecimal commission) {

    /** No terms. */
    public static final Terms EMPTY = new Terms(null, null, null, null, null, null, null);
  }
}
