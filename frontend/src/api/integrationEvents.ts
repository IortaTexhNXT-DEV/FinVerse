import { api, toQuery } from './client';
import type { PageResponse } from './types';

/** One integration topic (Kafka) of the catalogue. */
export interface IntegrationTopic {
  name: string;
  deadLetterTopic: string;
  eventTypes: string[];
  description: string;
  kafkaEnabled: boolean;
}

export type OutboxStatus = 'PENDING' | 'SENT' | 'LOCAL' | 'FAILED';

/** One row of the transactional outbox. */
export interface OutboxEvent {
  id: number;
  eventId: string;
  topic: string;
  type: string;
  key: string;
  companyCode?: string;
  correlationId: string;
  occurredAt: string;
  status: OutboxStatus;
  attempts: number;
  nextAttemptAt: string;
  lastError?: string;
  sentAt?: string;
  payload: string;
}

export type DeadLetterStatus = 'NEW' | 'RETRIED' | 'DISCARDED';

/** An event a consumer could not process (dead-letter topic). */
export interface DeadLetter {
  id: number;
  eventId?: string;
  originalTopic: string;
  deadLetterTopic: string;
  key?: string;
  consumerGroup?: string;
  errorMessage?: string;
  status: DeadLetterStatus;
  receivedAt: string;
  resolvedAt?: string;
  resolvedBy?: string;
  payload: string;
}

/** Support API of the integration events (System Administrator). */
export const integrationEventsApi = {
  topics: () => api.get<IntegrationTopic[]>('/admin/events/topics'),
  outbox: (status?: OutboxStatus, key?: string, page = 0) =>
    api.get<PageResponse<OutboxEvent>>(
      `/admin/events/outbox${toQuery({ status, key, page, size: 25 })}`,
    ),
  retryOutbox: (id: number) => api.post<undefined>(`/admin/events/outbox/${id}/retry`),
  deadLetters: (status: DeadLetterStatus | 'ALL', page = 0) =>
    api.get<PageResponse<DeadLetter>>(
      `/admin/events/dead-letters${toQuery({ status, page, size: 25 })}`,
    ),
  retryDeadLetter: (id: number) => api.post<undefined>(`/admin/events/dead-letters/${id}/retry`),
  discardDeadLetter: (id: number) =>
    api.post<undefined>(`/admin/events/dead-letters/${id}/discard`),
};
