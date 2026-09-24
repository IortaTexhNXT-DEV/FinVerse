package com.iortatechnxt.brokerverse.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.docgen.service.DocumentComposer;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec;
import com.iortatechnxt.brokerverse.docgen.service.SheetSpec;
import com.iortatechnxt.brokerverse.messaging.domain.MessageFile;
import com.iortatechnxt.brokerverse.messaging.domain.MessageStatus;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundAttachment;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundMessage;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundMessage.RecordLink;
import com.iortatechnxt.brokerverse.messaging.service.MailDispatchJob;
import com.iortatechnxt.brokerverse.messaging.service.MessageSearch;
import com.iortatechnxt.brokerverse.messaging.service.MessageService;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.messaging.service.OutboundEmail;
import com.iortatechnxt.brokerverse.messaging.service.OutboundEmail.Protection;
import com.iortatechnxt.brokerverse.messaging.service.QueuedEmail;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.system.domain.JobTrigger;
import com.iortatechnxt.brokerverse.system.service.JobRegistry;
import com.lowagie.text.pdf.PdfReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.List;
import org.apache.poi.poifs.crypt.Decryptor;
import org.apache.poi.poifs.crypt.EncryptionInfo;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

@IntegrationTest
class MessagingIT {

  private static final String PDF = "application/pdf";
  private static final String XLSX =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

  @Autowired private MessageService messages;
  @Autowired private NotificationService notifications;
  @Autowired private DocumentComposer composer;
  @Autowired private JobRegistry jobs;
  @Autowired private AsUser as;

  private byte[] pdf() {
    return composer.pdf(
        new DocumentSpec(
            "BDO Insurance and Reinsurance Brokers, Inc.",
            "E-policy",
            "ARN-TEST",
            List.of(
                new DocumentSpec.Fields(
                    "Policy", List.of(new DocumentSpec.Field("Policy no.", "MC-1"))),
                new DocumentSpec.Table(
                    "Items",
                    List.of("Item", "Amount"),
                    List.of(List.of("Car", "1,000.00")),
                    List.of(1)),
                new DocumentSpec.Text("Terms", "Para one.\n\nPara two.")),
            List.of("Prepared by", "Approved by"),
            "EPOLICY_EMAIL v1"));
  }

  @Test
  void protectedAttachmentsAreEncryptedAndThePasswordFollowsSeparately()
      throws IOException, GeneralSecurityException {
    byte[] xlsx = composer.xlsx(new SheetSpec("Slip", List.of("A", "B"), List.of(List.of("x", 1))));
    QueuedEmail queued =
        as.run(
            "epol",
            () ->
                messages.queueEmail(
                    new OutboundEmail(
                        null,
                        "EPOLICY",
                        List.of("juan@example.ph"),
                        List.of(),
                        "Your e-policy MC-1",
                        "Dear Juan, attached.",
                        List.of(
                            new MessageFile("MC-1.pdf", PDF, pdf()),
                            new MessageFile("slip.xlsx", XLSX, xlsx)),
                        new Protection("Secret#123", true, "Your password is given by your agent."),
                        new RecordLink("MessagingTest", "1", "ARN-TEST"))));
    assertThat(queued.passwordMessageId()).isNotNull();

    OutboundMessage mail = messages.get(queued.messageId());
    assertThat(mail.getStatus()).isEqualTo(MessageStatus.SENT);
    assertThat(mail.isSimulated()).isTrue();
    List<OutboundAttachment> files = messages.attachments(mail.getId());
    assertThat(files).allMatch(OutboundAttachment::isPasswordProtected);
    assertThatThrownBy(() -> new PdfReader(files.get(0).getContent()).close())
        .isInstanceOf(Exception.class);
    PdfReader opened =
        new PdfReader(files.get(0).getContent(), "Secret#123".getBytes(StandardCharsets.UTF_8));
    assertThat(opened.getNumberOfPages()).isEqualTo(1);
    opened.close();
    try (POIFSFileSystem fs =
        new POIFSFileSystem(new ByteArrayInputStream(files.get(1).getContent()))) {
      Decryptor d = Decryptor.getInstance(new EncryptionInfo(fs));
      assertThat(d.verifyPassword("wrong")).isFalse();
      assertThat(d.verifyPassword("Secret#123")).isTrue();
    }

    OutboundMessage password = messages.get(queued.passwordMessageId());
    assertThat(password.getPurpose()).isEqualTo(MessageService.PASSWORD_PURPOSE);
    assertThat(password.getPasswordForId()).isEqualTo(mail.getId());
    assertThat(password.getBody()).contains("Secret#123", "given by your agent");
    assertThat(messages.attachments(password.getId())).isEmpty();
    assertThat(messages.forRecord("MessagingTest", "1")).hasSize(2);
  }

  @Test
  void failedDeliveriesAreRetriedThenMarkedFailedAndCanBeResent() {
    QueuedEmail queued =
        as.run(
            "proc",
            () ->
                messages.queueEmail(
                    new OutboundEmail(
                        null,
                        "PLACEMENT",
                        List.of("underwriting@insurer.invalid"),
                        List.of("copy@example.ph"),
                        "Placement slip PL-1",
                        "Please place.",
                        List.of(),
                        null,
                        new RecordLink("MessagingTest", "2", "PL-1"))));
    OutboundMessage first = messages.get(queued.messageId());
    assertThat(first.getStatus()).isEqualTo(MessageStatus.QUEUED);
    assertThat(first.getAttempts()).isEqualTo(1);
    assertThat(first.getLastError()).contains("Mailbox unavailable");
    jobs.run(MailDispatchJob.JOB_NAME, JobTrigger.MANUAL);
    jobs.run(MailDispatchJob.JOB_NAME, JobTrigger.MANUAL);
    OutboundMessage failed = messages.get(queued.messageId());
    assertThat(failed.getStatus()).isEqualTo(MessageStatus.FAILED);
    assertThat(failed.getAttempts()).isEqualTo(3);
    assertThat(
            messages
                .search(
                    new MessageSearch(MessageStatus.FAILED, "PLACEMENT", "pl-1"),
                    Pageable.ofSize(10))
                .getContent())
        .extracting(OutboundMessage::getId)
        .contains(failed.getId());
    OutboundMessage retried = as.run("badmin", () -> messages.retry(failed.getId()));
    assertThat(messages.get(retried.getId()).getAttempts()).isEqualTo(1);
    assertThatThrownBy(() -> messages.retry(queued.messageId()))
        .extracting("code")
        .isEqualTo("MESSAGE_NOT_FAILED");
  }

  @Test
  void rejectsBadAddressesAndUnprotectableFiles() {
    assertThatThrownBy(() -> queue(List.of("not-an-address"), List.of(), null))
        .extracting("code")
        .isEqualTo("EMAIL_ADDRESS_INVALID");
    assertThatThrownBy(() -> queue(List.of(" "), List.of(), null))
        .extracting("code")
        .isEqualTo("EMAIL_RECIPIENT_REQUIRED");
    assertThatThrownBy(
            () ->
                queue(
                    List.of("a@example.ph"),
                    List.of(
                        new MessageFile(
                            "notes.csv", "text/csv", "a,b".getBytes(StandardCharsets.UTF_8))),
                    new Protection(null, false, null)))
        .extracting("code")
        .isEqualTo("DOCUMENT_NOT_PROTECTABLE");
  }

  private QueuedEmail queue(List<String> to, List<MessageFile> files, Protection protection) {
    return as.run(
        "proc",
        () ->
            messages.queueEmail(
                new OutboundEmail(
                    null, "TEST", to, null, "Subject", "Body", files, protection, null)));
  }

  @Test
  void notificationsArePerUserAndCanBeMarkedRead() {
    as.run(
        "badmin",
        () ->
            notifications.notifyPermission(
                "EPOLICY_SEND",
                new Notice("E-policies ready", "3 to send", "/issuance", null, null)));
    long unread = as.run("epol", () -> notifications.unreadCount());
    assertThat(unread).isPositive();
    var mine = as.run("epol", () -> notifications.mine(true, Pageable.ofSize(5))).getContent();
    assertThat(mine.get(0).getTitle()).isEqualTo("E-policies ready");
    assertThatThrownBy(() -> as.run("proc", () -> notifications.markRead(mine.get(0).getId())))
        .hasMessageContaining("Notification");
    as.run("epol", () -> notifications.markRead(mine.get(0).getId()));
    assertThat(as.run("epol", () -> notifications.unreadCount())).isEqualTo(unread - 1);
    as.run("epol", () -> notifications.markAllRead());
    assertThat(as.run("epol", () -> notifications.unreadCount())).isZero();
  }
}
