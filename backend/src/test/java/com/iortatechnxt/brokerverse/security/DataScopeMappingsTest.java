package com.iortatechnxt.brokerverse.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.common.security.CompanyScoped;
import com.iortatechnxt.brokerverse.security.DataScopeMappings.Mechanism;
import com.iortatechnxt.brokerverse.security.api.DataScopeTargets;
import com.iortatechnxt.brokerverse.security.api.DataScopeTargets.Target;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;

/**
 * The rules of the data scope architecture check and of the body advice: what counts as taking a
 * company and which mechanism covers it; a company bound under another name fails the build unless
 * the method checks it explicitly.
 */
class DataScopeMappingsTest {

  /** A body with a company and a branch. */
  public record Line(Long companyId, Long branchId, String text) {}

  /** A bulk body: items with a company. */
  public record Bulk(List<Line> items, String note) {}

  /** A body naming two companies under other names. */
  public record Pair(Long companyAId, Long companyBId) {}

  /** A body without a company. */
  public record Plain(String text) {}

  /** A bean-style body. */
  public static final class Bean {
    private final Long companyId;

    Bean(Long companyId) {
      this.companyId = companyId;
    }

    public Long getCompanyId() {
      return companyId;
    }
  }

  /** Controller methods of every kind (never registered). */
  @SuppressWarnings("unused")
  static final class Fixture {
    public void query(@RequestParam Long companyId) {}

    public void named(@RequestParam("companyId") Long id) {}

    public void path(@PathVariable Long companyId) {}

    public void body(@RequestBody Line line) {}

    public void listBody(@RequestBody List<Line> lines) {}

    public void bulkBody(@RequestBody Bulk bulk) {}

    public void beanBody(@RequestBody Bean bean) {}

    public void form(@ModelAttribute Line search) {}

    public void none(@RequestParam Long id, @RequestBody Plain plain) {}

    public void otherName(@RequestParam("company") Long companyId) {}

    public void header(@RequestHeader("X-Company") Long companyId) {}

    public void part(@RequestPart("data") Line line) {}

    @CompanyScoped("checks the part explicitly")
    public void explicitPart(@RequestPart("data") Line line) {}

    public void pair(@RequestBody Pair pair) {}

    @CompanyScoped("checks both companies")
    public void explicitPair(@RequestBody Pair pair) {}
  }

  private static Method method(String name) {
    for (Method m : Fixture.class.getDeclaredMethods()) {
      if (m.getName().equals(name)) {
        return m;
      }
    }
    throw new IllegalArgumentException(name);
  }

  @Test
  void queryParametersAndPathVariablesAreTheInterceptors() {
    assertThat(DataScopeMappings.mechanisms(method("query")))
        .containsExactly(Mechanism.INTERCEPTOR);
    assertThat(DataScopeMappings.mechanisms(method("named")))
        .containsExactly(Mechanism.INTERCEPTOR);
    assertThat(DataScopeMappings.mechanisms(method("path"))).containsExactly(Mechanism.INTERCEPTOR);
    assertThat(DataScopeMappings.mechanisms(method("form"))).containsExactly(Mechanism.INTERCEPTOR);
  }

  @Test
  void bodiesWithACompanyAreTheBodyAdvices() {
    for (String name : List.of("body", "listBody", "bulkBody", "beanBody")) {
      assertThat(DataScopeMappings.mechanisms(method(name)))
          .as(name)
          .containsExactly(Mechanism.BODY_ADVICE);
    }
    assertThat(DataScopeMappings.takesCompany(method("none"))).isFalse();
    assertThat(DataScopeMappings.covered(method("none"))).isTrue();
  }

  @Test
  void aCompanyNoCheckReadsFailsUnlessCheckedExplicitly() {
    assertThat(DataScopeMappings.covered(method("otherName"))).isFalse();
    assertThat(DataScopeMappings.covered(method("header"))).isFalse();
    assertThat(DataScopeMappings.covered(method("part"))).isFalse();
    assertThat(DataScopeMappings.mechanisms(method("explicitPart")))
        .containsExactly(Mechanism.EXPLICIT);
    assertThat(DataScopeMappings.covered(method("pair"))).isFalse();
    assertThat(DataScopeMappings.mechanisms(method("explicitPair")))
        .containsExactly(Mechanism.EXPLICIT);
  }

  @Test
  void theAdviceReadsTheCompaniesOfABody() {
    assertThat(DataScopeTargets.companiesOf(new Line(1L, 2L, "x")))
        .containsExactly(new Target(1L, 2L));
    assertThat(
            DataScopeTargets.companiesOf(
                new Bulk(List.of(new Line(1L, null, "a"), new Line(3L, 4L, "b")), "n")))
        .containsExactly(new Target(1L, null), new Target(3L, 4L));
    assertThat(DataScopeTargets.companiesOf(List.of(new Line(5L, null, "c"))))
        .containsExactly(new Target(5L, null));
    assertThat(DataScopeTargets.companiesOf(new Bean(6L))).containsExactly(new Target(6L, null));
    assertThat(DataScopeTargets.companiesOf(new Plain("p"))).isEmpty();
    assertThat(DataScopeTargets.companiesOf(null)).isEmpty();
    assertThat(DataScopeTargets.companiesOf("text")).isEmpty();
    assertThat(DataScopeTargets.carriesCompany(String.class)).isFalse();
    assertThat(DataScopeTargets.carriesCompany(null)).isFalse();
  }
}
