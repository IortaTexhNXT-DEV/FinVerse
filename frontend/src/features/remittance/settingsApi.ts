import { api } from '@/api/client';
import { toQuery } from './api';

/**
 * Remittance Settings and the extract files of a batch (FRS BRD-02 FRID.RMT.001 and 003; ICD
 * IF-RMT-01, 02 and 04): /api/v1/remittance/settings and /api/v1/remittance/batches/{id}.
 */

export interface RemittanceSettings {
  scheduleActive: boolean;
  extractionTime: string;
  workingDaysOnly: boolean;
  folderNormalPhp: string;
  folderNormalUsd: string;
  folderWithIncentives: string;
  filePattern: string;
  textPrefix: string;
}

export type SettingChangeStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'SUPERSEDED';

export interface SettingChange {
  id: number;
  values: RemittanceSettings;
  reason: string;
  status: SettingChangeStatus;
  requestedBy: string;
  requestedAt: string;
  decidedBy?: string;
  decidedAt?: string;
  decisionComment?: string;
}

export interface SettingsView {
  current: RemittanceSettings;
  changes: SettingChange[];
}

export interface FileDelivery {
  id: number;
  stage: 'CREATED' | 'APPROVED' | 'RESENT';
  fileName: string;
  folder: string;
  status: 'DELIVERED' | 'FAILED';
  reference?: string;
  error?: string;
  attemptedAt: string;
  attemptedBy?: string;
}

const BASE = '/remittance';

export const remittanceSettingsApi = {
  get: (companyId: number) => api.get<SettingsView>(`${BASE}/settings${toQuery({ companyId })}`),
  propose: (companyId: number, values: RemittanceSettings, reason: string) =>
    api.post<SettingChange>(`${BASE}/settings/changes`, { companyId, values, reason }),
  decide: (id: number, approve: boolean, comment?: string) =>
    api.post<SettingChange>(
      `${BASE}/settings/changes/${String(id)}/${approve ? 'approve' : 'reject'}`,
      { comment },
    ),
  extract: (batchId: number, format: 'EXCEL' | 'TEXT') =>
    api.getFile(`${BASE}/batches/${String(batchId)}/extract/${format}`),
  deliveries: (batchId: number) =>
    api.get<FileDelivery[]>(`${BASE}/batches/${String(batchId)}/deliveries`),
  resend: (batchId: number) =>
    api.post<FileDelivery[]>(`${BASE}/batches/${String(batchId)}/deliveries`, {}),
};

/** Whether proposed settings can be sent: a time, three folders, a pattern with the batch keys. */
export function settingsReady(v: RemittanceSettings): boolean {
  return (
    /^([01]\d|2[0-3]):[0-5]\d$/.test(v.extractionTime) &&
    [v.folderNormalPhp, v.folderNormalUsd, v.folderWithIncentives].every((f) => f.trim() !== '') &&
    v.filePattern.includes('{insurer}') &&
    v.filePattern.includes('{settlement}') &&
    /^[A-Za-z0-9]{0,10}$/.test(v.textPrefix)
  );
}
