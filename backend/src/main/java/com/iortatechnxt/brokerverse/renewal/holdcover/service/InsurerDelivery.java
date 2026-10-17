package com.iortatechnxt.brokerverse.renewal.holdcover.service;

import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfile;
import com.iortatechnxt.brokerverse.catalog.domain.PlacementChannel;
import com.iortatechnxt.brokerverse.catalog.service.InsurerService;
import com.iortatechnxt.brokerverse.messaging.domain.MessageFile;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundMessage.RecordLink;
import com.iortatechnxt.brokerverse.messaging.service.MessageService;
import com.iortatechnxt.brokerverse.messaging.service.OutboundEmail;
import com.iortatechnxt.brokerverse.renewal.channel.domain.ChannelMessage;
import com.iortatechnxt.brokerverse.renewal.channel.service.ChannelGateways;
import com.iortatechnxt.brokerverse.renewal.channel.service.ChannelService;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * The delivery of a file to an insurer (FRRN.029.03, FRRN.036.06): an insurer enrolled in MFT (its
 * placement channel is a file transfer, or it is listed in {@value #ENROLLED}) receives the file in
 * its MFT location, any other insurer by e-mail to its placement mailboxes with the file attached.
 * The file is kept on the renewal accounts it concerns.
 */
@Component
public class InsurerDelivery {

  /** Parameter: insurers enrolled in MFT. */
  public static final String ENROLLED = "RNW_MFT_ENROLLED_INSURERS";

  private static final String XLSX =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

  private final InsurerService insurers;
  private final SystemParameterService parameters;
  private final ChannelService channels;
  private final MessageService messages;
  private final DocumentService documents;

  /**
   * Creates the delivery.
   *
   * @param insurers insurer maintenance (channel and mailboxes)
   * @param parameters MFT enrolment
   * @param channels MFT
   * @param messages e-mail
   * @param documents stored files
   */
  public InsurerDelivery(
      InsurerService insurers,
      SystemParameterService parameters,
      ChannelService channels,
      MessageService messages,
      DocumentService documents) {
    this.insurers = insurers;
    this.parameters = parameters;
    this.channels = channels;
    this.messages = messages;
    this.documents = documents;
  }

  /**
   * Whether an insurer is enrolled in MFT.
   *
   * @param companyId company
   * @param insurerCode insurer
   * @return true for MFT
   */
  public boolean mft(Long companyId, String insurerCode) {
    boolean listed =
        parameters.items(ENROLLED).stream().anyMatch(c -> c.strip().equals(insurerCode));
    return listed
        || insurers.requireInsurer(companyId, insurerCode).getPlacementChannel()
            == PlacementChannel.SFTP;
  }

  /**
   * Stores a file on the renewal accounts it concerns and sends it to the insurer.
   *
   * @param companyId company
   * @param insurerCode insurer
   * @param file file
   * @param about renewal accounts of the file, the first holding it
   * @param kind kind of document (HOLD_COVER, PLACEMENT, ...)
   * @return how it was sent and the message
   */
  public Sent send(
      Long companyId,
      String insurerCode,
      MessageFile file,
      List<RenewalCandidate> about,
      String kind) {
    RenewalCandidate first = about.get(0);
    Long attachment =
        documents
            .upload(
                new AttachmentTarget(RenewalCodes.ENTITY, first.getId().toString()),
                List.of(new DocumentService.UploadedFile(file.fileName(), file.content())),
                new DocumentService.UploadOptions(
                    RenewalCodes.DOC_RENEWAL_LETTER, false, kind, file.fileName(), null))
            .get(0)
            .getId();
    List<AttachmentTarget> others = new ArrayList<>();
    about.stream()
        .skip(1)
        .forEach(c -> others.add(new AttachmentTarget(RenewalCodes.ENTITY, c.getId().toString())));
    if (!others.isEmpty()) {
      documents.link(attachment, others);
    }
    String subject = kind.replace('_', ' ') + " - " + file.fileName();
    if (mft(companyId, insurerCode)) {
      ChannelService.Sent sent =
          channels.send(
              companyId,
              new ChannelService.Outbound(
                  ChannelGateways.MFT,
                  new ChannelMessage.Document(
                      kind,
                      file.fileName(),
                      first.getId(),
                      first.getRenewalRef(),
                      file.fileName(),
                      attachment),
                  new ChannelMessage.Address(insurerCode, null, subject, null, null)));
      return new Sent(ChannelGateways.MFT, sent.message().getMessageNo(), sent.error());
    }
    InsurerProfile insurer = insurers.requireInsurer(companyId, insurerCode);
    var queued =
        messages.queueEmail(
            new OutboundEmail(
                companyId,
                kind,
                insurer.getPlacementEmailList(),
                List.of(),
                subject,
                "Please find attached " + file.fileName() + ".",
                List.of(new MessageFile(file.fileName(), contentType(file), file.content())),
                null,
                new RecordLink(
                    RenewalCodes.ENTITY, first.getId().toString(), first.getRenewalRef())));
    return new Sent("EMAIL", String.valueOf(queued.messageId()), null);
  }

  private static String contentType(MessageFile file) {
    return file.mimeType() == null ? XLSX : file.mimeType();
  }

  /**
   * How a file was sent.
   *
   * @param channel MFT or EMAIL
   * @param message message number
   * @param error error of a refused MFT transfer, null when sent
   */
  public record Sent(String channel, String message, String error) {}
}
