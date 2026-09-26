package com.iortatechnxt.brokerverse.eb;

import static com.iortatechnxt.brokerverse.eb.EbFixtures.AO;
import static com.iortatechnxt.brokerverse.eb.EbFixtures.hmo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.eb.domain.EbItemStatus;
import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import com.iortatechnxt.brokerverse.eb.domain.EbResponsibleParty;
import com.iortatechnxt.brokerverse.eb.domain.EbTrackedItem;
import com.iortatechnxt.brokerverse.eb.domain.EbTrackedItemRepository;
import com.iortatechnxt.brokerverse.eb.tracked.service.ItemFollowUpJob;
import com.iortatechnxt.brokerverse.eb.tracked.service.TrackedItemQuery;
import com.iortatechnxt.brokerverse.eb.tracked.service.TrackedItemQuery.ItemCriteria;
import com.iortatechnxt.brokerverse.eb.tracked.service.TrackedItemQuery.ItemRow;
import com.iortatechnxt.brokerverse.eb.tracked.service.TrackedItemService;
import com.iortatechnxt.brokerverse.eb.tracked.service.TrackedItemService.Action;
import com.iortatechnxt.brokerverse.eb.tracked.service.TrackedItemService.ItemInput;
import com.iortatechnxt.brokerverse.eb.tracked.service.TrackedItemService.StatusChange;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Tracked items and their follow-ups (FR-EB-057; job EB_ITEM_FOLLOWUP; wave E1-C): opening,
 * receiving, releasing and closing items, the Pending Items filters, the follow-up e-mails every
 * EB_FOLLOWUP_DAYS working days past due and the escalation after EB_FOLLOWUP_MAX.
 */
@IntegrationTest
class EbTrackedItemsIT {

  @Autowired private EbFixtures fx;
  @Autowired private TrackedItemService service;
  @Autowired private TrackedItemQuery query;
  @Autowired private ItemFollowUpJob job;
  @Autowired private EbTrackedItemRepository items;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private AsUser as;

  private EbTrackedItem open(
      EbProgramme p, String type, String member, LocalDate due, String mail) {
    return as.run(
        AO,
        () ->
            service.open(
                fx.company(),
                p.getId(),
                new ItemInput(
                    type,
                    null,
                    new EbTrackedItem.Details(
                        "Card of " + member,
                        member,
                        null,
                        null,
                        EbResponsibleParty.INSURER,
                        "INS-MGIC",
                        mail,
                        due,
                        null))));
  }

  @Test
  void itemsAreOpenedFilteredAndMovedOn() {
    EbProgramme p = fx.programme(true, List.of(hmo(null, null)));
    String member = "EMP-" + EbFixtures.token();
    EbTrackedItem item = open(p, "HMO_CARD", member, LocalDate.now().minusDays(3), null);
    assertThatThrownBy(() -> open(p, "NOPE", member, LocalDate.now(), null))
        .isInstanceOf(RuntimeException.class);
    assertThatThrownBy(() -> open(p, "HMO_CARD", member, null, null))
        .extracting("code")
        .isEqualTo("EB_ITEM_DUE_REQUIRED");
    assertThatThrownBy(() -> open(p, "HMO_CARD", member, LocalDate.now(), "not-mail"))
        .extracting("code")
        .isEqualTo("EMAIL_ADDRESS_INVALID");

    List<ItemRow> byMember =
        query
            .search(
                fx.company(), new ItemCriteria(null, member, null, null, null, false, null), 0, 10)
            .content();
    assertThat(byMember).extracting(ItemRow::id).containsExactly(item.getId());
    assertThat(byMember.get(0).daysPastDue()).isGreaterThanOrEqualTo(2);
    assertThat(
            query
                .search(
                    fx.company(),
                    new ItemCriteria(
                        p.getId(),
                        null,
                        "HMO_CARD",
                        "INSURER",
                        "PENDING",
                        true,
                        p.getProgrammeNo()),
                    0,
                    10)
                .content())
        .hasSize(1);
    assertThat(query.overdue(fx.company())).isPositive();

    Long company = fx.company();
    assertThatThrownBy(
            () ->
                as.run(
                    AO,
                    () ->
                        service.change(
                            company,
                            item.getId(),
                            new StatusChange(Action.RECEIVE, null, null, null))))
        .extracting("code")
        .isEqualTo("EB_ITEM_RECEIVED_DATE");
    assertThatThrownBy(
            () ->
                as.run(
                    AO,
                    () ->
                        service.change(
                            company,
                            item.getId(),
                            new StatusChange(Action.RELEASE, null, null, null))))
        .extracting("code")
        .isEqualTo("EB_ITEM_NOT_RECEIVED");
    as.run(
        AO,
        () ->
            service.change(
                company,
                item.getId(),
                new StatusChange(Action.RECEIVE, LocalDate.now().minusDays(1), null, "Card in")));
    as.run(
        "ebproc",
        () ->
            service.change(
                company, item.getId(), new StatusChange(Action.RELEASE, null, null, null)));
    EbTrackedItem released = items.findById(item.getId()).orElseThrow();
    assertThat(released.getStatus()).isEqualTo(EbItemStatus.RELEASED);
    assertThat(released.getRemarks()).isEqualTo("Card in");
    assertThatThrownBy(
            () ->
                as.run(
                    AO,
                    () ->
                        service.update(
                            company,
                            item.getId(),
                            new EbTrackedItem.Details(
                                "x",
                                null,
                                null,
                                null,
                                EbResponsibleParty.CLIENT,
                                null,
                                null,
                                LocalDate.now(),
                                null))))
        .extracting("code")
        .isEqualTo("EB_ITEM_NOT_PENDING");

    EbTrackedItem other = open(p, "BILLING_INVOICE", member, LocalDate.now().plusDays(3), null);
    as.run(
        AO,
        () ->
            service.update(
                company,
                other.getId(),
                new EbTrackedItem.Details(
                    "Billing of additions",
                    member,
                    "EBM-1",
                    null,
                    EbResponsibleParty.INSURER,
                    "INS-LAC",
                    "billing@insurer.example",
                    LocalDate.now().plusDays(5),
                    null)));
    assertThatThrownBy(
            () ->
                as.run(
                    AO,
                    () ->
                        service.change(
                            company,
                            other.getId(),
                            new StatusChange(Action.CLOSE, null, null, null))))
        .extracting("code")
        .isEqualTo("EB_ITEM_RECEIVED_DATE");
    as.run(
        AO,
        () ->
            service.change(
                company,
                other.getId(),
                new StatusChange(Action.CLOSE, null, LocalDate.now(), "Billed")));
    assertThat(items.findById(other.getId()).orElseThrow().getStatus())
        .isEqualTo(EbItemStatus.CLOSED);
    assertThat(query.row(company, other.getId()).memberChangeRef()).isEqualTo("EBM-1");
  }

  @Test
  void pastDueItemsAreFollowedUpThenEscalated() {
    EbProgramme p = fx.programme(true, List.of(hmo(null, null)));
    LocalDate due = LocalDate.of(2034, 3, 1);
    EbTrackedItem item =
        open(p, "HMO_CARD", "EMP-" + EbFixtures.token(), due, "cards@insurer.example");
    EbTrackedItem viaInsurer = open(p, "CONTRACT", "EMP-" + EbFixtures.token(), due, null);

    job.execute(due.plusDays(2));
    assertThat(followUps(item)).isZero();

    // Five working days after 1-Mar-2034 (a Wednesday) is 8-Mar-2034.
    job.execute(LocalDate.of(2034, 3, 8));
    assertThat(followUps(item)).isEqualTo(1);
    assertThat(items.findById(viaInsurer.getId()).orElseThrow().getFollowUpsSent()).isEqualTo(1);
    job.execute(LocalDate.of(2034, 3, 9));
    assertThat(followUps(item)).isEqualTo(1);
    job.execute(LocalDate.of(2034, 3, 15));
    job.execute(LocalDate.of(2034, 3, 22));
    assertThat(followUps(item)).isEqualTo(3);
    assertThat(items.findById(item.getId()).orElseThrow().getEscalatedAt()).isNull();
    job.execute(LocalDate.of(2034, 3, 29));
    EbTrackedItem escalated = items.findById(item.getId()).orElseThrow();
    assertThat(escalated.getEscalatedAt()).isNotNull();
    assertThat(escalated.getFollowUpsSent()).isEqualTo(3);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from alt_alert where exception_code = 'EB_ITEM_OVERDUE' and dedup_key = ?",
                Long.class,
                "EB_ITEM_OVERDUE:" + item.getId()))
        .isEqualTo(1L);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from msg_outbound where purpose = 'EB_ITEM_FOLLOWUP'"
                    + " and recipients = 'cards@insurer.example' and entity_id = ?",
                Long.class,
                p.getId().toString()))
        .isEqualTo(3L);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from msg_notification where recipient = ? and title = 'Pending item escalated'"
                    + " and entity_id = ?",
                Long.class,
                AO,
                p.getId().toString()))
        .isEqualTo(2L);
    job.execute(LocalDate.of(2034, 4, 5));
    assertThat(followUps(item)).isEqualTo(3);
  }

  private int followUps(EbTrackedItem item) {
    return items.findById(item.getId()).orElseThrow().getFollowUpsSent();
  }
}
