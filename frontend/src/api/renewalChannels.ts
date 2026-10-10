import { api, toQuery } from './client';

/** CCM and MFT messages of Renewal: Channel Monitor and the deliveries of a renewal account. */
export interface ChannelRow {
  messageNo: string;
  channel: string;
  direction: string;
  docKind: string;
  docRef: string | null;
  renewalRef: string | null;
  fileName: string | null;
  recipients: string | null;
  status: string;
  externalRef: string | null;
  attempts: number;
  lastError: string | null;
  createdAt: string;
  submittedAt: string | null;
  sentAt: string | null;
  deliveredAt: string | null;
  createdBy: string;
}

export interface ChannelEventView {
  status: string;
  label: string;
  detail: string | null;
  at: string;
  by: string;
}

export interface ChannelConnection {
  settings: {
    channel: string;
    mode: string;
    endpoint: string;
    keySetting: string;
    keySet: boolean;
  };
  check: { channel: string; live: boolean; reachable: boolean; detail: string };
}

export interface Delivery {
  messageNo: string;
  channel: string;
  docKind: string;
  docRef: string | null;
  fileName: string | null;
  recipients: string | null;
  status: string;
  statusLabel: string;
  externalRef: string | null;
  error: string | null;
  queuedAt: string;
  sentAt: string | null;
  deliveredAt: string | null;
  queuedBy: string;
}

export interface ChannelFilter {
  channel?: string;
  status?: string;
  kind?: string;
  search?: string;
}

const CH = '/renewal/channels';

export const renewalChannelsApi = {
  list: (companyId: number, f: ChannelFilter) =>
    api.get<ChannelRow[]>(`${CH}${toQuery({ companyId, ...f })}`),
  history: (companyId: number, messageNo: string) =>
    api.get<ChannelEventView[]>(
      `${CH}/${encodeURIComponent(messageNo)}/history${toQuery({ companyId })}`,
    ),
  resend: (companyId: number, messageNo: string) =>
    api.post<{ messageNo: string; status: string; error: string | null }>(
      `${CH}/${encodeURIComponent(messageNo)}/resend${toQuery({ companyId })}`,
      {},
    ),
  cancel: (companyId: number, messageNo: string) =>
    api.post<{ messageNo: string; status: string; error: string | null }>(
      `${CH}/${encodeURIComponent(messageNo)}/cancel${toQuery({ companyId })}`,
      {},
    ),
  errorReport: (companyId: number, channel?: string) =>
    api.getFile(`${CH}/error-report${toQuery({ companyId, channel })}`),
  connection: (companyId: number, channel: string) =>
    api.get<ChannelConnection>(`${CH}/connection${toQuery({ companyId, channel })}`),
  deliveries: (companyId: number, ref: string) =>
    api.get<Delivery[]>(
      `/renewal/candidates/${encodeURIComponent(ref)}/deliveries${toQuery({ companyId })}`,
    ),
};

/** The delivery status as users read it. */
export const CHANNEL_STATUS_LABELS: Record<string, string> = {
  PENDING_TRANSMISSION: 'Pending Transmission',
  SUBMITTED: 'Submitted to CCM',
  SENT: 'Sent',
  DELIVERED: 'Delivered',
  FAILED: 'Failed',
  CANCELLED: 'Cancelled',
  RECEIVED: 'Received',
  PROCESSED: 'Processed',
};
