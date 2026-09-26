package com.iortatechnxt.brokerverse.lov;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import com.iortatechnxt.brokerverse.lov.domain.LovDetails;
import com.iortatechnxt.brokerverse.lov.domain.LovValue;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class LovServiceIT {

  private static final LocalDate TODAY = LocalDate.now();

  @Autowired private LovService lovs;
  @Autowired private AsUser as;

  @Test
  void newValueIsUsableOnlyAfterAuthorizationAndWithinEffectivity() {
    LovValue created =
        as.run(
            "badmin",
            () ->
                lovs.create(
                    "RETURN_REASON",
                    "LOV_IT_" + System.nanoTime() % 100000,
                    new LovDetails("Test reason", 5, null, TODAY, null)));
    assertThat(created.getRecordStatus()).isEqualTo(RecordStatus.PENDING_AUTHORIZATION);
    assertThatThrownBy(() -> lovs.requireValid("RETURN_REASON", created.getCode(), TODAY))
        .extracting("code")
        .isEqualTo("LOV_VALUE_INVALID");
    assertThatThrownBy(() -> as.run("badmin", () -> lovs.authorize(created.getId())))
        .extracting("code")
        .isEqualTo("MAKER_CHECKER_VIOLATION");

    as.run("approver", () -> lovs.authorize(created.getId()));
    assertThat(lovs.requireValid("RETURN_REASON", created.getCode(), TODAY).getLabel())
        .isEqualTo("Test reason");
    assertThat(lovs.activeValues("RETURN_REASON", TODAY))
        .extracting(LovValue::getCode)
        .contains(created.getCode());

    // End-dating: a change of effectivity must be authorized again.
    as.run(
        "badmin",
        () ->
            lovs.update(
                created.getId(),
                new LovDetails("Test reason", 5, null, TODAY, TODAY.plusDays(10))));
    as.run("approver", () -> lovs.authorize(created.getId()));
    assertThat(lovs.get(created.getId()).isUsableOn(TODAY.plusDays(11))).isFalse();
    assertThat(lovs.get(created.getId()).isUsableOn(TODAY.plusDays(10))).isTrue();

    as.run("badmin", () -> lovs.deactivate(created.getId()));
    assertThat(lovs.activeValues("RETURN_REASON", TODAY))
        .extracting(LovValue::getCode)
        .doesNotContain(created.getCode());
    assertThat(lovs.label("RETURN_REASON", created.getCode())).isEqualTo("Test reason");
  }

  @Test
  void rejectsDuplicatesInvalidDatesAndOptionalBlank() {
    assertThatThrownBy(
            () ->
                as.run(
                    "badmin",
                    () ->
                        lovs.create(
                            "RETURN_REASON",
                            "OTHERS",
                            new LovDetails("Dup", 1, null, TODAY, null))))
        .hasMessageContaining("OTHERS");
    assertThatThrownBy(
            () ->
                as.run(
                    "badmin",
                    () ->
                        lovs.create(
                            "RETURN_REASON",
                            "BAD_DATES_" + System.nanoTime() % 100000,
                            new LovDetails("Bad", 1, null, TODAY, TODAY.minusDays(1)))))
        .extracting("code")
        .isEqualTo("LOV_EFFECTIVITY_INVALID");
    lovs.validateOptional("RETURN_REASON", " ", TODAY);
    assertThat(lovs.label("RETURN_REASON", null)).isNull();
    assertThat(lovs.label("RETURN_REASON", "UNKNOWN_CODE")).isEqualTo("UNKNOWN_CODE");
    assertThat(lovs.types())
        .extracting("code")
        .contains("RETURN_REASON", "DOCUMENT_TYPE", "ID_TYPE");
  }
}
