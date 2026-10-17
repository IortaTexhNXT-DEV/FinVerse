package com.iortatechnxt.brokerverse.system.api;

import com.iortatechnxt.brokerverse.system.service.ProductModules;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Refuses every API call to a controller of a switched-off product module with 404 {@code
 * MODULE_NOT_IN_USE} ("The ... module is not in use in this deployment"), before the permission
 * check.
 */
@Configuration(proxyBeanMethods = false)
public class ModuleSwitchInterceptor implements HandlerInterceptor, WebMvcConfigurer {

  private final ProductModules modules;

  /**
   * Creates the interceptor.
   *
   * @param modules module switches
   */
  public ModuleSwitchInterceptor(ProductModules modules) {
    this.modules = modules;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(this).addPathPatterns("/api/**");
  }

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    if (handler instanceof HandlerMethod method) {
      modules.requireClassOn(method.getBeanType());
    }
    return true;
  }
}
