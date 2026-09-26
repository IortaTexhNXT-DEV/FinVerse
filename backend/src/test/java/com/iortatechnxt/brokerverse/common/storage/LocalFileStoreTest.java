package com.iortatechnxt.brokerverse.common.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.common.util.Sha256;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalFileStoreTest {

  private static final byte[] PDF = "%PDF-1.4 local".getBytes(StandardCharsets.US_ASCII);
  private static final String PDF_TYPE = "application/pdf";

  @TempDir private Path root;

  private MutableClock clock;
  private LocalFileStore store;

  @BeforeEach
  void setUp() {
    clock = new MutableClock(Instant.parse("2026-09-26T08:00:00Z"));
    store = new LocalFileStore(properties(root, "secret-one", null), clock);
  }

  private static StorageProperties properties(Path root, String secret, String scan) {
    return new StorageProperties(
        "local",
        null,
        null,
        false,
        null,
        null,
        new StorageProperties.Local(root, secret, scan),
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  private static ObjectRef ref(String key) {
    return new ObjectRef(BucketClass.DOCUMENTS, key);
  }

  @Test
  void putGetMetadataCopyListAndDelete() {
    ObjectRef a = ref("bdoi/claim/2026/09/" + UUID.randomUUID());
    StoredObject stored = store.put(a, PDF, PDF_TYPE, Sha256.hex(PDF));
    assertThat(stored.versionId()).isNotBlank();
    assertThat(store.provider()).isEqualTo("local");
    assertThat(store.exists(a)).isTrue();
    assertThat(store.get(a)).isEqualTo(PDF);
    ObjectMetadata meta = store.metadata(a).orElseThrow();
    assertThat(meta.size()).isEqualTo(PDF.length);
    assertThat(meta.contentType()).isEqualTo(PDF_TYPE);
    assertThat(meta.tag("GuardDutyMalwareScanStatus")).contains("NO_THREATS_FOUND");
    assertThat(meta.legalHold()).isFalse();

    ObjectRef b = a.withPrefix(ObjectKeys.QUARANTINE_PREFIX);
    store.copy(a, b);
    assertThat(store.get(b)).isEqualTo(PDF);
    List<String> keys = new ArrayList<>();
    store.list(BucketClass.DOCUMENTS, "bdoi/", o -> keys.add(o.ref().key()));
    assertThat(keys).containsExactly(a.key());
    store.list(BucketClass.REPORTS, "", o -> keys.add(o.ref().key()));
    assertThat(keys).hasSize(1);

    store.delete(a);
    assertThat(store.exists(a)).isFalse();
    assertThat(store.metadata(a)).isEmpty();
    assertThatThrownBy(() -> store.get(a)).isInstanceOf(FileStoreException.class);
  }

  @Test
  void refusesChecksumMismatchAndProtectsHeldObjects() {
    ObjectRef a = ref("bdoi/claim/2026/09/held");
    assertThatThrownBy(() -> store.put(a, PDF, PDF_TYPE, Sha256.hex("other")))
        .isInstanceOf(FileStoreException.class)
        .hasMessageContaining("SHA-256");
    store.put(a, PDF, PDF_TYPE, Sha256.hex(PDF));
    store.legalHold(a, true);
    assertThat(store.metadata(a).orElseThrow().legalHold()).isTrue();
    assertThatThrownBy(() -> store.delete(a)).hasMessageContaining("legal hold");
    assertThatThrownBy(() -> store.put(a, PDF, PDF_TYPE, Sha256.hex(PDF)))
        .hasMessageContaining("legal hold");
    store.legalHold(a, false);
    store.delete(a);
    assertThat(store.exists(a)).isFalse();
    assertThatThrownBy(() -> store.legalHold(a, true)).isInstanceOf(FileStoreException.class);
  }

  @Test
  void scanResultCanBeSetAndCleared() {
    LocalFileStore pending = new LocalFileStore(properties(root, null, "SCAN_PENDING"), clock);
    ObjectRef a = ref("shared/x/2026/09/scan");
    pending.put(a, PDF, PDF_TYPE, Sha256.hex(PDF));
    assertThat(pending.metadata(a).orElseThrow().tag("GuardDutyMalwareScanStatus"))
        .contains("SCAN_PENDING");
    pending.markScanResult(a, null);
    assertThat(pending.metadata(a).orElseThrow().tags()).isEmpty();
    pending.markScanResult(a, "THREATS_FOUND");
    assertThat(pending.metadata(a).orElseThrow().tag("GuardDutyMalwareScanStatus"))
        .contains("THREATS_FOUND");
  }

  @Test
  void getLinksExpireAndCannotBeTamperedWith() {
    ObjectRef a = ref("bdoi/claim/2026/09/link");
    store.put(a, PDF, PDF_TYPE, Sha256.hex(PDF));
    PresignedLink link = store.presignedGet(a, Duration.ofSeconds(300), "Résumé.pdf", PDF_TYPE);
    assertThat(link.method()).isEqualTo("GET");
    assertThat(link.expiresAt()).isEqualTo(clock.instant().plusSeconds(300));
    assertThat(link.url().toString()).startsWith(LocalFileStore.CONTENT_PATH + "?token=");
    String token = link.url().getQuery().substring("token=".length());

    LocalFileStore.LinkedContent content = store.readLinked(token);
    assertThat(content.content()).isEqualTo(PDF);
    assertThat(content.fileName()).isEqualTo("Résumé.pdf");
    assertThat(content.contentType()).isEqualTo(PDF_TYPE);

    assertThatThrownBy(() -> store.writeLinked(token, PDF)).hasMessageContaining("not valid for");
    String tampered = token.substring(0, token.length() - 2) + "xx";
    assertThatThrownBy(() -> store.readLinked(tampered)).hasMessageContaining("Invalid link");
    assertThatThrownBy(() -> store.readLinked("abc")).hasMessageContaining("Invalid link");
    LocalFileStore other = new LocalFileStore(properties(root, "secret-two", null), clock);
    assertThatThrownBy(() -> other.readLinked(token)).hasMessageContaining("Invalid link");

    clock.advance(Duration.ofSeconds(301));
    assertThatThrownBy(() -> store.readLinked(token)).hasMessageContaining("expired");
  }

  @Test
  void putLinksStoreOnlyTheAnnouncedContent() {
    ObjectRef a = ref("incoming/bdoi/bank-file/2026/09/put");
    PresignedLink link = store.presignedPut(a, Duration.ofMinutes(5), "text/csv", Sha256.hex(PDF));
    assertThat(link.method()).isEqualTo("PUT");
    assertThat(link.headers()).isEqualTo(Map.of("Content-Type", "text/csv"));
    String token = link.url().getQuery().substring("token=".length());
    assertThatThrownBy(() -> store.writeLinked(token, "other".getBytes(StandardCharsets.UTF_8)))
        .hasMessageContaining("SHA-256");
    store.writeLinked(token, PDF);
    assertThat(store.get(a)).isEqualTo(PDF);
    assertThat(store.metadata(a).orElseThrow().contentType()).isEqualTo("text/csv");
  }

  @Test
  void keysAreValidatedAndCarryNoPersonalData() {
    assertThatThrownBy(() -> ref("../etc/passwd")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> ref("a//b")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> ref("a/b/")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> ref("Juan Dela Cruz.pdf"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> ref(null)).isInstanceOf(IllegalArgumentException.class);
    UUID id = UUID.fromString("0b6f0f47-4a55-4c55-9f55-6d7f0c0d0e0f");
    String key =
        ObjectKeys.newKey("BDOI", "BrokerClaim", Instant.parse("2026-03-05T00:00:00Z"), id);
    assertThat(key).isEqualTo("bdoi/broker-claim/2026/03/" + id);
    assertThat(ObjectKeys.segment("  ")).isEqualTo("x");
    assertThat(ObjectKeys.segment(null)).isEqualTo("x");
    assertThat(ObjectKeys.segment("A".repeat(60))).hasSize(40);
    assertThat(ObjectRef.isValidKey(key)).isTrue();
  }

  /** A clock that tests can move forward. */
  static final class MutableClock extends Clock {

    private Instant now;

    MutableClock(Instant now) {
      this.now = now;
    }

    void advance(Duration duration) {
      now = now.plus(duration);
    }

    @Override
    public ZoneOffset getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(java.time.ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return now;
    }
  }
}
