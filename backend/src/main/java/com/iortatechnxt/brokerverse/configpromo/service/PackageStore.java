package com.iortatechnxt.brokerverse.configpromo.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.common.util.Sha256;
import com.iortatechnxt.brokerverse.configpromo.domain.PackageKind;
import com.iortatechnxt.brokerverse.configpromo.domain.PromotionPackage;
import com.iortatechnxt.brokerverse.configpromo.domain.PromotionPackage.PackageFacts;
import com.iortatechnxt.brokerverse.configpromo.domain.PromotionPackage.PackageFile;
import com.iortatechnxt.brokerverse.configpromo.domain.PromotionPackageRepository;
import com.iortatechnxt.brokerverse.configpromo.engine.CanonicalJson;
import com.iortatechnxt.brokerverse.configpromo.engine.ConfigPackage;
import com.iortatechnxt.brokerverse.configpromo.engine.PackageCodec;
import com.iortatechnxt.brokerverse.configpromo.engine.PackageException;
import com.iortatechnxt.brokerverse.configpromo.engine.PackageManifest;
import com.iortatechnxt.brokerverse.storage.domain.FileOrigin;
import com.iortatechnxt.brokerverse.storage.domain.FileOwner;
import com.iortatechnxt.brokerverse.storage.domain.StoredFile;
import com.iortatechnxt.brokerverse.storage.service.StoredFileService;
import com.iortatechnxt.brokerverse.storage.service.StoredFileService.StoreRequest;
import java.time.Clock;
import org.springframework.stereotype.Component;

/**
 * Keeps packages: the file in the file store (record class {@code CONFIG_PACKAGE}, owner type
 * {@value #OWNER_TYPE}) and the package record; opens a kept package again, verifying its
 * signature.
 */
@Component
public class PackageStore {

  /** Owner type of the package files. */
  public static final String OWNER_TYPE = "ConfigPackage";

  /** Record class of the package files. */
  public static final String RECORD_CLASS = "CONFIG_PACKAGE";

  private static final String ZIP = "application/zip";

  private final StoredFileService files;
  private final PromotionPackageRepository packages;
  private final DocumentNumberService numbers;
  private final PromotionSettings settings;
  private final Clock clock;

  /**
   * Creates the store.
   *
   * @param files file store
   * @param packages package records
   * @param numbers document numbers
   * @param settings settings (signer)
   * @param clock clock
   */
  public PackageStore(
      StoredFileService files,
      PromotionPackageRepository packages,
      DocumentNumberService numbers,
      PromotionSettings settings,
      Clock clock) {
    this.files = files;
    this.packages = packages;
    this.numbers = numbers;
    this.settings = settings;
    this.clock = clock;
  }

  /**
   * Keeps a package (in the caller's transaction).
   *
   * @param kind origin
   * @param manifest its manifest
   * @param content zip bytes
   * @param keyId signing key id
   * @return package record
   */
  public PromotionPackage keep(
      PackageKind kind, PackageManifest manifest, byte[] content, String keyId) {
    String no = numbers.next("CFP-" + BusinessClock.today(clock).getYear());
    String fileName = no + "_" + manifest.sourceEnvironment() + "_configuration.zip";
    StoredFile stored =
        files.storeChecked(
            new StoreRequest(
                new FileOwner(null, OWNER_TYPE, manifest.packageId()),
                RECORD_CLASS,
                RECORD_CLASS,
                fileName,
                content,
                null),
            ZIP,
            kind == PackageKind.UPLOAD ? FileOrigin.UPLOADED : FileOrigin.GENERATED);
    return packages.save(
        new PromotionPackage(
            no,
            kind,
            new PackageFacts(
                manifest.packageId(),
                manifest.mode(),
                manifest.sourceEnvironment(),
                manifest.platformVersion(),
                manifest.schemaVersion(),
                manifest.includeUsers(),
                manifest.datasets().size(),
                manifest.totalRows(),
                manifest.description(),
                CanonicalJson.text(manifest),
                keyId),
            new PackageFile(stored.getId(), fileName, content.length, Sha256.hex(content))));
  }

  /**
   * A package record.
   *
   * @param id id
   * @return package
   */
  public PromotionPackage get(Long id) {
    return packages
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Configuration package", id));
  }

  /**
   * Opens a kept package: reads its file and verifies it.
   *
   * @param pkg package record
   * @return verified package
   */
  public ConfigPackage open(PromotionPackage pkg) {
    return verify(files.read(pkg.getStoredFileId()));
  }

  /**
   * Verifies the bytes of a package.
   *
   * @param content zip bytes
   * @return verified package
   */
  public ConfigPackage verify(byte[] content) {
    try {
      return PackageCodec.read(content, settings.signer(), settings.maxPackageBytes());
    } catch (PackageException e) {
      throw new BusinessRuleException("CONFIG_PACKAGE_INVALID", e.getMessage(), e);
    }
  }
}
