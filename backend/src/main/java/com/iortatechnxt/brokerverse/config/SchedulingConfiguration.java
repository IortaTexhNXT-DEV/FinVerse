package com.iortatechnxt.brokerverse.config;

import com.iortatechnxt.brokerverse.common.runtime.ConditionalOnWorkload;
import com.iortatechnxt.brokerverse.common.runtime.Workload;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Turns the task scheduler on only for the runtime roles that run scheduled work ({@code jobs},
 * {@code integration} and {@code all}). A {@code web} instance has no scheduler at all; which jobs
 * a scheduling instance registers is decided per job workload by {@code system.service
 * .JobScheduler}.
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnWorkload({Workload.BATCH, Workload.INTEGRATION})
public class SchedulingConfiguration {}
