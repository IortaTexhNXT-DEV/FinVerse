package com.iortatechnxt.brokerverse.renewal.mft.api;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.renewal.channel.service.ChannelGateways;
import com.iortatechnxt.brokerverse.renewal.mft.service.MftInbox;
import com.iortatechnxt.brokerverse.renewal.mft.service.MftInboxJob;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * The MFT inbound folders on the Channel Monitor: in SIT and UAT (MFT simulator) a file of an
 * insurer is placed in a folder as the MFT service would; the folders can be read at once instead
 * of waiting for the next run of {@value MftInboxJob#JOB_NAME}.
 */
@RestController
@RequestMapping("/api/v1/renewal/channels/mft-inbox")
public class MftInboxController {

  private static final String SETUP = "hasAuthority('RNW_CHANNEL_SETUP')";

  private final MftInbox inbox;
  private final ChannelGateways gateways;

  /**
   * Creates the controller.
   *
   * @param inbox inbound folders
   * @param gateways MFT mode
   */
  public MftInboxController(MftInbox inbox, ChannelGateways gateways) {
    this.inbox = inbox;
    this.gateways = gateways;
  }

  /**
   * Places a file of an insurer in an inbound folder (MFT simulator only).
   *
   * @param kind placement-response, hold-cover-response or epolicy
   * @param file file
   * @return the file name placed
   */
  @PostMapping
  @PreAuthorize(SETUP)
  public Map<String, String> place(
      @RequestParam String kind, @RequestPart("file") MultipartFile file) {
    if (gateways.of(ChannelGateways.MFT).live()) {
      throw new BusinessRuleException(
          "RNW_MFT_LIVE", "Files are placed by the MFT service when the live interface is on");
    }
    try {
      inbox.place(kind, file.getOriginalFilename(), file.getBytes());
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
    return Map.of("fileName", String.valueOf(file.getOriginalFilename()), "kind", kind);
  }

  /**
   * Reads the inbound folders now.
   *
   * @return files processed
   */
  @PostMapping("/poll")
  @PreAuthorize(SETUP)
  public Map<String, Integer> poll() {
    return Map.of("files", inbox.poll());
  }
}
