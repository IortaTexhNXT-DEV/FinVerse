package com.iortatechnxt.brokerverse.system.service;

import com.iortatechnxt.brokerverse.system.domain.ProductModule;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Which product modules use each permission, read from the {@code @PreAuthorize} checks of the
 * controllers. A permission used only by the controllers of switched-off modules grants nothing
 * (see {@link ProductModules#isPermissionActive}); a permission any platform controller checks
 * stays active. Built once, on first use.
 */
@Component
public class ModulePermissionIndex {

  /** Module key of the platform (a usage outside every product module). */
  static final String PLATFORM = "";

  private static final Pattern CODE = Pattern.compile("'([A-Z][A-Z0-9_]+)'");

  private final ObjectProvider<RequestMappingHandlerMapping> mappings;
  private volatile Map<String, Set<String>> usage;

  /**
   * Creates the index.
   *
   * @param mappings request mappings of the web layer (absent outside a web application)
   */
  public ModulePermissionIndex(ObjectProvider<RequestMappingHandlerMapping> mappings) {
    this.mappings = mappings;
  }

  /**
   * The modules whose controllers check a permission.
   *
   * @param permission permission code
   * @return module codes; {@value #PLATFORM} stands for a platform controller; empty when no
   *     controller checks the permission
   */
  public Set<String> modulesUsing(String permission) {
    return usage().getOrDefault(permission, Set.of());
  }

  /**
   * Every permission any controller checks.
   *
   * @return permission codes
   */
  public Set<String> permissions() {
    return usage().keySet();
  }

  private Map<String, Set<String>> usage() {
    Map<String, Set<String>> current = usage;
    if (current == null) {
      current = build();
      usage = current;
    }
    return current;
  }

  private Map<String, Set<String>> build() {
    Map<String, Set<String>> result = new HashMap<>();
    mappings
        .orderedStream()
        .forEach(
            mapping ->
                mapping.getHandlerMethods().values().forEach(handler -> record(handler, result)));
    Map<String, Set<String>> frozen = new HashMap<>();
    result.forEach((k, v) -> frozen.put(k, Set.copyOf(v)));
    return Collections.unmodifiableMap(frozen);
  }

  private static void record(HandlerMethod handler, Map<String, Set<String>> result) {
    Class<?> type = handler.getBeanType();
    String module = ProductModule.ofClass(type).map(Enum::name).orElse(PLATFORM);
    Set<String> codes = new HashSet<>();
    collect(AnnotatedElementUtils.findMergedAnnotation(type, PreAuthorize.class), codes);
    collect(
        AnnotatedElementUtils.findMergedAnnotation(handler.getMethod(), PreAuthorize.class), codes);
    codes.forEach(code -> result.computeIfAbsent(code, k -> new HashSet<>()).add(module));
  }

  private static void collect(PreAuthorize annotation, Set<String> codes) {
    if (annotation == null) {
      return;
    }
    Matcher m = CODE.matcher(annotation.value());
    while (m.find()) {
      codes.add(m.group(1));
    }
  }
}
