package com.iortatechnxt.brokerverse.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Permissions are consistent across the server, the migrations and the web client:
 *
 * <ul>
 *   <li>every permission a {@code @PreAuthorize} check uses is a {@link Permission} and is granted
 *       to at least one role by a migration (seed data does not count), or is listed below as
 *       unassigned by design with the reason;
 *   <li>every permission code the web client checks (screen permissions and {@code can(...)}) is a
 *       {@link Permission} that the server checks too (a {@code @PreAuthorize}, a report, a bulk
 *       upload, a workflow or a programmatic check).
 * </ul>
 */
final class PermissionConsistencyTest {

  private static final Path BACKEND = Path.of("").toAbsolutePath();
  private static final Path MAIN_JAVA = BACKEND.resolve("src/main/java");
  private static final Path MIGRATIONS = BACKEND.resolve("src/main/resources/db/migration");
  private static final Path FRONTEND = BACKEND.getParent().resolve("frontend/src");

  /**
   * Permissions no delivered role holds, on purpose. Each is granted through a User Access request
   * when the business needs it.
   */
  private static final Map<String, String> UNASSIGNED_BY_DESIGN = Map.of();

  private static final Pattern AUTHORITY_EXPRESSION =
      Pattern.compile("\"[^\"]*(?:hasAuthority|hasAnyAuthority)\\([^\"]*\"");
  private static final Pattern QUOTED_CODE = Pattern.compile("'([A-Z][A-Z0-9_]+)'");
  private static final Pattern FRONTEND_CODES =
      Pattern.compile(
          "(?:permission:\\s*|can\\(\\s*)'([A-Z][A-Z0-9_]+)'"
              + "|(?:alsoPermissions|requiresAll):\\s*\\[([^\\]]*)\\]");

  private static Set<String> permissionNames() {
    return Arrays.stream(Permission.values()).map(Enum::name).collect(Collectors.toSet());
  }

  private static String read(Path p) {
    try {
      return Files.readString(p);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static Stream<Path> files(Path root, String suffix) {
    try {
      return Files.walk(root).filter(p -> p.toString().endsWith(suffix)).toList().stream();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static Set<String> preAuthorizePermissions() {
    Set<String> codes = new TreeSet<>();
    files(MAIN_JAVA, ".java")
        .map(PermissionConsistencyTest::read)
        .forEach(
            src -> {
              Matcher expr = AUTHORITY_EXPRESSION.matcher(src);
              while (expr.find()) {
                Matcher code = QUOTED_CODE.matcher(expr.group());
                while (code.find()) {
                  codes.add(code.group(1));
                }
              }
            });
    return codes;
  }

  /** Quoted codes of the migration statements that write role grants. */
  private static Set<String> grantedByMigrations() {
    Set<String> codes = new TreeSet<>();
    files(MIGRATIONS, ".sql")
        .map(PermissionConsistencyTest::read)
        .map(sql -> sql.replaceAll("--[^\n]*", ""))
        .flatMap(sql -> Arrays.stream(sql.split(";")))
        .filter(st -> st.toLowerCase(Locale.ROOT).contains("insert into sec_role_permission"))
        .forEach(
            st -> {
              Matcher code = QUOTED_CODE.matcher(st);
              while (code.find()) {
                codes.add(code.group(1));
              }
            });
    return codes;
  }

  private static Set<String> frontendPermissions() {
    Set<String> codes = new TreeSet<>();
    files(FRONTEND, ".ts")
        .filter(p -> !p.toString().contains(".test."))
        .forEach(p -> collectFrontend(read(p), codes));
    files(FRONTEND, ".tsx")
        .filter(p -> !p.toString().contains(".test."))
        .forEach(p -> collectFrontend(read(p), codes));
    return codes;
  }

  private static void collectFrontend(String src, Set<String> codes) {
    Matcher m = FRONTEND_CODES.matcher(src);
    while (m.find()) {
      if (m.group(1) != null) {
        codes.add(m.group(1));
      } else {
        Matcher code = QUOTED_CODE.matcher(m.group(2));
        while (code.find()) {
          codes.add(code.group(1));
        }
      }
    }
  }

  /** Main code outside the permission enum, where the server checks a permission. */
  private static String serverCode() {
    return files(MAIN_JAVA, ".java")
        .filter(p -> !p.endsWith("Permission.java"))
        .map(PermissionConsistencyTest::read)
        .collect(Collectors.joining("\n"));
  }

  @Test
  void everyCheckedPermissionIsGrantedOrUnassignedByDesign() {
    Set<String> checked = preAuthorizePermissions();
    assertThat(checked).hasSizeGreaterThan(100);
    assertThat(checked)
        .as("checked permissions that are not permissions")
        .isSubsetOf(permissionNames());
    Set<String> granted = grantedByMigrations();
    Set<String> ungranted = new TreeSet<>(checked);
    ungranted.removeAll(granted);
    ungranted.removeIf(p -> Permission.valueOf(p).isInsurerOnly());
    ungranted.removeAll(UNASSIGNED_BY_DESIGN.keySet());
    assertThat(ungranted).as("permissions checked on the server but granted to no role").isEmpty();
  }

  @Test
  void everyWebClientPermissionIsCheckedByTheServer() {
    Set<String> used = frontendPermissions();
    assertThat(used).hasSizeGreaterThan(100);
    assertThat(used)
        .as("web client permissions unknown to the server")
        .isSubsetOf(permissionNames());
    String server = serverCode();
    Set<String> unchecked =
        used.stream()
            .filter(
                p ->
                    !server.contains("'" + p + "'")
                        && !server.contains("\"" + p + "\"")
                        && !server.contains("Permission." + p))
            .collect(Collectors.toCollection(TreeSet::new));
    assertThat(unchecked).as("web client permissions the server never checks").isEmpty();
  }
}
