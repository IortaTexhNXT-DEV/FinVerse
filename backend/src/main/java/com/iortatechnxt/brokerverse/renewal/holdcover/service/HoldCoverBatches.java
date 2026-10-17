package com.iortatechnxt.brokerverse.renewal.holdcover.service;

import com.iortatechnxt.brokerverse.renewal.domain.Bucket;
import com.iortatechnxt.brokerverse.renewal.domain.HoldCoverAsk;
import com.iortatechnxt.brokerverse.renewal.domain.HoldCoverAskRepository;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidateRepository;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalStage;
import com.iortatechnxt.brokerverse.renewal.service.RenewalParameters;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The automatic hold cover requests (FRRN.036.02, FRRN.036.04, FRRN.036.05): the Non-CBG request
 * once the client confirms the renewal; the Clean CBG accounts the set days before expiry, one file
 * per insurer and a separate file for the co-insured accounts; and on the set day of the month the
 * extension of the expired and unbooked CBG Home accounts.
 */
@Service
@Transactional
public class HoldCoverBatches {

  private static final Logger LOG = LoggerFactory.getLogger(HoldCoverBatches.class);
  private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMddyyyy");
  private static final Set<RenewalStage> OPEN =
      EnumSet.complementOf(EnumSet.of(RenewalStage.RENEWED, RenewalStage.CLOSED));
  private static final int CBG_DAYS_BEFORE = 75;
  private static final int EXTENSION_DAY = 15;
  private static final int EXTENSION_DAYS = 30;

  private final RenewalCandidateRepository candidates;
  private final HoldCoverAskRepository asks;
  private final HoldCoverRequests requests;
  private final HoldCoverFiles files;
  private final RenewalParameters renewal;
  private final SystemParameterService parameters;
  private final TransactionTemplate newTransaction;

  /**
   * Creates the batches.
   *
   * @param candidates renewals
   * @param asks requests
   * @param requests hold cover requests
   * @param files request files
   * @param renewal CBG segments
   * @param parameters days and switches
   * @param txManager transactions of the automatic requests
   */
  @SuppressWarnings("java:S107") // constructor injection
  public HoldCoverBatches(
      RenewalCandidateRepository candidates,
      HoldCoverAskRepository asks,
      HoldCoverRequests requests,
      HoldCoverFiles files,
      RenewalParameters renewal,
      SystemParameterService parameters,
      PlatformTransactionManager txManager) {
    this.candidates = candidates;
    this.asks = asks;
    this.requests = requests;
    this.files = files;
    this.renewal = renewal;
    this.parameters = parameters;
    this.newTransaction = new TransactionTemplate(txManager);
    this.newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  /**
   * After the client confirms a Non-CBG renewal, requests its hold cover from each insurer, once
   * the acceptance is committed and in a transaction of its own: a request that cannot be sent
   * never undoes the acceptance.
   *
   * @param candidateId renewal accepted
   */
  @Transactional(propagation = Propagation.SUPPORTS)
  public void afterAcceptance(Long candidateId) {
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      requestAfterAcceptance(candidateId);
      return;
    }
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            requestAfterAcceptance(candidateId);
          }
        });
  }

  private void requestAfterAcceptance(Long candidateId) {
    try {
      newTransaction.executeWithoutResult(
          s ->
              candidates
                  .findById(candidateId)
                  .filter(this::autoRequested)
                  .ifPresent(
                      c ->
                          requests.requestFor(
                              c, HoldCoverAsk.REQUEST, requests.defaultDays(c), null)));
    } catch (RuntimeException e) {
      LOG.warn(
          "Hold cover of renewal {} not requested automatically: {}", candidateId, e.getMessage());
    }
  }

  private boolean autoRequested(RenewalCandidate c) {
    var p = c.getSnapshot().product();
    boolean nonCbg = p == null || !renewal.cbgSegment(p.segment());
    boolean on = "true".equals(parameters.text("RNW_HOLD_COVER_AUTO_NON_CBG", "true").strip());
    return on && nonCbg && asks.findByCandidateIdOrderByIdDesc(c.getId()).isEmpty();
  }

  /**
   * Requests the hold cover of the Clean CBG renewal accounts expiring the set days later.
   *
   * @param today business date
   * @return requests created
   */
  public int cbgClean(LocalDate today) {
    LocalDate expiry =
        today.plusDays(parameters.intValue("RNW_HOLD_COVER_CBG_DAYS_BEFORE", CBG_DAYS_BEFORE));
    List<HoldCoverFiles.Line> lines = new ArrayList<>();
    for (RenewalCandidate c : candidates.findByStageIn(OPEN)) {
      var p = c.getSnapshot().product();
      boolean eligible =
          p != null
              && renewal.cbgSegment(p.segment())
              && c.getBucket() == Bucket.CLEAN
              && expiry.equals(c.getExpiryDate())
              && asks.findByCandidateIdOrderByIdDesc(c.getId()).isEmpty();
      if (eligible) {
        requests
            .requestFor(c, HoldCoverAsk.CBG_BATCH, requests.defaultDays(c), null)
            .forEach(a -> lines.add(new HoldCoverFiles.Line(c, a)));
      }
    }
    sendByInsurer(lines, "HOLD_COVER_CBG_", today);
    return lines.size();
  }

  /**
   * On the set day of the month, extends the hold cover of the expired and unbooked CBG Home
   * accounts.
   *
   * @param today business date
   * @return extensions created
   */
  public int cbgHomeExtensions(LocalDate today) {
    if (today.getDayOfMonth()
        != parameters.intValue("RNW_HOLD_COVER_CBG_HOME_EXTENSION_DAY", EXTENSION_DAY)) {
      return 0;
    }
    int days = parameters.intValue("RNW_HOLD_COVER_CBG_HOME_EXTENSION", EXTENSION_DAYS);
    List<HoldCoverFiles.Line> lines = new ArrayList<>();
    for (RenewalCandidate c : candidates.findByStageIn(OPEN)) {
      if (requests.cbgHome(c) && c.getExpiryDate().isBefore(today)) {
        LocalDate from = c.effectiveExpiry().isAfter(today) ? c.effectiveExpiry() : today;
        requests
            .requestFor(c, HoldCoverAsk.MONTHLY_EXTENSION, days, from)
            .forEach(a -> lines.add(new HoldCoverFiles.Line(c, a)));
      }
    }
    return lines.size();
  }

  private void sendByInsurer(List<HoldCoverFiles.Line> lines, String prefix, LocalDate today) {
    Map<String, List<HoldCoverFiles.Line>> files = new LinkedHashMap<>();
    for (HoldCoverFiles.Line l : lines) {
      boolean coInsured = l.ask().getSharePercent() != null;
      files
          .computeIfAbsent(
              l.ask().getInsurerCode() + (coInsured ? "_COINSURED" : ""), k -> new ArrayList<>())
          .add(l);
    }
    String date = DATE.format(today);
    files.forEach(
        (key, group) -> {
          String name = prefix + key + "_" + date;
          this.files.send(group.get(0).ask().getInsurerCode(), group, name, name);
        });
  }
}
