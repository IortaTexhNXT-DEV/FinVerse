package com.iortatechnxt.brokerverse.identity.service;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Binds the connection to the Enterprise SSO platform ({@link IdentityProperties}). */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(IdentityProperties.class)
public class IdentityConfiguration {}
