package com.iortatechnxt.brokerverse;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.security.DataScopeMappings;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
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
 *   <li>Seed data (SIT/UAT) is loaded only with the seed profile: every Spring bean of a {@code
 *       seed} package carries {@code @Profile("seed")} (with {@code "test"} where tests use it),
 *       and seed beans live in {@code seed} packages.
 *   <li>Business dates come from {@link BusinessClock} (the configured business zone): no {@code
 *       now} of LocalDate, LocalDateTime, LocalTime, ZonedDateTime, OffsetDateTime, Instant,
 *       YearMonth or Year elsewhere, as those take the clock's zone (UTC) or bypass the injected
 *       clock; no {@code Clock.systemUTC()} outside the application configuration; no static {@code
 *       ZoneId} fields.
 *   <li>Every controller method that takes a company is checked against the user's data scope
 *       (DATA_SCOPE_DESIGN.md): by the interceptor ({@code companyId} query parameter or path
 *       variable), by the body advice (a body with {@code companyId}), or explicitly ({@code
 *       CompanyScoped}).
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
          LocalTime.class.getName(),
          ZonedDateTime.class.getName(),
          OffsetDateTime.class.getName(),
          Instant.class.getName(),
          YearMonth.class.getName(),
          Year.class.getName());

  private static final DescribedPredicate<JavaMethodCall> CURRENT_DATE_CALL =
      DescribedPredicate.describe(
          "LocalDate/LocalDateTime/LocalTime/ZonedDateTime/OffsetDateTime/Instant/YearMonth/Year.now",
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
                  + " zone of the injected clock (UTC), and the current instant comes from the"
                  + " injected clock");

  private static final ArchRule NO_CLOCK_OUTSIDE_THE_CONFIGURATION =
      noClasses()
          .that()
          .doNotHaveFullyQualifiedName("com.iortatechnxt.brokerverse.config.ApplicationConfig")
          .should()
          .callMethod(Clock.class, "systemUTC")
          .orShould()
          .callMethod(Clock.class, "systemDefaultZone")
          .because("the clock is injected, so tests control time");

  private static final ArchRule NO_ZONE_CONSTANTS =
      noFields()
          .that()
          .haveRawType(ZoneId.class)
          .should()
          .beStatic()
          .because(
              "the business zone is configured per deployment and read from BusinessClock.zone()"
                  + " where it is used, never copied into a constant")
          .allowEmptyShould(true);

  private static final DescribedPredicate<JavaClass> SPRING_BEAN =
      DescribedPredicate.describe(
          "Spring beans", c -> c.isMetaAnnotatedWith(Component.class) && !c.isAnnotation());

  private static final ArchCondition<JavaClass> SEED_PROFILE_ONLY =
      new ArchCondition<>("be annotated with @Profile of the seed profile (and the test profile)") {
        @Override
        public void check(JavaClass item, ConditionEvents events) {
          boolean ok =
              item.tryGetAnnotationOfType(Profile.class)
                  .map(p -> Set.of(p.value()))
                  .filter(v -> v.contains("seed") && Set.of("seed", "test").containsAll(v))
                  .isPresent();
          if (!ok) {
            events.add(
                SimpleConditionEvent.violated(
                    item, item.getName() + " is a seed bean without @Profile(\"seed\")"));
          }
        }
      };

  private static final ArchRule SEED_BEANS_CARRY_THE_SEED_PROFILE =
      classes()
          .that()
          .resideInAPackage("..seed..")
          .and(SPRING_BEAN)
          .should(SEED_PROFILE_ONLY)
          .because("seed data is loaded only in SIT/UAT databases, never in production");

  private static final ArchRule SEED_DATA_LIVES_IN_SEED_PACKAGES =
      classes()
          .that()
          .haveSimpleNameContaining("SeedData")
          .or()
          .haveSimpleNameStartingWith("Seed")
          .and(SPRING_BEAN)
          .should()
          .resideInAPackage("..seed..")
          .because("seed beans are kept apart from the production services");

  private static final ArchCondition<JavaMethod> CHECK_THE_DATA_SCOPE =
      new ArchCondition<>("check the company it takes against the data scope") {
        @Override
        public void check(JavaMethod item, ConditionEvents events) {
          if (!DataScopeMappings.covered(item.reflect())) {
            events.add(
                SimpleConditionEvent.violated(
                    item,
                    item.getFullName()
                        + " takes a company that neither the data scope interceptor nor the body"
                        + " advice reads: bind it as companyId or mark the method @CompanyScoped"
                        + " and call DataScope.requireCompany"));
          }
        }
      };

  private static final ArchRule COMPANY_ENDPOINTS_ARE_DATA_SCOPED =
      methods()
          .that()
          .areDeclaredInClassesThat()
          .areAnnotatedWith(RestController.class)
          .and()
          .arePublic()
          .should(CHECK_THE_DATA_SCOPE)
          .because("a user may act only for the companies and branches of his data scope");

  @Test
  void companyEndpointsAreDataScoped() {
    COMPANY_ENDPOINTS_ARE_DATA_SCOPED.check(classes);
  }

  @Test
  void seedBeansCarryTheSeedProfile() {
    SEED_BEANS_CARRY_THE_SEED_PROFILE.check(classes);
    SEED_DATA_LIVES_IN_SEED_PACKAGES.check(classes);
  }

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

  @Test
  void theClockIsInjected() {
    NO_CLOCK_OUTSIDE_THE_CONFIGURATION.check(classes);
  }

  @Test
  void theBusinessZoneIsNotCopiedIntoConstants() {
    NO_ZONE_CONSTANTS.check(classes);
  }
}
