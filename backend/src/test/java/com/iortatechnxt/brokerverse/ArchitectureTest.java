package com.iortatechnxt.brokerverse;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
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
 *   <li>Business dates come from {@link BusinessClock} (business zone, default Asia/Manila): no
 *       {@code LocalDate.now}, {@code LocalDateTime.now}, {@code YearMonth.now} or {@code Year.now}
 *       elsewhere, as those take the date of the clock's zone (UTC).
 * </ul>
 *
 * <p>A plain JUnit Jupiter test (not the ArchUnit engine), so it runs in the alphabetical class
 * order of the build, before the Spring test contexts accumulate, and its class graph is released
 * afterwards.
 */
final class ArchitectureTest {

  private static JavaClasses classes;

  @BeforeAll
  static void importClasses() {
    classes =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.iortatechnxt.brokerverse");
  }

  @AfterAll
  static void releaseClasses() {
    classes = null;
  }

  private static final ArchRule MODULES_ARE_FREE_OF_CYCLES =
      slices().matching("com.iortatechnxt.brokerverse.(*)..").should().beFreeOfCycles();

  private static final ArchRule DOMAIN_DOES_NOT_DEPEND_ON_UPPER_LAYERS =
      noClasses()
          .that()
          .resideInAPackage("..domain..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..service..", "..api..");

  private static final ArchRule SERVICES_DO_NOT_DEPEND_ON_CONTROLLERS =
      noClasses()
          .that()
          .resideInAPackage("..service..")
          .should()
          .dependOnClassesThat()
          .areAnnotatedWith(RestController.class);

  private static final ArchRule CONTROLLERS_LIVE_IN_API_PACKAGES =
      classes().that().areAnnotatedWith(RestController.class).should().resideInAPackage("..api..");

  private static final ArchRule SERVICES_LIVE_IN_SERVICE_PACKAGES =
      classes()
          .that()
          .areAnnotatedWith(Service.class)
          .should()
          .resideInAnyPackage("..service..", "..core..", "..gl..", "..sequence..");

  private static final ArchRule BACKGROUND_WORK_IS_A_MANAGED_JOB =
      noMethods()
          .should()
          .beAnnotatedWith(Scheduled.class)
          .because("background work must be a ManagedJob shown in the job monitor");

  private static final Set<String> DATE_TYPES =
      Set.of(
          LocalDate.class.getName(),
          LocalDateTime.class.getName(),
          YearMonth.class.getName(),
          Year.class.getName());

  private static final DescribedPredicate<JavaMethodCall> CURRENT_DATE_CALL =
      DescribedPredicate.describe(
          "LocalDate/LocalDateTime/YearMonth/Year.now",
          call ->
              "now".equals(call.getName()) && DATE_TYPES.contains(call.getTargetOwner().getName()));

  private static final ArchRule BUSINESS_DATES_COME_FROM_THE_BUSINESS_CLOCK =
      noClasses()
          .that()
          .doNotBelongToAnyOf(BusinessClock.class)
          .should()
          .callMethodWhere(CURRENT_DATE_CALL)
          .because(
              "the business date is taken in the business zone by BusinessClock, not in the"
                  + " zone of the injected clock (UTC)");

  @Test
  void modulesAreFreeOfCycles() {
    MODULES_ARE_FREE_OF_CYCLES.check(classes);
  }

  @Test
  void domainDoesNotDependOnUpperLayers() {
    DOMAIN_DOES_NOT_DEPEND_ON_UPPER_LAYERS.check(classes);
  }

  @Test
  void servicesDoNotDependOnControllers() {
    SERVICES_DO_NOT_DEPEND_ON_CONTROLLERS.check(classes);
  }

  @Test
  void controllersLiveInApiPackages() {
    CONTROLLERS_LIVE_IN_API_PACKAGES.check(classes);
  }

  @Test
  void servicesLiveInServicePackages() {
    SERVICES_LIVE_IN_SERVICE_PACKAGES.check(classes);
  }

  @Test
  void backgroundWorkIsAManagedJob() {
    BACKGROUND_WORK_IS_A_MANAGED_JOB.check(classes);
  }

  @Test
  void businessDatesComeFromTheBusinessClock() {
    BUSINESS_DATES_COME_FROM_THE_BUSINESS_CLOCK.check(classes);
  }
}
