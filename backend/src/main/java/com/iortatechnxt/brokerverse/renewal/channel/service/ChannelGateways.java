package com.iortatechnxt.brokerverse.renewal.channel.service;

import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import org.springframework.stereotype.Component;

/**
 * The connection of each channel by its mode setting: the live interface (LIVE) or the simulator of
 * SIT and UAT (SIMULATOR, the default until the IT team of the bank gives the connection details).
 */
@Component
public class ChannelGateways {

  /** Channel of the client communications. */
  public static final String CCM = "CCM";

  /** Channel of the files with the insurers. */
  public static final String MFT = "MFT";

  /** Parameter: channels the simulator reports unavailable (tests of an outage). */
  public static final String OUTAGE = "RNW_CHANNEL_SIMULATOR_OUTAGE";

  private final SystemParameterService parameters;
  private final CcmHttpGateway ccm;
  private final CcmSimulator ccmSimulator;
  private final MftHttpGateway mft;
  private final MftSimulator mftSimulator;

  /**
   * Creates the selector.
   *
   * @param parameters mode settings
   * @param ccm live CCM interface
   * @param ccmSimulator CCM simulator
   * @param mft live MFT interface
   * @param mftSimulator MFT simulator
   */
  public ChannelGateways(
      SystemParameterService parameters,
      CcmHttpGateway ccm,
      CcmSimulator ccmSimulator,
      MftHttpGateway mft,
      MftSimulator mftSimulator) {
    this.parameters = parameters;
    this.ccm = ccm;
    this.ccmSimulator = ccmSimulator;
    this.mft = mft;
    this.mftSimulator = mftSimulator;
  }

  /**
   * The connection of a channel.
   *
   * @param channel CCM or MFT
   * @return the live interface or the simulator
   */
  public ChannelGateway of(String channel) {
    boolean live = "LIVE".equals(parameters.text("RNW_" + channel + "_MODE", "SIMULATOR").strip());
    if (MFT.equals(channel)) {
      return live ? mft : mftSimulator;
    }
    return live ? ccm : ccmSimulator;
  }
}
