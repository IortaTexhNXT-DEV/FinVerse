package com.iortatechnxt.brokerverse.csf.cases.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.csf.cases.domain.CsfCase.AccountRef;
import com.iortatechnxt.brokerverse.csf.cases.domain.CsfCase.Category;
import com.iortatechnxt.brokerverse.csf.cases.domain.CsfCase.Party;
import com.iortatechnxt.brokerverse.csf.cases.domain.CsfCaseSetupStore;
import com.iortatechnxt.brokerverse.csf.service.CsfClients;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * The inputs of a case checked against the case lists of values, the client and its accounts, the
 * turnaround times and the case parameters (FR-CSF-050, 051, 061).
 */
@Component
public class CsfCaseInputs {

  private final CsfClients clients;
  private final CsfCaseSetupStore setup;
  private final LovService lovs;
  private final SystemParameterService parameters;
  private final Clock clock;

  /**
   * Creates the checks.
   *
   * @param clients clients and accounts
   * @param setup turnaround times
   * @param lovs case lists
   * @param parameters case parameters
   * @param clock clock
   */
  public CsfCaseInputs(
      CsfClients clients,
      CsfCaseSetupStore setup,
      LovService lovs,
      SystemParameterService parameters,
      Clock clock) {
    this.clients = clients;
    this.setup = setup;
    this.lovs = lovs;
    this.parameters = parameters;
    this.clock = clock;
  }

  /**
   * The client of a case, or the caller who is not a client.
   *
   * @param companyId company
   * @param clientId client, may be null
   * @param callerName caller name
   * @param callerContact caller contact
   * @return party
   */
  public Party party(Long companyId, Long clientId, String callerName, String callerContact) {
    if (clientId != null) {
      Client client = clients.require(companyId, clientId);
      return new Party(client.getId(), client.getCode(), client.getDisplayName(), null, null);
    }
    if (blankToNull(callerName) == null || blankToNull(callerContact) == null) {
      throw new BusinessRuleException(
          "CSF_CASE_CALLER_REQUIRED", "Enter the caller's name and a contact number or e-mail");
    }
    return new Party(null, null, null, callerName.strip(), callerContact.strip());
  }

  /**
   * The account of a case.
   *
   * @param companyId company
   * @param clientId client
   * @param accountId account, may be null
   * @return account reference
   */
  public AccountRef account(Long companyId, Long clientId, Long accountId) {
    if (accountId == null || clientId == null) {
      return new AccountRef(null, null, null);
    }
    Account a = clients.requireAccount(clients.require(companyId, clientId), accountId);
    List<String> policies = a.getPolicyNumbers();
    return new AccountRef(
        a.getId(), a.getArn(), policies == null || policies.isEmpty() ? null : policies.get(0));
  }

  /**
   * The categories of a new case: every mandatory field filled with an active value.
   *
   * @param k categories as entered
   * @return categories
   */
  public Category category(Category k) {
    List<String> missing = new ArrayList<>();
    if (k == null) {
      missing.add("channel, case type, nature of transaction and the other categories");
    } else {
      addIfBlank(missing, k.channel(), "channel");
      addIfBlank(missing, k.caseType(), "case type");
      addIfBlank(missing, k.nature(), "nature of transaction");
      addIfBlank(missing, k.productType(), "product type");
      addIfBlank(missing, k.marketSegment(), "market segment");
      addIfBlank(missing, k.fulfilmentUnit(), "fulfilment unit");
    }
    if (!missing.isEmpty()) {
      throw new BusinessRuleException(
          "CSF_CASE_FIELDS_REQUIRED",
          "Complete the mandatory fields - " + String.join(", ", missing));
    }
    return new Category(
        active(CsfCaseCodes.LOV_CHANNEL, k.channel(), "channel"),
        active(CsfCaseCodes.LOV_TYPE, k.caseType(), "case type"),
        active(CsfCaseCodes.LOV_NATURE, k.nature(), "nature of transaction"),
        active(CsfCaseCodes.LOV_PRODUCT, k.productType(), "product type"),
        active(CsfCaseCodes.LOV_SEGMENT, k.marketSegment(), "market segment"),
        active(CsfCaseCodes.LOV_UNIT, k.fulfilmentUnit(), "fulfilment unit"),
        active(
            CsfCaseCodes.LOV_PRIORITY,
            Optional.ofNullable(blankToNull(k.priority())).orElse(CsfCaseCodes.NORMAL),
            "priority"));
  }

  /**
   * An active value of a case list.
   *
   * @param lov list
   * @param code code
   * @param name field name for the message
   * @return code
   */
  public String active(String lov, String code, String name) {
    String value = blankToNull(code);
    if (value == null) {
      throw new BusinessRuleException("CSF_CASE_FIELDS_REQUIRED", "Select the " + name);
    }
    boolean ok =
        lovs.activeValues(lov, BusinessClock.today(clock)).stream()
            .anyMatch(v -> v.getCode().equals(value));
    if (!ok) {
      throw new BusinessRuleException(
          "CSF_CASE_VALUE_INACTIVE",
          "The value " + lovs.label(lov, value) + " is no longer in use. Select another value");
    }
    return value;
  }

  /**
   * The turnaround time of a case type.
   *
   * @param caseType case type
   * @return working days
   */
  public int tatOf(String caseType) {
    return setup
        .typeSetting(caseType)
        .map(CsfCaseSetupStore.TypeSetting::tatDays)
        .orElseThrow(
            () ->
                new BusinessRuleException(
                    "CSF_CASE_TAT_MISSING",
                    "The case type "
                        + lovs.label(CsfCaseCodes.LOV_TYPE, caseType)
                        + " has no turnaround time. Ask the administrator to set it on Case"
                        + " Routing"));
  }

  /**
   * Whether a second open case of the same transaction is refused.
   *
   * @return true when refused
   */
  public boolean duplicateCheck() {
    return Boolean.parseBoolean(parameters.text(CsfCaseCodes.PARAM_DUPLICATE, "true").strip());
  }

  /**
   * The prefix of the Case IDs of the current year.
   *
   * @return prefix, e.g. CASE-2026
   */
  public String numberPrefix() {
    return parameters.text(CsfCaseCodes.PARAM_PREFIX, "CASE").strip()
        + "-"
        + BusinessClock.currentYear(clock).getValue();
  }

  /**
   * The label of a code.
   *
   * @param lov list
   * @param code code
   * @return label
   */
  public String label(String lov, String code) {
    return lovs.label(lov, code);
  }

  /**
   * Refuses a blank text.
   *
   * @param value text
   * @param code error code
   * @param message message
   */
  public static void requireText(String value, String code, String message) {
    if (blankToNull(value) == null) {
      throw new BusinessRuleException(code, message);
    }
  }

  /**
   * A stripped text, null when blank.
   *
   * @param s text
   * @return text or null
   */
  public static String blankToNull(String s) {
    return s == null || s.isBlank() ? null : s.strip();
  }

  /**
   * The case type and the nature of transaction in words.
   *
   * @param caseType case type
   * @param nature nature of transaction
   * @return text
   */
  public String categoryText(String caseType, String nature) {
    return label(CsfCaseCodes.LOV_TYPE, caseType) + " / " + label(CsfCaseCodes.LOV_NATURE, nature);
  }

  /**
   * A comment appended to a history summary.
   *
   * @param comment comment, may be blank
   * @return ": comment" or empty
   */
  public static String suffix(String comment) {
    String c = blankToNull(comment);
    return c == null ? "" : ": " + c;
  }

  private static void addIfBlank(List<String> missing, String value, String name) {
    if (blankToNull(value) == null) {
      missing.add(name);
    }
  }
}
