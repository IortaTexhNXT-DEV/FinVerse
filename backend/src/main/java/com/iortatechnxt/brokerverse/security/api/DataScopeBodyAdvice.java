package com.iortatechnxt.brokerverse.security.api;

import com.iortatechnxt.brokerverse.common.security.DataScope;
import java.lang.reflect.Type;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

/**
 * Checks the company and branch a request body names against the data scope of the signed-in user,
 * after the body is read and before the controller runs (DATA_SCOPE_DESIGN.md section 4). Reads
 * what {@link DataScopeTargets} describes: the body's {@code companyId} (and {@code branchId}), the
 * items of a list body, the items of the list components of a record body. The system-to-system
 * APIs pass, as the guard runs them as the system.
 */
@ControllerAdvice
public class DataScopeBodyAdvice extends RequestBodyAdviceAdapter {

  private final DataScope dataScope;

  /**
   * Creates the advice.
   *
   * @param dataScope data scope guard
   */
  public DataScopeBodyAdvice(DataScope dataScope) {
    this.dataScope = dataScope;
  }

  @Override
  public boolean supports(
      MethodParameter methodParameter,
      Type targetType,
      Class<? extends HttpMessageConverter<?>> converterType) {
    return DataScopeTargets.carriesCompany(targetType);
  }

  @Override
  public Object afterBodyRead(
      Object body,
      HttpInputMessage inputMessage,
      MethodParameter parameter,
      Type targetType,
      Class<? extends HttpMessageConverter<?>> converterType) {
    DataScopeTargets.companiesOf(body)
        .forEach(t -> dataScope.requireBranch(t.companyId(), t.branchId()));
    return body;
  }
}
