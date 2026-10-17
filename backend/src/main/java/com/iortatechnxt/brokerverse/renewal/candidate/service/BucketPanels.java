package com.iortatechnxt.brokerverse.renewal.candidate.service;

import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.renewal.candidate.service.CandidateFilter.Flags;
import com.iortatechnxt.brokerverse.renewal.candidate.service.CandidateFilter.Tab;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/**
 * The bucket panels of the renewal accounts (BDOI Renewal FRS FRRN.002.05) and the sort of a list
 * (FRRN.002.06). The third bucket is chosen by the parameter {@value #THIRD_BUCKET}: NON_RENEWABLE
 * (BDOI's panels Clean, Review, Non-Renewable and All; the Exception accounts show under Review) or
 * EXCEPTION (Clean, Review, Exception and All).
 */
@Component
public class BucketPanels {

  /** Parameter: NON_RENEWABLE or EXCEPTION. */
  public static final String THIRD_BUCKET = "RNW_THIRD_BUCKET";

  /** Value of the parameter for the Clean / Review / Exception panels. */
  public static final String EXCEPTION = "EXCEPTION";

  private static final Map<String, String> SORT_KEYS =
      Map.of(
          "expiry", "snapshot.expiryDate",
          "ref", "renewalRef",
          "stage", "stage",
          "assured", "snapshot.client.assuredName",
          "client", "snapshot.client.clientName",
          "premium", "snapshot.premium.basicPremium",
          "sumInsured", "snapshot.premium.totalSumInsured",
          "invoice", "expiringInvoiceNo");

  private static final String[] MONTHS = {
    "JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"
  };

  private final SystemParameterService parameters;
  private final CurrentUser currentUser;

  /**
   * Creates the panels.
   *
   * @param parameters system parameters
   * @param currentUser user (the In Processing tab of a Processing Officer)
   */
  public BucketPanels(SystemParameterService parameters, CurrentUser currentUser) {
    this.parameters = parameters;
    this.currentUser = currentUser;
  }

  /**
   * The third panel in use.
   *
   * @return NON_RENEWABLE or EXCEPTION
   */
  public String thirdBucket() {
    String v = parameters.text(THIRD_BUCKET, "NON_RENEWABLE");
    return EXCEPTION.equals(v == null ? "" : v.strip()) ? EXCEPTION : "NON_RENEWABLE";
  }

  /**
   * The criteria with the bucket panels of the setting in use.
   *
   * @param filter criteria
   * @return criteria
   */
  public CandidateFilter apply(CandidateFilter filter) {
    CandidateFilter f = ownProcessing(filter);
    return EXCEPTION.equals(thirdBucket()) ? exceptionPanels(f) : f;
  }

  /**
   * The In Processing tab of a Processing Officer (without the assignment function) lists only the
   * renewals assigned to him (FRRN.003.05).
   */
  private CandidateFilter ownProcessing(CandidateFilter filter) {
    if (filter.tab() != Tab.IN_PROCESSING
        || currentUser.optionalUsername().isEmpty()
        || !currentUser.hasAuthority(RenewalCodes.PROCESS)
        || currentUser.hasAuthority(RenewalCodes.PROCESS_ASSIGN)) {
      return filter;
    }
    Flags f = filter.flags() == null ? Flags.NONE : filter.flags();
    return new CandidateFilter(
        filter.companyId(),
        filter.tab(),
        filter.search(),
        filter.expiryFrom(),
        filter.expiryTo(),
        filter.codes(),
        new Flags(
            f.assignedToMe(),
            currentUser.username(),
            f.returned(),
            f.nrns(),
            f.urgent(),
            f.kycDue(),
            f.dueWithinDays(),
            f.disposed()));
  }

  private static CandidateFilter exceptionPanels(CandidateFilter filter) {
    if (filter.tab() == Tab.BUCKET_REVIEW) {
      return filter.withTab(Tab.BUCKET_REVIEW_ONLY);
    }
    return filter.tab() == Tab.BUCKET_NON_RENEWABLE ? filter.withTab(Tab.EXCEPTIONS) : filter;
  }

  /**
   * The file name of the RMEL download (FRRN.004.06): {@code RMEL_<MMM>_<YYYY>_<MMDDYYYY>.xlsx},
   * the month and year of the list and the date of the extraction.
   *
   * @param listFrom first expiry of the list, or null for the month of the extraction
   * @param extractedOn date of the extraction
   * @return file name, e.g. RMEL_MAR_2027_10102026.xlsx
   */
  public static String rmelFileName(LocalDate listFrom, LocalDate extractedOn) {
    LocalDate month = listFrom == null ? extractedOn : listFrom;
    return "RMEL_"
        + MONTHS[month.getMonthValue() - 1]
        + "_"
        + month.getYear()
        + "_"
        + extractedOn.format(DateTimeFormatter.ofPattern("MMddyyyy"))
        + ".xlsx";
  }

  /**
   * The sort of a list: {@code key,asc} or {@code key,desc} on the expiry date, reference, stage,
   * assured, client, premium, sum insured or invoice; null for the default order.
   *
   * @param sort sort text
   * @return sort or null
   */
  public static Sort sort(String sort) {
    if (sort == null || sort.isBlank()) {
      return null;
    }
    String[] parts = sort.split(",");
    String property = SORT_KEYS.get(parts[0].strip());
    if (property == null) {
      return null;
    }
    boolean desc = parts.length > 1 && "desc".equals(parts[1].strip());
    return Sort.by(
        desc ? Sort.Order.desc(property) : Sort.Order.asc(property), Sort.Order.asc("id"));
  }
}
