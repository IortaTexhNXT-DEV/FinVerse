import { api, toQuery } from './client';

/**
 * Renewal accounts created by hand with the duplicate warning, Refresh Endorsements and the CBG
 * Motor automatic values (BDOI Renewal FRS FRRN.004.06, FRRN.005.01, FRRN.006.01, FRRN.009.01).
 */
export interface DuplicateMatch {
  ref: string | null;
  exact: boolean;
  cancelled: boolean;
  criteria: string[];
}

export interface ManualCreationResult {
  renewalRef: string | null;
  message: string | null;
  duplicates: DuplicateMatch[];
}

export interface EndorsementRow {
  reference: string;
  source: string;
  status: string | null;
  effectiveDate: string | null;
}

export interface AutoValueRow {
  field: string;
  expiring: string | null;
  renewal: string | null;
  account: string | null;
  difference: number | null;
}

const BASE = '/renewal';

export const renewalUpdatesApi = {
  createManual: (companyId: number, invoiceNo: string, confirmed: boolean) =>
    api.post<ManualCreationResult>(`${BASE}/manual-accounts${toQuery({ companyId })}`, {
      invoiceNo,
      confirmed,
    }),
  refreshEndorsements: (companyId: number, ref: string) =>
    api.post<EndorsementRow[]>(
      `${BASE}/candidates/${encodeURIComponent(ref)}/refresh-endorsements${toQuery({ companyId })}`,
      {},
    ),
  autoUpdate: (companyId: number, ref: string) =>
    api.get<AutoValueRow[]>(
      `${BASE}/candidates/${encodeURIComponent(ref)}/auto-update${toQuery({ companyId })}`,
    ),
};
