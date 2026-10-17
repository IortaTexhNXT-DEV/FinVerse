package com.iortatechnxt.brokerverse.security;

import com.iortatechnxt.brokerverse.common.security.CompanyScoped;
import com.iortatechnxt.brokerverse.security.api.DataScopeTargets;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.RecordComponent;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.beans.BeanUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;

/**
 * How a controller method receives a company and which data scope mechanism checks it
 * (DATA_SCOPE_DESIGN.md section 4), shared by the architecture rule and the coverage test.
 */
public final class DataScopeMappings {

  /** The mechanism that checks a company a method receives. */
  public enum Mechanism {
    /** Query parameter or path variable {@code companyId}, or a form object bound from them. */
    INTERCEPTOR,
    /** A request body the body advice reads. */
    BODY_ADVICE,
    /** An explicit guard call, the method is {@code @CompanyScoped}. */
    EXPLICIT,
    /** Nothing checks it: the build fails. */
    UNCOVERED
  }

  private DataScopeMappings() {}

  /**
   * The mechanisms that check the companies a method receives.
   *
   * @param method controller method
   * @return mechanisms; empty when the method takes no company
   */
  public static Set<Mechanism> mechanisms(Method method) {
    Set<Mechanism> found = EnumSet.noneOf(Mechanism.class);
    for (Parameter p : method.getParameters()) {
      Mechanism m = mechanism(p);
      if (m != null) {
        found.add(m);
      }
    }
    if (found.remove(Mechanism.UNCOVERED)
        && !AnnotatedElementUtils.hasAnnotation(method, CompanyScoped.class)) {
      found.add(Mechanism.UNCOVERED);
    } else if (AnnotatedElementUtils.hasAnnotation(method, CompanyScoped.class)) {
      found.add(Mechanism.EXPLICIT);
    }
    return found;
  }

  /**
   * Whether the method takes a company.
   *
   * @param method controller method
   * @return true when a parameter carries a company
   */
  public static boolean takesCompany(Method method) {
    return !mechanisms(method).isEmpty();
  }

  /**
   * Whether every company the method receives is checked.
   *
   * @param method controller method
   * @return true when covered (or the method takes no company)
   */
  public static boolean covered(Method method) {
    return !mechanisms(method).contains(Mechanism.UNCOVERED);
  }

  private static Mechanism mechanism(Parameter p) {
    String bound = boundParameterName(p);
    if (bound != null) {
      return bound.equals(DataScopeTargets.COMPANY) ? Mechanism.INTERCEPTOR : namedCompany(p);
    }
    if (p.isAnnotationPresent(RequestBody.class)) {
      return bodyMechanism(p);
    }
    if (p.isAnnotationPresent(RequestPart.class)
        || p.isAnnotationPresent(RequestHeader.class)
        || p.isAnnotationPresent(CookieValue.class)) {
      return DataScopeTargets.carriesCompany(p.getParameterizedType())
          ? Mechanism.UNCOVERED
          : namedCompany(p);
    }
    // A form object (@ModelAttribute or no annotation) is bound from the query parameters, so its
    // companyId is the query parameter the interceptor reads.
    return !BeanUtils.isSimpleProperty(p.getType())
            && DataScopeTargets.carriesCompany(p.getParameterizedType())
        ? Mechanism.INTERCEPTOR
        : namedCompany(p);
  }

  /** The request name of a query parameter or path variable, null for other parameters. */
  private static String boundParameterName(Parameter p) {
    RequestParam param = p.getAnnotation(RequestParam.class);
    if (param != null) {
      return boundName(param.name(), param.value(), p);
    }
    PathVariable path = p.getAnnotation(PathVariable.class);
    return path == null ? null : boundName(path.name(), path.value(), p);
  }

  /**
   * A body with {@code companyId} is read by the advice; a body that names a company under another
   * name ({@code parentCompanyId}, {@code companyAId}) needs an explicit check as well.
   */
  private static Mechanism bodyMechanism(Parameter p) {
    if (otherCompanyNames(p.getType())) {
      return Mechanism.UNCOVERED;
    }
    return DataScopeTargets.carriesCompany(p.getParameterizedType()) ? Mechanism.BODY_ADVICE : null;
  }

  private static boolean otherCompanyNames(Class<?> type) {
    if (!type.isRecord()) {
      return false;
    }
    for (RecordComponent c : type.getRecordComponents()) {
      String name = c.getName();
      if (c.getType() == Long.class
          && !DataScopeTargets.COMPANY.equals(name)
          && name.matches("\\w*[cC]ompany\\w*Id")) {
        return true;
      }
    }
    return false;
  }

  private static Mechanism namedCompany(Parameter p) {
    return DataScopeTargets.COMPANY.equals(p.getName()) ? Mechanism.UNCOVERED : null;
  }

  private static String boundName(String name, String value, Parameter p) {
    if (!name.isEmpty()) {
      return name;
    }
    return value.isEmpty() ? p.getName() : value;
  }
}
