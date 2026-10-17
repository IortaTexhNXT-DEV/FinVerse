import { api } from './client';

/** Core entities of the MIS fields (FR-CR-062). */
export type MisEntity = 'USER' | 'PRODUCT' | 'INSURER' | 'CLAIM' | 'ACCOUNT' | 'TRANSACTION';

/** A field of the MIS fields catalogue. */
export interface MisField {
  id: number;
  code: string;
  label: string;
  entity: MisEntity;
  /** Data element read by the field. */
  source: string;
  /** Report column keys that carry the data element. */
  columnKeys: string[];
  lovType: string | null;
  recordStatus: 'PENDING_AUTHORIZATION' | 'ACTIVE' | 'INACTIVE';
  maker: string;
  authorizedBy: string | null;
  authorizedAt: string | null;
}

/** A field to add or change. */
export interface MisFieldInput {
  code: string;
  label: string;
  entity: MisEntity;
  source: string;
  lovType: string | null;
}

const B = '/mis-fields';

/** MIS Fields (Broking Setup, FR-CR-062) and the labels of the Layout panel of the reports. */
export const misFieldsApi = {
  list: () => api.get<MisField[]>(B),
  create: (input: MisFieldInput) => api.post<MisField>(B, input),
  change: (id: number, input: MisFieldInput) => api.put<MisField>(`${B}/${String(id)}`, input),
  authorize: (id: number) => api.post<MisField>(`${B}/${String(id)}/authorize`),
  deactivate: (id: number) => api.post<MisField>(`${B}/${String(id)}/deactivate`),
  reactivate: (id: number) => api.post<MisField>(`${B}/${String(id)}/reactivate`),
};

/** The data elements of each entity an MIS field may read (the server checks the same list). */
export const MIS_SOURCES: Record<MisEntity, readonly string[]> = {
  PRODUCT: ['PRODUCT_TYPE', 'RISK_CODE'],
  INSURER: ['INSURER'],
  ACCOUNT: [
    'POLICY_NUMBER',
    'MARKET_SEGMENT',
    'BOOKING_DATE',
    'BRANCH',
    'STATUS',
    'BUSINESS_ORIGIN',
    'REGION',
    'AREA',
  ],
  CLAIM: ['CLAIM_STATUS'],
  USER: ['ACCOUNT_OFFICER', 'UNIT_HEAD', 'DEPARTMENT'],
  TRANSACTION: ['TRANSACTION_TYPE', 'CHART_OF_ACCOUNT'],
};
