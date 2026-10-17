package com.iortatechnxt.brokerverse.audit.api;

import com.iortatechnxt.brokerverse.audit.service.RecordScope;
import com.iortatechnxt.brokerverse.common.security.OwnedRecord;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.Advisor;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.HandlerMapping;

/**
 * The record scope check (DATA_SCOPE_DESIGN.md section 4.1): when an endpoint that addresses a
 * stored record by its id is called, the record named by its {@link OwnedRecord} declarations is
 * loaded and refused with HTTP 404 when it belongs to a company or branch outside the data scope of
 * the signed-in user. The check runs after the permission check of the endpoint ({@code
 * PreAuthorize}), so a caller without the permission is still answered 403, and before the endpoint
 * body, so nothing is read or changed for a record outside the scope. A path value that is not a
 * number is left to the request binding (HTTP 400).
 */
@Configuration(proxyBeanMethods = false)
public final class RecordScopeAdvisor {

  private RecordScopeAdvisor() {}

  /**
   * The advisor applied to every controller.
   *
   * @param recordScope record scope check (resolved on first use)
   * @return advisor
   */
  @Bean
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  static Advisor recordScopeCheck(ObjectProvider<RecordScope> recordScope) {
    DefaultPointcutAdvisor advisor =
        new DefaultPointcutAdvisor(
            new AnnotationMatchingPointcut(RestController.class, true), new Check(recordScope));
    advisor.setOrder(Ordered.LOWEST_PRECEDENCE);
    return advisor;
  }

  /**
   * The records an endpoint addresses, by path variable: the declarations of its class, replaced by
   * those of the method for the same variable.
   *
   * @param type controller class
   * @param method endpoint method
   * @return declaration per path variable
   */
  public static Map<String, OwnedRecord> declarations(Class<?> type, Method method) {
    Map<String, OwnedRecord> found = new LinkedHashMap<>();
    AnnotatedElementUtils.findMergedRepeatableAnnotations(type, OwnedRecord.class)
        .forEach(d -> found.put(d.variable(), d));
    AnnotatedElementUtils.findMergedRepeatableAnnotations(method, OwnedRecord.class)
        .forEach(d -> found.put(d.variable(), d));
    return found;
  }

  /** Checks the records of the current request before the endpoint runs. */
  private static final class Check implements MethodInterceptor {

    private final ObjectProvider<RecordScope> recordScope;

    private Check(ObjectProvider<RecordScope> recordScope) {
      this.recordScope = recordScope;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
      RequestAttributes request = RequestContextHolder.getRequestAttributes();
      if (request != null
          && request.getAttribute(
                  HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST)
              instanceof Map<?, ?> variables
          && !variables.isEmpty()) {
        Class<?> type =
            invocation.getThis() == null
                ? invocation.getMethod().getDeclaringClass()
                : AopUtils.getTargetClass(invocation.getThis());
        declarations(type, invocation.getMethod())
            .forEach((variable, record) -> check(record, variables.get(variable)));
      }
      return invocation.proceed();
    }

    private void check(OwnedRecord record, Object value) {
      if (!(value instanceof String text)) {
        return;
      }
      Long id = number(text);
      if (record.type().isEmpty()) {
        recordScope.getObject().require(record.value(), id);
      } else {
        recordScope.getObject().require(record.type(), id);
      }
    }

    private static Long number(String value) {
      try {
        return Long.valueOf(value.trim());
      } catch (NumberFormatException ex) {
        return null;
      }
    }
  }
}
