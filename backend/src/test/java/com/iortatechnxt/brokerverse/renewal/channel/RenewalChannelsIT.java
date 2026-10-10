package com.iortatechnxt.brokerverse.renewal.channel;

import static com.iortatechnxt.brokerverse.renewal.RenewalFixtures.ADMIN;
import static com.iortatechnxt.brokerverse.renewal.RenewalFixtures.PO;
import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService;
import com.iortatechnxt.brokerverse.renewal.RenewalFixtures;
import com.iortatechnxt.brokerverse.renewal.channel.domain.ChannelMessage;
import com.iortatechnxt.brokerverse.renewal.channel.domain.ChannelStatus;
import com.iortatechnxt.brokerverse.renewal.channel.service.ChannelGateways;
import com.iortatechnxt.brokerverse.renewal.channel.service.ChannelMonitor;
import com.iortatechnxt.brokerverse.renewal.channel.service.ChannelService;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The CCM channel against its simulator (FRRN.017.02, FRRN.022.02, FRRN.023.02): a document
 * submitted with its transaction reference and then sent and delivered, an invalid recipient
 * refused with the client's message, an outage kept pending and retried, a resend of the same
 * document, the delivery history and the error report.
 */
@IntegrationTest
class RenewalChannelsIT {

  @Autowired private RenewalFixtures fx;
  @Autowired private ChannelService channels;
  @Autowired private ChannelMonitor monitor;
  @Autowired private DocumentService documents;
  @Autowired private SystemParameterService parameters;
  @Autowired private AsUser as;

  private ChannelService.Outbound outbound(RenewalCandidate c, String to) {
    Long file =
        as.run(
            PO,
            () ->
                documents
                    .upload(
                        new AttachmentTarget(RenewalCodes.ENTITY, c.getId().toString()),
                        List.of(new DocumentService.UploadedFile("RA.pdf", pdf())),
                        new DocumentService.UploadOptions(
                            RenewalCodes.DOC_RENEWAL_ADVICE, false, "RA", "RA", null))
                    .get(0)
                    .getId());
    return new ChannelService.Outbound(
        ChannelGateways.CCM,
        new ChannelMessage.Document(
            "RA", "RA-TEST-" + c.getId(), c.getId(), c.getRenewalRef(), "MTR_RA.pdf", file),
        new ChannelMessage.Address(to, null, "Renewal Advice", "Please find attached", null));
  }

  private static byte[] pdf() {
    String text = "%PDF-1.4\n1 0 obj<<>>endobj\ntrailer<<>>\n%%EOF";
    return text.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
  }

  @Test
  void aDocumentIsSubmittedToCcmThenSentAndDeliveredWithItsHistory() {
    RenewalCandidate c = fx.extractedMotor();
    ChannelService.Sent sent =
        as.run(PO, () -> channels.send(fx.company(), outbound(c, "juan@example.ph")));
    assertThat(sent.error()).isNull();
    ChannelMessage m = sent.message();
    assertThat(m.getStatus()).isEqualTo(ChannelStatus.SUBMITTED);
    assertThat(m.getExternalRef()).startsWith("CCMSIM-");
    as.run(PO, () -> channels.refresh());
    as.run(PO, () -> channels.refresh());
    ChannelMessage after = as.run(PO, () -> channels.get(fx.company(), m.getMessageNo()));
    assertThat(after.getStatus()).isEqualTo(ChannelStatus.DELIVERED);
    assertThat(as.run(PO, () -> channels.history(fx.company(), m.getMessageNo())))
        .extracting(e -> e.getStatus())
        .containsSubsequence(
            ChannelStatus.PENDING_TRANSMISSION,
            ChannelStatus.SUBMITTED,
            ChannelStatus.SENT,
            ChannelStatus.DELIVERED);

    ChannelService.Sent again = as.run(PO, () -> channels.resend(fx.company(), m.getMessageNo()));
    assertThat(again.message().getMessageNo()).isNotEqualTo(m.getMessageNo());
    assertThat(again.message().getAttachmentId()).isEqualTo(m.getAttachmentId());
  }

  @Test
  void anInvalidRecipientIsRefusedAndListedInTheErrorReport() {
    RenewalCandidate c = fx.extractedMotor();
    ChannelService.Sent sent =
        as.run(PO, () -> channels.send(fx.company(), outbound(c, "not-an-address")));
    assertThat(sent.error())
        .isEqualTo("Unable to send the file. One or more recipient email addresses are invalid.");
    assertThat(sent.message().getStatus()).isEqualTo(ChannelStatus.FAILED);
    assertThat(
            as.run(
                PO,
                () ->
                    monitor.list(
                        fx.company(),
                        new ChannelMonitor.Filter("CCM", "FAILED", null, c.getRenewalRef()))))
        .hasSize(1);
    assertThat(as.run(PO, () -> monitor.errorReport(fx.company(), "CCM")).content()).isNotEmpty();
  }

  @Test
  void aCcmOutageKeepsTheDocumentPendingUntilTheRetry() {
    RenewalCandidate c = fx.extractedMotor();
    as.run(ADMIN, () -> parameters.update(ChannelGateways.OUTAGE, "CCM"));
    ChannelService.Sent sent;
    try {
      sent = as.run(PO, () -> channels.send(fx.company(), outbound(c, "juan@example.ph")));
      assertThat(sent.error())
          .isEqualTo("Unable to send the file. CCM service is currently unavailable.");
      assertThat(sent.message().getStatus()).isEqualTo(ChannelStatus.PENDING_TRANSMISSION);
      assertThat(as.run(PO, () -> channels.check("CCM")).reachable()).isFalse();
    } finally {
      as.run(ADMIN, () -> parameters.update(ChannelGateways.OUTAGE, ""));
    }
    as.run(PO, () -> channels.retry());
    assertThat(
            as.run(PO, () -> channels.get(fx.company(), sent.message().getMessageNo())).getStatus())
        .isEqualTo(ChannelStatus.SUBMITTED);
    assertThat(as.run(PO, () -> monitor.settings("CCM")).mode()).isEqualTo("SIMULATOR");
  }
}
