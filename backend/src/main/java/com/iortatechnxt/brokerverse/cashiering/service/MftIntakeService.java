package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.cashiering.domain.ChannelFile;
import com.iortatechnxt.brokerverse.cashiering.domain.ChannelProfile;
import com.iortatechnxt.brokerverse.cashiering.domain.ChannelProfileRepository;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Files received via MFT (FRS.CSH.05.01.04): at the times agreed with BDOI IT, the files placed in
 * the MFT folder of each payment file type whose MFT switch is on (Bills Payment files of OBPCS,
 * matured post-dated checks of PMS) are taken and processed like uploaded files; a file is then
 * moved to the sub-folder {@code processed} or, when a file check refuses it, {@code refused}, and
 * the refusal is alerted to the Cashiering Team Leader. The folder and the switch are settings of
 * Cashiering Setup; the MFT connection itself is configured at BDOI's site.
 */
@Service
public class MftIntakeService {

  /** User recorded as the uploader of the files received via MFT. */
  public static final String MFT_USER = "MFT";

  private final ChannelProfileRepository profiles;
  private final ChannelFileService channelFiles;
  private final OrganizationService organization;
  private final SystemParameterService parameters;
  private final Path root;

  /**
   * Creates the intake.
   *
   * @param profiles profiles of the file types (MFT folder and switch)
   * @param channelFiles intake of a file
   * @param organization companies
   * @param parameters company of the MFT files
   * @param root root of the MFT folders
   */
  public MftIntakeService(
      ChannelProfileRepository profiles,
      ChannelFileService channelFiles,
      OrganizationService organization,
      SystemParameterService parameters,
      @Value("${brokerverse.cashiering.mft-root:${java.io.tmpdir}/bibs-mft}") String root) {
    this.profiles = profiles;
    this.channelFiles = channelFiles;
    this.organization = organization;
    this.parameters = parameters;
    this.root = Path.of(root).toAbsolutePath().normalize();
  }

  /**
   * Takes the files waiting in the MFT folders and processes them.
   *
   * @return the files received
   */
  public List<ChannelFile> poll() {
    Authentication previous = SecurityContextHolder.getContext().getAuthentication();
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                MFT_USER,
                null,
                List.of(new SimpleGrantedAuthority(PaymentFileHandler.PERMISSION))));
    try {
      Long companyId = company();
      List<ChannelFile> received = new ArrayList<>();
      for (ChannelProfile profile : profiles.findAll()) {
        if (profile.isMftEnabled() && profile.getMftFolder() != null) {
          received.addAll(take(companyId, profile));
        }
      }
      return received;
    } finally {
      SecurityContextHolder.getContext().setAuthentication(previous);
    }
  }

  /**
   * The folder of a file type under the MFT root.
   *
   * @param profile profile
   * @return folder
   */
  public Path folder(ChannelProfile profile) {
    Path folder = root.resolve(profile.getMftFolder()).normalize();
    if (!folder.startsWith(root)) {
      throw new BusinessRuleException(
          "MFT_FOLDER_OUTSIDE",
          "The MFT folder of " + profile.getName() + " is outside the MFT root");
    }
    return folder;
  }

  private List<ChannelFile> take(Long companyId, ChannelProfile profile) {
    Path folder = folder(profile);
    List<ChannelFile> received = new ArrayList<>();
    if (!Files.isDirectory(folder)) {
      return received;
    }
    try (Stream<Path> waiting = Files.list(folder)) {
      for (Path file : waiting.filter(Files::isRegularFile).sorted().toList()) {
        Path name = file.getFileName();
        String fileName = name == null ? file.toString() : name.toString();
        ChannelFile done =
            channelFiles.receive(
                companyId, profile.getFileType(), fileName, Files.readAllBytes(file), "MFT");
        received.add(done);
        Path target =
            folder.resolve(ChannelFile.REFUSED.equals(done.getStatus()) ? "refused" : "processed");
        Files.createDirectories(target);
        Files.move(file, target.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (IOException ex) {
      throw new UncheckedIOException("MFT folder " + folder + " cannot be read", ex);
    }
    return received;
  }

  private Long company() {
    String code = parameters.text("CASH_MFT_COMPANY_CODE", "").strip();
    List<Company> companies = organization.listCompanies();
    return companies.stream()
        .filter(c -> code.isEmpty() || code.equals(c.getCode()))
        .findFirst()
        .orElseThrow(
            () -> new BusinessRuleException("MFT_NO_COMPANY", "No company receives the MFT files"))
        .getId();
  }
}
