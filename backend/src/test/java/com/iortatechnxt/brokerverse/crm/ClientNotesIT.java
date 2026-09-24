package com.iortatechnxt.brokerverse.crm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.domain.ClientInstruction;
import com.iortatechnxt.brokerverse.crm.domain.InstructionContent;
import com.iortatechnxt.brokerverse.crm.domain.NoteHistory;
import com.iortatechnxt.brokerverse.crm.service.ClientBanner;
import com.iortatechnxt.brokerverse.crm.service.ClientNotesService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class ClientNotesIT {

  @Autowired private CrmFixtures fx;
  @Autowired private ClientNotesService notes;
  @Autowired private AsUser as;

  @Test
  void tagsAndInstructionsShowOnTheBannerAndKeepHistory() {
    Client c = fx.prospect(CrmFixtures.person());
    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    as.run("ao", () -> notes.addTag(c.getId(), "VIP"));
    assertThatThrownBy(() -> as.run("ao", () -> notes.addTag(c.getId(), "VIP")))
        .extracting("code")
        .isEqualTo("CLIENT_TAG_EXISTS");
    assertThatThrownBy(() -> as.run("ao", () -> notes.addTag(c.getId(), "NO_SUCH_TAG")))
        .extracting("code")
        .isEqualTo("LOV_VALUE_INVALID");

    ClientInstruction current =
        as.run(
            "ao",
            () ->
                notes.addInstruction(
                    c.getId(),
                    new InstructionContent("BILLING", "Bill the parent company", today, null)));
    ClientInstruction future =
        as.run(
            "ao",
            () ->
                notes.addInstruction(
                    c.getId(),
                    new InstructionContent(
                        "COMMUNICATION", "Call after 5 pm", today.plusDays(10), null)));

    ClientBanner banner = notes.banner(c.getId());
    assertThat(banner.tags()).extracting(ClientBanner.Tag::label).containsExactly("VIP client");
    assertThat(banner.instructions())
        .extracting(ClientBanner.Instruction::id)
        .containsExactly(current.getId());

    as.run(
        "ao2",
        () ->
            notes.changeInstruction(
                c.getId(),
                future.getId(),
                new InstructionContent(
                    "COMMUNICATION", "Call after 6 pm", today, today.plusDays(5))));
    assertThat(notes.banner(c.getId()).instructions()).hasSize(2);
    assertThatThrownBy(
            () ->
                as.run(
                    "ao",
                    () ->
                        notes.changeInstruction(
                            c.getId(),
                            future.getId(),
                            new InstructionContent(
                                "COMMUNICATION", "x", today, today.minusDays(1)))))
        .extracting("code")
        .isEqualTo("INSTRUCTION_DATES");

    as.run("ao", () -> notes.endInstruction(c.getId(), current.getId()));
    as.run(
        "ao",
        () -> {
          notes.removeTag(c.getId(), "VIP");
          return null;
        });
    ClientBanner after = notes.banner(c.getId());
    assertThat(after.tags()).isEmpty();
    assertThat(after.instructions())
        .extracting(ClientBanner.Instruction::id)
        .containsExactly(future.getId());
    assertThat(notes.instructions(c.getId())).hasSize(2);

    assertThat(notes.history(c.getId()))
        .extracting(NoteHistory::getAction)
        .containsExactly("REMOVED", "REMOVED", "CHANGED", "ADDED", "ADDED", "ADDED");
    NoteHistory change = notes.history(c.getId()).get(2);
    assertThat(change.getActor()).isEqualTo("ao2");
    assertThat(change.getFromValue()).contains("Call after 5 pm");
    assertThat(change.getToValue()).contains("Call after 6 pm");
  }
}
