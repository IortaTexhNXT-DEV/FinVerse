package com.iortatechnxt.brokerverse.collections.home.service;

import com.iortatechnxt.brokerverse.collections.common.domain.ClxEnums.ItemStatus;
import com.iortatechnxt.brokerverse.collections.common.domain.CollectionItemRepository;
import com.iortatechnxt.brokerverse.collections.common.service.CollectionsWorkCountSource;
import com.iortatechnxt.brokerverse.collections.feed.service.InboxService;
import com.iortatechnxt.brokerverse.collections.files.service.ScheduledFileService;
import com.iortatechnxt.brokerverse.collections.worklist.service.WorklistFilter;
import com.iortatechnxt.brokerverse.collections.worklist.service.WorklistFilter.Work;
import com.iortatechnxt.brokerverse.collections.worklist.service.WorklistQueryService;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The worklist tiles of the Collections home (COLLECTIONS_DESIGN 11, CQ21): my open accounts, all
 * open accounts, unassigned accounts (for those who assign, BRCLXN.052), direct payments returned
 * by insurers (CMRID.009), credit balances (CQ04) and the files published this week.
 */
@Component
@Transactional(readOnly = true)
public class WorklistWorkCounts implements CollectionsWorkCountSource {

  private static final String WORKLIST = "/collections/worklist";
  private static final int FIRST = 10;

  private final CollectionItemRepository items;
  private final WorklistQueryService worklist;
  private final ScheduledFileService files;
  private final CurrentUser currentUser;

  /**
   * Creates the source.
   *
   * @param items items
   * @param worklist worklist reads
   * @param files published files
   * @param currentUser signed-in user
   */
  public WorklistWorkCounts(
      CollectionItemRepository items,
      WorklistQueryService worklist,
      ScheduledFileService files,
      CurrentUser currentUser) {
    this.items = items;
    this.worklist = worklist;
    this.files = files;
    this.currentUser = currentUser;
  }

  @Override
  public List<WorkCount> counts(Long companyId, String username) {
    List<WorkCount> tiles = new ArrayList<>();
    tiles.add(
        new WorkCount(
            "mine",
            "My Open Accounts",
            items.countByCompanyIdAndCurrentHandlerIgnoreCaseAndStatus(
                companyId, username, ItemStatus.OPEN),
            WORKLIST + "?mine=true",
            false,
            FIRST));
    tiles.add(
        new WorkCount(
            "open",
            "All Open Accounts",
            items.countByCompanyIdAndStatus(companyId, ItemStatus.OPEN),
            WORKLIST,
            false,
            FIRST + 1));
    if (currentUser.hasAuthority("CLX_ASSIGN")) {
      tiles.add(
          new WorkCount(
              "unassigned",
              "Unassigned Accounts",
              worklist.count(companyId, unassigned()),
              WORKLIST + "?unassigned=true",
              true,
              FIRST + 2));
    }
    tiles.add(
        new WorkCount(
            "dpReturned",
            "DP Returned by Insurer",
            items.countByCompanyIdAndStatusAndDispositionCode(
                companyId, ItemStatus.OPEN, InboxService.DP_RETURNED_DISPOSITION),
            WORKLIST + "?disposition=" + InboxService.DP_RETURNED_DISPOSITION,
            true,
            FIRST * 2 + 1));
    tiles.add(
        new WorkCount(
            "credit",
            "Credit Balances",
            items.countByCompanyIdAndStatus(companyId, ItemStatus.CREDIT),
            WORKLIST + "?tab=CREDIT",
            false,
            FIRST * 2 + 2));
    tiles.add(
        new WorkCount(
            "files",
            "Files Ready This Week",
            files.readyThisWeek(companyId),
            "/collections/files",
            false,
            FIRST * 2 + FIRST));
    return tiles;
  }

  private static WorklistFilter unassigned() {
    return WorklistFilter.of(ItemStatus.OPEN)
        .with(new Work(null, true, null, null, null, null, null));
  }
}
