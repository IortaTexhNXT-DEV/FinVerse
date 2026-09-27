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
 * The persona menus of BRD-10, BRD-11 and BRD-7 (client requirement 14), shared with the web
 * client: the file {@code frontend/src/navigation/personaMenus.json} lists, per suite and role, the
 * permissions the database grants and the menu screens the role must see. The web client checks the
 * menu against it; the backend tests check the grants and the screens' endpoints.
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
   * @param seedUser SIT/UAT user holding only this role
   * @param permissionScope prefix of the listed permissions (a role of another business area whose
   *     other grants the suite does not pin), null when every permission is listed
   * @param permissions permissions granted (those of the scope when a scope is set)
   * @param screens menu screens of the suite's sections
   */
  public record Persona(
      String role,
      String seedUser,
      String permissionScope,
      Set<String> permissions,
      List<String> screens) {

    /**
     * The permissions of a set this persona pins.
     *
     * @param granted permissions held
     * @return those of the scope (all of them without a scope)
     */
    public Set<String> inScope(Set<String> granted) {
      Set<String> result = new TreeSet<>(granted);
      if (permissionScope != null) {
        result.removeIf(p -> !p.startsWith(permissionScope));
      }
      return result;
    }
  }

  /**
   * Reads the file.
   *
   * @return personas of every suite by role code, in file order
   */
  public static Map<String, Persona> load() {
    try {
      JsonNode suites = new ObjectMapper().readTree(Files.readString(FILE)).get("suites");
      Map<String, Persona> result = new LinkedHashMap<>();
      for (JsonNode suite : suites) {
        for (Map.Entry<String, JsonNode> e : suite.get("roles").properties()) {
          result.put(e.getKey(), persona(e.getKey(), e.getValue()));
        }
      }
      return result;
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot read " + FILE.toAbsolutePath(), e);
    }
  }

  private static Persona persona(String role, JsonNode node) {
    Set<String> permissions = new TreeSet<>();
    node.get("permissions").forEach(p -> permissions.add(p.asText()));
    List<String> screens = new ArrayList<>();
    node.get("screens").forEach(s -> screens.add(s.asText()));
    JsonNode scope = node.get("permissionScope");
    return new Persona(
        role,
        node.get("seedUser").asText(),
        scope == null ? null : scope.asText(),
        permissions,
        List.copyOf(screens));
  }
}
