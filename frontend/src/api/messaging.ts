import { api, toQuery } from './client';
import type { PageResponse } from './types';

export type MessageStatus = 'QUEUED' | 'SENT' | 'FAILED' | 'CANCELLED';

export interface MessageAttachmentInfo {
  id: number;
  fileName: string;
  sizeBytes: number;
  sha256: string;
  passwordProtected: boolean;
}

export interface OutboundMessage {
  id: number;
  purpose: string;
  recipients: string;
  cc?: string;
  subject: string;
  body: string;
  status: MessageStatus;
  attempts: number;
  lastError?: string;
  sentAt?: string;
  simulated: boolean;
  entityType?: string;
  entityId?: string;
  reference?: string;
  passwordForId?: number;
  createdAt: string;
  createdBy: string;
  attachments: MessageAttachmentInfo[];
}

export interface AppNotification {
  id: number;
  title: string;
  body?: string;
  link?: string;
  createdAt: string;
  read: boolean;
}

export interface MessageFilters {
  status?: MessageStatus;
  purpose?: string;
  text?: string;
  page?: number;
}

/** Outbound e-mail log and in-app notifications. */
export const messagingApi = {
  search: (f: MessageFilters) =>
    api.get<PageResponse<OutboundMessage>>(`/messages${toQuery({ ...f, size: 50 })}`),
  forRecord: (entityType: string, entityId: string | number) =>
    api.get<OutboundMessage[]>(
      `/messages/by-record${toQuery({ entityType, entityId: String(entityId) })}`,
    ),
  retry: (id: number) => api.post<OutboundMessage>(`/messages/${id}/retry`),
  notifications: (unreadOnly = false) =>
    api.get<PageResponse<AppNotification>>(`/notifications${toQuery({ unreadOnly, size: 20 })}`),
  unreadCount: () => api.get<{ unread: number }>('/notifications/unread-count'),
  markRead: (id: number) => api.post<AppNotification>(`/notifications/${id}/read`),
  markAllRead: () => api.post<{ updated: number }>('/notifications/read-all'),
};
