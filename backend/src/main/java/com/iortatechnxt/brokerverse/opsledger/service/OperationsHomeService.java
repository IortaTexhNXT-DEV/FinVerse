package com.iortatechnxt.brokerverse.opsledger.service;

import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.lov.domain.LovValue;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.opsledger.service.port.OpsWorkCountSource;
import com.iortatechnxt.brokerverse.opsledger.service.port.OpsWorkCountSource.Section;
import com.iortatechnxt.brokerverse.opsledger.service.port.OpsWorkCountSource.WorkCount;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Operations home (BRQID.003): one card per team section with the work counts of the ledger and
 * of every Operations module ({@link OpsWorkCountSource}), shown only to users holding one of the
 * section's permissions, and the links to the integrated applications (LOV {@code
 * OPS_EXTERNAL_LINK}, label "Name|https://url").
 */
@Service
@Transactional(readOnly = true)
public class OperationsHomeService {

  /** LOV of the external application links. */
  public static final String LINK_LOV = "OPS_EXTERNAL_LINK";

  private static final Map<Section, List<String>> TEAMS = teams();

  private final List<OpsWorkCountSource> sources;
  private final CurrentUser currentUser;
  private final LovService lovs;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param sources count sources of the ledger and the modules
   * @param currentUser current user
   * @param lovs lists of values (links)
   * @param clock clock
   */
  public OperationsHomeService(
      List<OpsWorkCountSource> sources, CurrentUser currentUser, LovService lovs, Clock clock) {
    this.sources = sources;
    this.currentUser = currentUser;
    this.lovs = lovs;
    this.clock = clock;
  }

  private static Map<Section, List<String>> teams() {
    Map<Section, List<String>> teams = new EnumMap<>(Section.class);
    teams.put(
        Section.CASHIERING,
        List.of("CASH_RECEIPT", "CASH_APPLY", "CASH_APPROVE", "CASH_DISPOSITION", "CWT_PROCESS"));
    teams.put(
        Section.REMITTANCE,
        List.of("REMIT_EXTRACT", "REMIT_PROCESS", "REMIT_APPROVE", "HOLD_REQUEST", "HOLD_APPROVE"));
    teams.put(Section.PRODRECON, List.of("RECON_PROCESS", "RECON_SEND"));
    teams.put(Section.ADJUSTMENT, List.of("ADJ_REQUEST", "ADJ_PROCESS", "ADJ_APPROVE", "ADJ_POST"));
    teams.put(
        Section.COMMISSION,
        List.of("COMMREC_PROCESS", "COMMREC_APPROVE", "INCENTIVE_MANAGE", "BIR_CERT_ACK"));
    teams.put(Section.DISBURSEMENT, List.of("DISB_PROCESS"));
    teams.put(Section.INTERFACES, List.of("FLOWIN_MANAGE"));
    return teams;
  }

  /**
   * The sections the current user works in, with their counts, in display order.
   *
   * @param companyId company
   * @return sections
   */
  public List<SectionCounts> sections(Long companyId) {
    Map<Section, List<WorkCount>> bySection = new EnumMap<>(Section.class);
    for (OpsWorkCountSource source : sources) {
      for (WorkCount count : source.counts(companyId)) {
        if (mayWorkIn(count.section())) {
          bySection.computeIfAbsent(count.section(), s -> new ArrayList<>()).add(count);
        }
      }
    }
    return bySection.entrySet().stream()
        .map(e -> new SectionCounts(e.getKey(), TEAMS.get(e.getKey()), e.getValue()))
        .toList();
  }

  /**
   * Whether the current user works in a section.
   *
   * @param section section
   * @return true when holding one of its permissions
   */
  public boolean mayWorkIn(Section section) {
    return TEAMS.getOrDefault(section, List.of()).stream().anyMatch(currentUser::hasAuthority);
  }

  /**
   * Links to the integrated applications (BRQID.003).
   *
   * @return links; a value without "|url" has no address yet
   */
  public List<ExternalLink> links() {
    return lovs.activeValues(LINK_LOV, LocalDate.now(clock)).stream()
        .map(OperationsHomeService::link)
        .toList();
  }

  private static ExternalLink link(LovValue value) {
    String label = value.getLabel();
    int bar = label.indexOf('|');
    return bar < 0
        ? new ExternalLink(value.getCode(), label.strip(), null)
        : new ExternalLink(
            value.getCode(), label.substring(0, bar).strip(), label.substring(bar + 1).strip());
  }

  /**
   * A section of the home with its counts.
   *
   * @param section section
   * @param permissions permissions that open it
   * @param counts tiles
   */
  public record SectionCounts(Section section, List<String> permissions, List<WorkCount> counts) {

    /** Defensive copies. */
    public SectionCounts {
      permissions = List.copyOf(permissions);
      counts = List.copyOf(counts);
    }
  }

  /**
   * A link to an integrated application.
   *
   * @param code LOV code
   * @param name name
   * @param url address, null when not configured yet
   */
  public record ExternalLink(String code, String name, String url) {}
}
