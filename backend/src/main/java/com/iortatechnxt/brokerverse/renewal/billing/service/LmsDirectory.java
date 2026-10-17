package com.iortatechnxt.brokerverse.renewal.billing.service;

import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * The shared LMS directory of the billing files (FRRN.027): with {@value #MODE} LIVE a billing file
 * is written to the LMS directory agreed with BDOI IT; with SIMULATOR (SIT and UAT) to the
 * simulator directory; OFF leaves the upload out.
 */
@Component
public class LmsDirectory {

  /** Parameter: LIVE, SIMULATOR or OFF. */
  public static final String MODE = "RNW_LMS_MODE";

  /** Upload status: written. */
  public static final String UPLOADED = "UPLOADED";

  /** Upload status: the directory could not be written. */
  public static final String FAILED = "FAILED";

  /** Upload status: no upload. */
  public static final String OFF = "OFF";

  private final SystemParameterService parameters;
  private final Path live;
  private final Path simulator;

  /**
   * Creates the directory.
   *
   * @param parameters mode
   * @param live LMS directory of the live interface
   * @param simulator directory of the simulator
   */
  public LmsDirectory(
      SystemParameterService parameters,
      @Value("${brokerverse.renewal.lms-root:}") String live,
      @Value("${java.io.tmpdir}/bibs-lms-simulator") String simulator) {
    this.parameters = parameters;
    this.live = live.isBlank() ? null : Path.of(live).toAbsolutePath().normalize();
    this.simulator = Path.of(simulator).toAbsolutePath().normalize();
  }

  /**
   * Writes a billing file to the LMS directory.
   *
   * @param fileName file name
   * @param content content
   * @return UPLOADED, FAILED or OFF
   */
  public String upload(String fileName, byte[] content) {
    String mode = parameters.text(MODE, "SIMULATOR").strip();
    Path dir = "LIVE".equals(mode) ? live : simulator;
    if (OFF.equals(mode) || dir == null) {
      return OFF;
    }
    try {
      Files.createDirectories(dir);
      Path target = dir.resolve(fileName).normalize();
      if (!target.startsWith(dir)) {
        return FAILED;
      }
      Files.write(target, content);
      return UPLOADED;
    } catch (IOException ex) {
      return FAILED;
    }
  }
}
