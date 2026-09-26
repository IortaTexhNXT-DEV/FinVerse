package com.iortatechnxt.brokerverse.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * The persona menus of BRD-10 and BRD-11 (client requirement 14), shared with the web client: the
 * file {@code frontend/src/navigation/personaMenus.json} lists, per role, the permissions the
 * database grants and the menu screens the role must see. The web client checks the menu against
 * it; the backend tests check the grants and the screens' endpoints.
 */
public final class PersonaMenus {

  /** Location of the shared file, from the backend module directory. */
  public static final Path FILE =
      Path.of("..", "frontend", "src", "navigation", "personaMenus.json");

  private PersonaMenus() {}

  /**
   * One role of the file.
   *
   * @param role role code
   * @param demoUser demo user holding only this role
   * @param permissions permissions granted
   * @param screens menu screens of the three sections
   */
  public record Persona(
      String role, String demoUser, Set<String> permissions, List<String> screens) {}

  /**
   * Reads the file.
   *
   * @return personas by role code, in file order
   */
  public static Map<String, Persona> load() {
    try {
      JsonNode roles = new ObjectMapper().readTree(Files.readString(FILE)).get("roles");
      Map<String, Persona> result = new LinkedHashMap<>();
      for (Map.Entry<String, JsonNode> e : roles.properties()) {
        Set<String> permissions = new TreeSet<>();
        e.getValue().get("permissions").forEach(p -> permissions.add(p.asText()));
        List<String> screens = new ArrayList<>();
        e.getValue().get("screens").forEach(s -> screens.add(s.asText()));
        result.put(
            e.getKey(),
            new Persona(
                e.getKey(),
                e.getValue().get("demoUser").asText(),
                permissions,
                List.copyOf(screens)));
      }
      return result;
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot read " + FILE.toAbsolutePath(), e);
    }
  }
}
