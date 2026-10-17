package com.iortatechnxt.brokerverse.events.service;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Binds the integration event settings (with Kafka on or off). */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(EventsProperties.class)
public class EventsConfiguration {}
