package com.iortatechnxt.brokerverse.accounting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.accounting.domain.AccountingEventLogRepository;
import com.iortatechnxt.brokerverse.accounting.domain.CostCenterRule;
import com.iortatechnxt.brokerverse.accounting.domain.CostCenterRuleValues;
import com.iortatechnxt.brokerverse.accounting.service.AccountingEventPublisher;
import com.iortatechnxt.brokerverse.accounting.service.BusinessEvent;
import com.iortatechnxt.brokerverse.accounting.service.CostCenterRuleService;
import com.iortatechnxt.brokerverse.alert.domain.AlertRepository;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.journal.domain.JournalBatch;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestData;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Cost-centre rules of the accounting engine (FRBS 3.1.1): a line of an account that requires a
 * cost centre gets it from the first matching rule; without one the event fails, is logged and
 * raises COST_CENTER_MISSING.
 */
@IntegrationTest
class CostCenterRuleIT {

  private static final String EVENT = "FRBS_SERVICE_FEE_ACCRUE";
  private static final String MODULE = "CCRTEST";

  @Autowired private AccountingEventPublisher publisher;
  @Autowired private CostCenterRuleService rules;
  @Autowired private AccountingEventLogRepository eventLog;
  @Autowired private AlertRepository alerts;
  @Autowired private AsUser as;
  @Autowired private TestData data;

  private BusinessEvent accrual(String key) {
    return new BusinessEvent(
        EVENT,
        data.company().getId(),
        data.branch("HO").getId(),
        LocalDate.now(),
        "PHP",
        MODULE,
        key,
        "SFR-" + key.substring(0, 8),
        null,
        null,
        null,
        "Service fee accrual",
        Map.of("AMOUNT", new BigDecimal("250.00")),
        Map.of());
  }

  @Test
  void missingCostCentreFailsTheEventUntilARuleProvidesIt() {
    Long company = data.company().getId();
    String first = UUID.randomUUID().toString();
    assertThatThrownBy(() -> as.run("accountant", () -> publisher.publish(accrual(first))))
        .isInstanceOf(BusinessRuleException.class)
        .extracting("code")
        .isEqualTo(CostCenterRuleService.MISSING);
    assertThat(eventLog.findAll())
        .anyMatch(
            e -> first.equals(e.getSourceReference()) && "FAILED".equals(e.getStatus().name()));
    assertThat(alerts.findAll())
        .anyMatch(
            a ->
                CostCenterRuleService.MISSING.equals(a.getExceptionCode())
                    && first.equals(a.getEntityId()));

    CostCenterRule other =
        as.run(
            "fmanager",
            () ->
                rules.create(
                    company,
                    new CostCenterRuleValues(
                        2, "SOME_OTHER_MODULE", EVENT, null, null, null, "UW", null, true)));
    CostCenterRule rule =
        as.run(
            "fmanager",
            () ->
                rules.create(
                    company,
                    new CostCenterRuleValues(
                        3, MODULE, EVENT, null, null, "5614", "FIN", "Service fees", true)));
    try {
      JournalBatch posted =
          as.run("accountant", () -> publisher.publish(accrual(UUID.randomUUID().toString())));
      assertThat(posted.getLines())
          .filteredOn(l -> "5614".equals(l.getAccount().getCode()))
          .singleElement()
          .extracting(l -> l.getCostCenter())
          .isEqualTo("FIN");
      assertThat(rules.list(company)).extracting(CostCenterRule::getId).contains(rule.getId());
    } finally {
      as.run("fmanager", () -> rules.update(rule.getId(), inactive(rule)));
      as.run("fmanager", () -> rules.update(other.getId(), inactive(other)));
    }
  }

  private static CostCenterRuleValues inactive(CostCenterRule r) {
    return new CostCenterRuleValues(
        r.getPriority(),
        r.getSourceModule(),
        r.getEventType(),
        r.getBranchId(),
        r.getPartyCode(),
        r.getAccountCode(),
        r.getCostCenter(),
        r.getDescription(),
        false);
  }
}
