import { api, toQuery } from './client';

/** One field change of a master record (G05 change log). */
export interface FieldChange {
  id: number;
  entityType: string;
  entityId: number;
  /** CREATE (the record was added) or UPDATE (a field changed). */
  changeKind: 'CREATE' | 'UPDATE';
  field: string;
  oldValue: string | null;
  newValue: string | null;
  maker: string | null;
  madeAt: string;
  /** Checker who authorized the change; null while pending or for records outside maker-checker. */
  checker: string | null;
  checkedAt: string | null;
}

/** Field-level change history of master records (insurers, products, clients and other masters). */
export const changeHistoryApi = {
  history: (entityType: string, entityId: number) =>
    api.get<FieldChange[]>(`/change-history${toQuery({ entityType, entityId })}`),
};
