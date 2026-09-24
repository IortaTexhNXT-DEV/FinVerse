package com.iortatechnxt.brokerverse;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

/**
 * Architecture rules, enforced on every build.
 *
 * <ul>
 *   <li>Modules (top-level packages) are free of dependency cycles.
 *   <li>Layers: api -&gt; service -&gt; domain; domain never depends on service or api.
 *   <li>Controllers live in {@code ..api..}; services never depend on controllers.
 *   <li>Background work is a {@code system.service.ManagedJob} (job monitor, run history, failure
 *       alert), never a {@code @Scheduled} method (developer guide section 10.3).
 * </ul>
 */
@AnalyzeClasses(
    packages = "com.iortatechnxt.brokerverse",
    importOptions = ImportOption.DoNotIncludeTests.class)
final class ArchitectureTest {

  @ArchTest
  static final ArchRule MODULES_ARE_FREE_OF_CYCLES =
      slices().matching("com.iortatechnxt.brokerverse.(*)..").should().beFreeOfCycles();

  @ArchTest
  static final ArchRule DOMAIN_DOES_NOT_DEPEND_ON_UPPER_LAYERS =
      noClasses()
          .that()
          .resideInAPackage("..domain..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..service..", "..api..");

  @ArchTest
  static final ArchRule SERVICES_DO_NOT_DEPEND_ON_CONTROLLERS =
      noClasses()
          .that()
          .resideInAPackage("..service..")
          .should()
          .dependOnClassesThat()
          .areAnnotatedWith(RestController.class);

  @ArchTest
  static final ArchRule CONTROLLERS_LIVE_IN_API_PACKAGES =
      classes().that().areAnnotatedWith(RestController.class).should().resideInAPackage("..api..");

  @ArchTest
  static final ArchRule SERVICES_LIVE_IN_SERVICE_PACKAGES =
      classes()
          .that()
          .areAnnotatedWith(Service.class)
          .should()
          .resideInAnyPackage("..service..", "..core..", "..gl..", "..sequence..");

  @ArchTest
  static final ArchRule BACKGROUND_WORK_IS_A_MANAGED_JOB =
      noMethods()
          .should()
          .beAnnotatedWith(Scheduled.class)
          .because("background work must be a ManagedJob shown in the job monitor");

  private ArchitectureTest() {}
}
