package com.iortatechnxt.brokerverse.system.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import java.util.Map;
import java.util.TreeMap;

/**
 * A named set of module switches (for example the profile of an insurance broker, which does not
 * use the insurer suite). Applying a profile requests the changes of every module whose switch
 * differs; each change takes effect on approval like a single switch.
 */
@Entity
@Table(name = "sys_module_profile")
public class ModuleProfile extends BaseEntity {

  @Column(nullable = false, length = 40, unique = true, updatable = false)
  private String code;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(nullable = false, length = 400)
  private String description;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
      name = "sys_module_profile_module",
      joinColumns = @JoinColumn(name = "profile_id"))
  @MapKeyColumn(name = "module_code", length = 40)
  @Column(name = "enabled", nullable = false)
  private Map<String, Boolean> modules = new TreeMap<>();

  protected ModuleProfile() {}

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  /**
   * Module switches of the profile; a module not listed is on.
   *
   * @return state per module code
   */
  public Map<String, Boolean> getModules() {
    return Map.copyOf(modules);
  }

  /**
   * Whether the profile has a module on.
   *
   * @param moduleCode module code
   * @return true unless the profile lists the module as off
   */
  public boolean hasOn(String moduleCode) {
    return modules.getOrDefault(moduleCode, Boolean.TRUE);
  }
}
