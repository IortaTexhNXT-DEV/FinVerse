package com.iortatechnxt.brokerverse.report.core;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.report.core.CodeSetSource.CodeOption;
import java.util.List;
import org.junit.jupiter.api.Test;

/** A list parameter prints its chosen entry by name in the report header. */
class LookupNamesTest {

  private static final ParameterSpec PROFILE =
      ParameterSpec.optional("groupProfile", "Group Profile (code)", ParameterType.TEXT);

  @Test
  void theNameOfTheChosenEntryIsPrinted() {
    assertThat(
            LookupNames.shown(
                PROFILE,
                "UAM_APPROVER",
                source -> {
                  assertThat(source).isEqualTo(PlatformCodeSets.GROUP_PROFILE);
                  return List.of(
                      new CodeOption("MKT_AO", "Marketing Account Officer"),
                      new CodeOption("UAM_APPROVER", "User Access Approver"));
                }))
        .isEqualTo("User Access Approver");
  }

  @Test
  void aValueOutsideTheListOrOfAPlainParameterPrintsAsEntered() {
    assertThat(LookupNames.shown(PROFILE, "GONE", source -> List.of())).isEqualTo("GONE");
    assertThat(
            LookupNames.shown(
                PROFILE,
                "X",
                source -> {
                  throw new IllegalStateException("no list");
                }))
        .isEqualTo("X");
    ParameterSpec batch = ParameterSpec.optional("batchNo", "Batch No.", ParameterType.TEXT);
    assertThat(LookupNames.shown(batch, "B-7", source -> List.of())).isEqualTo("B-7");
  }
}
