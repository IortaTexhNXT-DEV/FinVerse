package com.iortatechnxt.brokerverse;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.iortatechnxt.brokerverse.audit.api.RecordScopeAdvisor;
import com.iortatechnxt.brokerverse.common.security.CompanyScoped;
import com.iortatechnxt.brokerverse.common.security.GlobalRecord;
import com.iortatechnxt.brokerverse.common.security.OwnedBy;
import com.iortatechnxt.brokerverse.common.security.OwnedRecord;
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
import jakarta.persistence.Entity;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
 *   <li>Every endpoint that addresses a stored record by a numeric id in its path names the record
 *       ({@code OwnedRecord}, checked against the data scope before the endpoint runs, HTTP 404
 *       outside it), checks it explicitly ({@code CompanyScoped}), or is on the explicit allow list
 *       of programme-wide records ({@code GlobalRecord}); every named record belongs to a company
 *       (its own company, a parent record of {@code OwnedBy}, or a linked record).
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

  /** Text SQL run without bound parameters, or a sort taken unchecked into the query text. */
  private static final DescribedPredicate<JavaMethodCall> UNBOUND_SQL_CALL =
      DescribedPredicate.describe(
          "Statement.execute*/addBatch(String), Connection.createStatement or JpaSort.unsafe",
          call -> {
            String owner = call.getTargetOwner().getName();
            String name = call.getName();
            return switch (owner) {
              case "java.sql.Statement" -> name.startsWith("execute") || "addBatch".equals(name);
              case "java.sql.Connection" -> "createStatement".equals(name);
              case "org.springframework.data.jpa.domain.JpaSort" -> "unsafe".equals(name);
              default -> false;
            };
          });

  private static final ArchRule SQL_VALUES_ARE_BOUND_PARAMETERS =
      noClasses()
          .should()
          .callMethodWhere(UNBOUND_SQL_CALL)
          .because(
              "values reach SQL as bound parameters (PreparedStatement, JdbcTemplate arguments,"
                  + " named parameters) and sort keys through a whitelist, never as query text");

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

  private static final ArchCondition<JavaMethod> NAME_THE_RECORD_OF_ITS_ID =
      new ArchCondition<>("name the record a numeric id in its path addresses") {
        @Override
        public void check(JavaMethod item, ConditionEvents events) {
          boolean endpoint =
              item.getAnnotations().stream()
                  .anyMatch(a -> MAPPINGS.contains(a.getRawType().getName()));
          if (!endpoint
              || item.isAnnotatedWith(GlobalRecord.class)
              || item.getOwner().isAnnotatedWith(GlobalRecord.class)
              || item.isAnnotatedWith(CompanyScoped.class)) {
            return;
          }
          Method method = item.reflect();
          List<String> ids = recordIds(method);
          Map<String, OwnedRecord> declared =
              RecordScopeAdvisor.declarations(method.getDeclaringClass(), method);
          if (!ids.isEmpty() && ids.stream().noneMatch(declared::containsKey)) {
            events.add(
                SimpleConditionEvent.violated(
                    item,
                    item.getFullName()
                        + " addresses a record by "
                        + ids
                        + " without naming it: declare @OwnedRecord(<entity>.class, variable ="
                        + " ...) on the method or its controller, or @GlobalRecord(<reason>) for a"
                        + " programme-wide record"));
          }
        }
      };

  /**
   * The numeric path variables of an endpoint that address a record (the company itself is checked
   * by the data scope interceptor).
   *
   * @param method endpoint
   * @return path variable names
   */
  static List<String> recordIds(Method method) {
    List<String> ids = new ArrayList<>();
    for (Parameter p : method.getParameters()) {
      PathVariable variable = p.getAnnotation(PathVariable.class);
      if (variable == null || p.getType() != Long.class && p.getType() != long.class) {
        continue;
      }
      String name =
          !variable.value().isEmpty()
              ? variable.value()
              : !variable.name().isEmpty() ? variable.name() : p.getName();
      if (!"companyId".equals(name)) {
        ids.add(name);
      }
    }
    return ids;
  }

  /**
   * Every endpoint of the user APIs that addresses a record by a numeric id names it, so the record
   * scope check refuses a record of a company outside the data scope of the caller (OWASP API1,
   * IDOR); programme-wide records are on the explicit {@code GlobalRecord} allow list.
   */
  private static final ArchRule ID_ENDPOINTS_NAME_THEIR_RECORD =
      methods()
          .that()
          .areDeclaredInClassesThat()
          .areAnnotatedWith(RestController.class)
          .and()
          .areDeclaredInClassesThat()
          .resideOutsideOfPackage("..integration..")
          .and()
          .arePublic()
          .should(NAME_THE_RECORD_OF_ITS_ID)
          .because("a user may read or change only the records of his data scope");

  private static final ArchCondition<JavaClass> NAME_COMPANY_RECORDS =
      new ArchCondition<>("name only records that belong to a company") {
        @Override
        public void check(JavaClass item, ConditionEvents events) {
          Class<?> type = item.reflect();
          List<OwnedRecord> named = new ArrayList<>();
          for (Method m : type.getDeclaredMethods()) {
            named.addAll(List.of(m.getAnnotationsByType(OwnedRecord.class)));
          }
          named.addAll(List.of(type.getAnnotationsByType(OwnedRecord.class)));
          for (OwnedRecord r : named) {
            if (r.type().isEmpty() == (r.value() == void.class)) {
              events.add(
                  SimpleConditionEvent.violated(
                      item, item.getName() + " names a record by neither or both entity and type"));
            } else if (r.type().isEmpty() && !ownedByACompany(r.value())) {
              events.add(
                  SimpleConditionEvent.violated(
                      item,
                      item.getName()
                          + " names "
                          + r.value().getName()
                          + ", which belongs to no company: mark it @OwnedBy its parent, or use"
                          + " @GlobalRecord"));
            }
          }
        }
      };

  /**
   * Whether a stored record belongs to a company: it carries the company, it is a company, it names
   * its parent ({@code OwnedBy}), or it links to another record (an attachment).
   *
   * @param type entity class
   * @return true when the record scope check can resolve its company
   */
  static boolean ownedByACompany(Class<?> type) {
    if (!type.isAnnotationPresent(Entity.class)) {
      return false;
    }
    boolean ownCompany =
        "Company".equals(type.getSimpleName())
            || hasAccessor(type, "getCompanyId")
            || hasAccessor(type, "getCompany");
    boolean linked = hasAccessor(type, "getEntityType") && hasAccessor(type, "getEntityId");
    return ownCompany || linked || type.isAnnotationPresent(OwnedBy.class);
  }

  private static boolean hasAccessor(Class<?> type, String name) {
    try {
      return type.getMethod(name).getParameterCount() == 0;
    } catch (NoSuchMethodException ex) {
      return false;
    }
  }

  private static final ArchRule NAMED_RECORDS_BELONG_TO_A_COMPANY =
      classes()
          .that()
          .areAnnotatedWith(RestController.class)
          .should(NAME_COMPANY_RECORDS)
          .because("a record without a company has nothing to check; list it as @GlobalRecord");

  private static final ArchRule COMPANY_ENDPOINTS_ARE_DATA_SCOPED =
      methods()
          .that()
          .areDeclaredInClassesThat()
          .areAnnotatedWith(RestController.class)
          .and()
          .arePublic()
          .should(CHECK_THE_DATA_SCOPE)
          .because("a user may act only for the companies and branches of his data scope");

  /** Request mappings of Spring MVC. */
  private static final Set<String> MAPPINGS =
      Set.of(
          RequestMapping.class.getName(),
          GetMapping.class.getName(),
          PostMapping.class.getName(),
          PutMapping.class.getName(),
          PatchMapping.class.getName(),
          DeleteMapping.class.getName());

  private static final ArchCondition<JavaMethod> DECLARE_WHO_MAY_CALL =
      new ArchCondition<>("declare who may call it with @PreAuthorize") {
        @Override
        public void check(JavaMethod item, ConditionEvents events) {
          boolean endpoint =
              item.getAnnotations().stream()
                  .anyMatch(a -> MAPPINGS.contains(a.getRawType().getName()));
          boolean declared =
              item.isAnnotatedWith(PreAuthorize.class)
                  || item.getOwner().isAnnotatedWith(PreAuthorize.class);
          if (endpoint && !declared) {
            events.add(
                SimpleConditionEvent.violated(
                    item,
                    item.getFullName()
                        + " is an endpoint without @PreAuthorize on the method or its class;"
                        + " name the permission, or mark a public endpoint permitAll()"));
          }
        }
      };

  /**
   * Every endpoint of the user APIs declares who may call it: a permission, a signed-in user, or
   * explicitly anyone ({@code permitAll()}). The system-to-system APIs under {@code
   * /integration/**} are authorised by their own chain (token scopes, deny by default) and are left
   * out.
   */
  private static final ArchRule ENDPOINTS_DECLARE_WHO_MAY_CALL =
      methods()
          .that()
          .areDeclaredInClassesThat()
          .areAnnotatedWith(RestController.class)
          .and()
          .areDeclaredInClassesThat()
          .resideOutsideOfPackage("..integration..")
          .and()
          .arePublic()
          .should(DECLARE_WHO_MAY_CALL)
          .because("no endpoint may be reachable by accident (OWASP A01, API5)");

  private static final ArchCondition<JavaMethod> TAKE_A_REQUEST_RECORD =
      new ArchCondition<>("take its request body as a request type, not as a stored entity") {
        @Override
        public void check(JavaMethod item, ConditionEvents events) {
          item.getParameters().stream()
              .filter(p -> p.isAnnotatedWith(RequestBody.class))
              .filter(p -> p.getRawType().isAnnotatedWith(Entity.class))
              .forEach(
                  p ->
                      events.add(
                          SimpleConditionEvent.violated(
                              item,
                              item.getFullName()
                                  + " binds the request body to the entity "
                                  + p.getRawType().getName()
                                  + "; bind a request record listing the fields a caller may set")));
        }
      };

  /**
   * A request body is never bound to an entity (mass assignment, OWASP API3): a caller could set
   * the id, the version, the owner or the status.
   */
  private static final ArchRule REQUEST_BODIES_ARE_NOT_ENTITIES =
      methods()
          .that()
          .areDeclaredInClassesThat()
          .areAnnotatedWith(RestController.class)
          .should(TAKE_A_REQUEST_RECORD)
          .because("only the fields of the request type can be set by a caller");

  @Test
  void requestBodiesAreNotEntities() {
    REQUEST_BODIES_ARE_NOT_ENTITIES.check(classes);
  }

  @Test
  void endpointsDeclareWhoMayCall() {
    ENDPOINTS_DECLARE_WHO_MAY_CALL.check(classes);
  }

  @Test
  void companyEndpointsAreDataScoped() {
    COMPANY_ENDPOINTS_ARE_DATA_SCOPED.check(classes);
  }

  @Test
  void idEndpointsNameTheirRecord() {
    ID_ENDPOINTS_NAME_THEIR_RECORD.check(classes);
    NAMED_RECORDS_BELONG_TO_A_COMPANY.check(classes);
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
  void sqlValuesAreBoundParameters() {
    SQL_VALUES_ARE_BOUND_PARAMETERS.check(classes);
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
