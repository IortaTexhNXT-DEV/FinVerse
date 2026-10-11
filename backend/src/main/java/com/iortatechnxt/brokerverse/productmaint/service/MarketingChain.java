package com.iortatechnxt.brokerverse.productmaint.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NoticeDelivery;
import com.iortatechnxt.brokerverse.productmaint.domain.MarketingApproval;
import com.iortatechnxt.brokerverse.productmaint.domain.MarketingApprovalRepository;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageRequest;
import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.domain.AppUserRepository;
import com.iortatechnxt.brokerverse.security.service.UserDirectory;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Marketing approval of a package request (BDOI FRS FRPM.011.02 and the setting {@value
 * #SETTING}): ANY_ONE (one approval by the Team Leader, Team Head or Unit Head, as BRPM.008 states)
 * or TL_TH_UH (the Team Leader, then the Team Head, then the Unit Head approve, each on his own;
 * the request goes to TSU after the Unit Head). A user's level is the user level of the user's
 * profile (Team Leader, Team Head, Unit Head). Every approval is kept in the history.
 */
@Component
@Transactional
public class MarketingChain {

  /** Setting of the chain. */
  public static final String SETTING = "PM_MARKETING_CHAIN";

  /** BDOI's chain. */
  public static final String CHAIN = "TL_TH_UH";

  /** Number of levels of BDOI's chain. */
  public static final int LEVELS = 3;

  private static final String APPROVER_PERMISSION = "PKG_REQUEST_APPROVE";
  private static final Map<String, Integer> LEVEL_OF =
      Map.of("TEAM_LEAD", 1, "TEAM_HEAD", 2, "UNIT_HEAD", LEVELS);
  private static final List<String> NAMES =
      List.of("Marketing Team Leader", "Marketing Team Head", "Marketing Unit Head");

  private final MarketingApprovalRepository approvals;
  private final AppUserRepository users;
  private final UserDirectory directory;
  private final NoticeDelivery delivery;
  private final SystemParameterService parameters;
  private final Clock clock;

  /**
   * Creates the chain.
   *
   * @param approvals approval history
   * @param users users (user level)
   * @param directory holders of the approval permission
   * @param delivery notices to the next approver
   * @param parameters business parameters (setting)
   * @param clock clock
   */
  public MarketingChain(
      MarketingApprovalRepository approvals,
      AppUserRepository users,
      UserDirectory directory,
      NoticeDelivery delivery,
      SystemParameterService parameters,
      Clock clock) {
    this.approvals = approvals;
    this.users = users;
    this.directory = directory;
    this.delivery = delivery;
    this.parameters = parameters;
    this.clock = clock;
  }

  /**
   * Whether BDOI's three-level chain applies.
   *
   * @return true for TL_TH_UH
   */
  public boolean chained() {
    return CHAIN.equals(parameters.text(SETTING, "ANY_ONE").strip());
  }

  /**
   * Records the approval of the current level.
   *
   * @param p request
   * @param user approver
   * @param remarks remarks
   * @return true when the Marketing approval is complete (the request goes to TSU)
   */
  public boolean approve(PackageRequest p, String user, String remarks) {
    if (!chained()) {
      approvals.save(new MarketingApproval(p.getId(), 1, user, remarks, clock.instant()));
      return true;
    }
    int required = p.getRouting().getMarketingLevel() + 1;
    if (levelOf(user) != required) {
      throw new BusinessRuleException(
          "PKG_MARKETING_LEVEL", "This approval is given by the " + NAMES.get(required - 1));
    }
    approvals.save(new MarketingApproval(p.getId(), required, user, remarks, clock.instant()));
    int given = p.getRouting().approveLevel();
    if (given >= LEVELS) {
      return true;
    }
    tellNext(p, given + 1);
    return false;
  }

  /**
   * The approval history of a request.
   *
   * @param requestId request
   * @return approvals in order
   */
  public List<MarketingApproval> history(Long requestId) {
    return approvals.findByRequestIdOrderByIdAsc(requestId);
  }

  /**
   * The level of a user: Team Leader 1, Team Head 2, Unit Head 3 (a user without a level is a Team
   * Leader).
   *
   * @param username user
   * @return level
   */
  public int levelOf(String username) {
    return users
        .findByUsernameIgnoreCase(username)
        .map(AppUser::getUserLevel)
        .map(l -> LEVEL_OF.getOrDefault(l, 1))
        .orElse(1);
  }

  private void tellNext(PackageRequest p, int level) {
    Notice notice =
        new Notice(
            p.getRequestNo() + " for " + NAMES.get(level - 1) + " approval",
            p.getTitle(),
            PackageRequests.link(p),
            PackageRequests.ENTITY,
            String.valueOf(p.getId()));
    for (String u : directory.usersWithPermission(APPROVER_PERMISSION)) {
      if (levelOf(u) == level && !CurrentUser.sameUser(u, p.getCreatedBy())) {
        delivery.toUser(u, notice, "PM_MARKETING_APPROVAL", true);
      }
    }
  }
}
