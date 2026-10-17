package com.iortatechnxt.brokerverse.productmaint.service;

import com.iortatechnxt.brokerverse.productmaint.domain.MasterInboxFile;
import com.iortatechnxt.brokerverse.productmaint.domain.MasterInboxFileRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The delivery target of the product master changes: the simulated receiving system when {@code
 * brokerverse.product-master.simulator} is on (SIT and UAT, refused in production), otherwise the
 * folder {@code brokerverse.product-master.folder} the receiving systems read; the file is written
 * under a temporary name and renamed so that a reader never sees half a file.
 */
@Configuration(proxyBeanMethods = false)
public class MasterTargets {

  /**
   * The target of the configuration.
   *
   * @param simulator whether the receiving system is simulated
   * @param folder folder of the receiving systems, blank until connected
   * @param inbox simulated receiving system
   * @param clock clock
   * @return target
   */
  @Bean
  public MasterTarget productMasterTarget(
      @Value("${brokerverse.product-master.simulator:false}") boolean simulator,
      @Value("${brokerverse.product-master.folder:}") String folder,
      MasterInboxFileRepository inbox,
      Clock clock) {
    if (simulator) {
      return new SimulatedTarget(inbox, clock);
    }
    return new FolderTarget(folder);
  }

  /** The simulated receiving system of SIT and UAT. */
  static final class SimulatedTarget implements MasterTarget {

    private final MasterInboxFileRepository inbox;
    private final Clock clock;

    SimulatedTarget(MasterInboxFileRepository inbox, Clock clock) {
      this.inbox = inbox;
      this.clock = clock;
    }

    @Override
    public String name() {
      return "Receiving system simulator";
    }

    @Override
    public boolean connected() {
      return true;
    }

    @Override
    public void deliver(String fileName, String content, int records) {
      inbox.save(new MasterInboxFile(fileName, content, records, clock.instant()));
    }
  }

  /** The folder that the receiving systems read. */
  static final class FolderTarget implements MasterTarget {

    private final String folder;

    FolderTarget(String folder) {
      this.folder = folder == null ? "" : folder.strip();
    }

    @Override
    public String name() {
      return folder.isEmpty()
          ? "Not connected (connection details from the bank's IT at SIT)"
          : folder;
    }

    @Override
    public boolean connected() {
      return !folder.isEmpty();
    }

    @Override
    public void deliver(String fileName, String content, int records) {
      try {
        Path dir = Path.of(folder);
        Files.createDirectories(dir);
        Path part = dir.resolve(fileName + ".part");
        Files.writeString(part, content, StandardCharsets.UTF_8);
        Files.move(
            part,
            dir.resolve(fileName),
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.ATOMIC_MOVE);
      } catch (IOException | RuntimeException e) {
        throw new MasterTarget.MasterTransferException(
            "The file could not be written to " + folder + ": " + e.getMessage(), e);
      }
    }
  }
}
