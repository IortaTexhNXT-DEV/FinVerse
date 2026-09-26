package com.iortatechnxt.brokerverse.coa.service;

import com.iortatechnxt.brokerverse.coa.domain.GlAccount;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Applies every account level posting control to a proposed posting line.
 *
 * <p>Returns all violations at once (instead of failing on the first) so users can correct a
 * journal in one pass.
 */
@Service
public class PostingEligibilityService {

  private static final String ACCOUNT = "Account ";

  /**
   * Validates an account for a posting.
   *
   * @param account target account
   * @param ctx posting facts
   * @return list of violation messages; empty when eligible
   */
  public List<String> violations(GlAccount account, PostingContext ctx) {
    List<String> errors = new ArrayList<>();
    String code = account.getCode();
    if (!account.isActive()) {
      errors.add(ACCOUNT + code + " is not active/authorized");
    }
    if (!account.isPostable()) {
      errors.add(ACCOUNT + code + " is a heading and cannot be posted to");
    }
    if (account.isFrozen()) {
      errors.add(ACCOUNT + code + " is frozen: " + account.getFreezeReason());
    }
    if (account.isClosedOn(ctx.valueDate()) || ctx.valueDate().isBefore(account.getOpenedOn())) {
      errors.add(ACCOUNT + code + " is not open on " + ctx.valueDate());
    }
    if (!account.acceptsCurrency(ctx.currency())) {
      errors.add("Currency " + ctx.currency() + " is not allowed for account " + code);
    }
    if (!account.acceptsBranch(ctx.branchId())) {
      errors.add("Branch is not permitted to post to account " + code);
    }
    addManualControls(account, ctx, errors);
    addDimensionControls(account, ctx, errors);
    return errors;
  }

  private static void addManualControls(
      GlAccount account, PostingContext ctx, List<String> errors) {
    if (!ctx.manual()) {
      return;
    }
    String code = account.getCode();
    if (!account.isAllowManualPosting()) {
      errors.add("Manual posting is not allowed to account " + code);
    }
    if (!account.acceptsAnyRole(ctx.roleCodes())) {
      errors.add("Your role has no access code for account " + code);
    }
    if (account.isControlAccount() && !ctx.hasSubLedgerParty()) {
      errors.add("Control account " + code + " requires a sub-ledger reference");
    }
  }

  private static void addDimensionControls(
      GlAccount account, PostingContext ctx, List<String> errors) {
    if (account.isCostCenterRequired() && !ctx.hasCostCenter()) {
      errors.add("Cost centre is mandatory for account " + account.getCode());
    }
    if (account.isBusinessLineRequired() && !ctx.hasBusinessLine()) {
      errors.add("Line of business is mandatory for account " + account.getCode());
    }
  }
}
