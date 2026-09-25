package com.iortatechnxt.brokerverse.commission.service;

import com.iortatechnxt.brokerverse.commission.domain.CommissionEnums.ListSource;
import com.iortatechnxt.brokerverse.commission.domain.DpItem.Submission;
import com.iortatechnxt.brokerverse.commission.domain.DpList;
import com.iortatechnxt.brokerverse.commission.domain.DpList.FileKey;
import com.iortatechnxt.brokerverse.commission.domain.DpList.Origin;
import com.iortatechnxt.brokerverse.commission.domain.DpListRepository;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.util.Sha256;
import com.iortatechnxt.brokerverse.opsledger.domain.FlowInEnums.Trigger;
import com.iortatechnxt.brokerverse.opsledger.domain.FlowInRun;
import com.iortatechnxt.brokerverse.opsledger.domain.FlowInRun.FileRef;
import com.iortatechnxt.brokerverse.opsledger.service.FlowInHandler.FlowInFile;
import com.iortatechnxt.brokerverse.opsledger.service.FlowInService;
import com.iortatechnxt.brokerverse.opsledger.service.port.CollectionFeed;
import com.iortatechnxt.brokerverse.opsledger.service.port.FeedItem;
import com.iortatechnxt.brokerverse.organization.domain.Branch;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Direct payment list intake and tracking (CMRID.001): upload of a branch's list as a run of the
 * flow-in feed {@code COLLECTION_DP_LIST}, the pull of lists waiting in the Collection system
 * ({@code CollectionFeed}; nothing with the manual transport, OQ38), the lists received and the
 * submission tracker per branch for a period. Uploads and pulls are not transactional: each account
 * commits on its own.
 */
@Service
public class DpListService {

  private final FlowInService flowIn;
  private final DpListHandler handler;
  private final DpIntakeService intake;
  private final DpListRepository lists;
  private final CollectionFeed collection;
  private final OrganizationService organization;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param flowIn flow-in framework
   * @param handler list handler
   * @param intake intake
   * @param lists lists
   * @param collection Collection system port
   * @param organization branches
   * @param clock clock
   */
  public DpListService(
      FlowInService flowIn,
      DpListHandler handler,
      DpIntakeService intake,
      DpListRepository lists,
      CollectionFeed collection,
      OrganizationService organization,
      Clock clock) {
    this.flowIn = flowIn;
    this.handler = handler;
    this.intake = intake;
    this.lists = lists;
    this.collection = collection;
    this.organization = organization;
    this.clock = clock;
  }

  /**
   * Uploads a DP list (CMRID.001).
   *
   * @param companyId company
   * @param fileName file name (naming convention checked)
   * @param content bytes
   * @return the list with its counts
   */
  public DpList upload(Long companyId, String fileName, byte[] content) {
    DpListHandler.origin(fileName);
    AtomicReference<DpList> taken = new AtomicReference<>();
    FlowInRun run =
        flowIn.run(
            DpListHandler.FEED,
            Trigger.UPLOAD,
            new FileRef(fileName, Sha256.hex(content)),
            ctx -> taken.set(handler.process(companyId, new FlowInFile(fileName, content), ctx)));
    if (taken.get() == null) {
      throw new BusinessRuleException(
          "DP_LIST_REFUSED",
          "The DP list was not taken in: "
              + (run.getErrorDetail() == null ? run.getMessage() : run.getErrorDetail()));
    }
    return intake.requireList(taken.get().getId());
  }

  /**
   * Takes in the DP lists waiting in the Collection system (CMRID.001, OQ38).
   *
   * @param companyId company
   * @return the lists created (none with the manual transport)
   */
  public List<DpList> pull(Long companyId) {
    List<FeedItem> pending = collection.pending(companyId, DpListHandler.FEED);
    Map<String, List<FeedItem>> byBranch = new LinkedHashMap<>();
    for (FeedItem item : pending) {
      String branch = item.fields().getOrDefault("Branch", DpListHandler.HEAD_OFFICE);
      byBranch.computeIfAbsent(branch.toUpperCase(Locale.ROOT), k -> new ArrayList<>()).add(item);
    }
    List<DpList> created = new ArrayList<>();
    byBranch.forEach(
        (branch, items) ->
            flowIn.run(
                DpListHandler.FEED,
                Trigger.MANUAL,
                FileRef.NONE,
                ctx -> {
                  Origin origin =
                      new Origin(ListSource.COLLECTION_FEED, branch, LocalDate.now(clock));
                  DpList list = intake.open(companyId, origin, FileKey.NONE);
                  int row = 0;
                  for (FeedItem item : items) {
                    int rowNo = ++row;
                    Submission s = DpListHandler.submission(item.fields(), origin);
                    ctx.accept(
                        list.getListNo() + ":" + item.key(),
                        item.fields().toString(),
                        () -> intake.add(list.getId(), rowNo, s));
                  }
                  created.add(intake.finish(list.getId(), ctx.runNo()));
                }));
    return created;
  }

  /**
   * Lists received in a period.
   *
   * @param companyId company
   * @param from first submission date
   * @param to last submission date
   * @param pageable page
   * @return lists, newest first
   */
  @Transactional(readOnly = true)
  public Page<DpList> lists(Long companyId, LocalDate from, LocalDate to, Pageable pageable) {
    return lists.findByCompanyIdAndSubmissionDateBetweenOrderBySubmissionDateDescIdDesc(
        companyId, from, to, pageable);
  }

  /**
   * The submission tracker (CMRID.001): every branch of the company with the lists it submitted in
   * the period; lists of names that are not branches are shown too.
   *
   * @param companyId company
   * @param from first submission date
   * @param to last submission date
   * @return one row per branch
   */
  @Transactional(readOnly = true)
  public List<Submissions> tracker(Long companyId, LocalDate from, LocalDate to) {
    List<DpList> received =
        lists.findByCompanyIdAndSubmissionDateBetweenOrderByBranchCodeAsc(companyId, from, to);
    Map<String, Submissions> rows = new LinkedHashMap<>();
    Map<String, String> alias = new HashMap<>();
    for (Branch b : organization.listBranches(companyId)) {
      String code = b.isHeadOffice() ? DpListHandler.HEAD_OFFICE : key(b.getCode());
      rows.put(code, Submissions.none(code, b.getName()));
      alias.put(key(b.getName()), code);
      alias.put(code, code);
    }
    for (DpList l : received) {
      String code = alias.getOrDefault(key(l.getBranchCode()), key(l.getBranchCode()));
      rows.merge(code, Submissions.none(code, null).with(l), (current, added) -> current.with(l));
    }
    return new ArrayList<>(rows.values());
  }

  private static String key(String value) {
    return value.replaceAll("\\s", "").toUpperCase(Locale.ROOT);
  }

  /**
   * The lists of a branch in a period.
   *
   * @param branchCode branch in the file names
   * @param branchName branch name, null when not a branch of the company
   * @param lists lists received
   * @param accounts accounts received
   * @param lastSubmission last submission date, null when missing
   */
  public record Submissions(
      String branchCode, String branchName, int lists, int accounts, LocalDate lastSubmission) {

    static Submissions none(String code, String name) {
      return new Submissions(code, name, 0, 0, null);
    }

    Submissions with(DpList l) {
      LocalDate last =
          lastSubmission == null || l.getSubmissionDate().isAfter(lastSubmission)
              ? l.getSubmissionDate()
              : lastSubmission;
      return new Submissions(branchCode, branchName, lists + 1, accounts + l.getItemCount(), last);
    }

    /**
     * Whether the branch submitted in the period.
     *
     * @return true when at least one list was received
     */
    public boolean received() {
      return lists > 0;
    }
  }
}
