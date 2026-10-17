package com.iortatechnxt.brokerverse.report.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Report parameters whose values come from a platform list are chosen by name, in business terms.
 */
class ParameterLookupsTest {

  @Test
  void offersListParametersAsAChoiceByName() {
    ParameterSpec module =
        ParameterLookups.refine(
            ParameterSpec.optional("area", "Module (area code)", ParameterType.TEXT));
    assertThat(module.type()).isEqualTo(ParameterType.LOOKUP);
    assertThat(module.label()).isEqualTo("Module");
    assertThat(module.options()).isEqualTo(List.of(PlatformCodeSets.MODULE));

    ParameterSpec profile =
        ParameterLookups.refine(
            ParameterSpec.optional("groupProfile", "Group Profile (code)", ParameterType.TEXT));
    assertThat(profile.label()).isEqualTo("Group Profile");
    assertThat(profile.options()).isEqualTo(List.of(PlatformCodeSets.GROUP_PROFILE));

    ParameterSpec insurer =
        ParameterLookups.refine(
            ParameterSpec.required("insurer", "Insurer Code", ParameterType.TEXT));
    assertThat(insurer.label()).isEqualTo("Insurer");
    assertThat(insurer.required()).isTrue();
    assertThat(
            ParameterLookups.refine(
                    ParameterSpec.optional("agent", "Agent (user ID)", ParameterType.TEXT))
                .label())
        .isEqualTo("Agent");
  }

  @Test
  void leavesOtherParametersAsDeclared() {
    ParameterSpec date = ParameterSpec.required("fromDate", "From Date", ParameterType.DATE);
    assertThat(ParameterLookups.refine(date)).isSameAs(date);
    ParameterSpec batch = ParameterSpec.optional("batchNo", "Batch No.", ParameterType.TEXT);
    assertThat(ParameterLookups.refine(batch)).isSameAs(batch);
  }

  @Test
  void describesReportsInBusinessWords() {
    assertThat(
            ParameterLookups.businessDescription(
                "Post-dated cheques issued in a period, by paying bank (FPD004)"))
        .isEqualTo("Post-dated cheques issued in a period, by paying bank");
    assertThat(ParameterLookups.businessDescription("Requests with their status (MKT 1.18.1)"))
        .isEqualTo("Requests with their status");
    assertThat(ParameterLookups.businessDescription("Post-dated cheques received (due_to_bank)"))
        .isEqualTo("Post-dated cheques received (due to bank)");
    assertThat(ParameterLookups.businessDescription("Claims per handler (90 by default)"))
        .isEqualTo("Claims per handler (90 by default)");
  }
}
