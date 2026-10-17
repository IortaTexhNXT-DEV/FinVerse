import type { AlertItem, ExceptionCodeInfo } from '@/api/alerts';
import { humanize } from '@/utils/format';

/** The reference of an alert as users quote it: ALR-000012. */
export function alertRef(id: number): string {
  return `ALR-${String(id).padStart(6, '0')}`;
}

/** The business names of the alert rules, by rule code. */
export function ruleNames(codes: readonly ExceptionCodeInfo[] | undefined): Map<string, string> {
  return new Map((codes ?? []).map((c) => [c.code, c.name]));
}

/** The business name of an alert rule; the code in words while the names load. */
export function ruleName(names: ReadonlyMap<string, string>, code: string): string {
  return names.get(code) ?? humanize(code);
}

/** Records whose page opens by the key the alert carries. */
const RECORD_PAGES: Record<string, (key: string) => string> = {
  BrokerClaim: (key) => `/claims-handling/${key}`,
  MigBatch: (key) => `/migration/batches/${encodeURIComponent(key)}`,
  OpsInvoice: (key) => `/operations/invoices/${encodeURIComponent(key)}`,
  CollectionWorklist: () => '/collections/worklist',
  JournalBatch: () => '/gl/journals',
  ScreeningCase: () => '/screening/cases',
  SubmittedRun: () => '/submitted/runs',
};

/** The page of the record an alert is about, when there is one. */
export function recordLink(alert: Pick<AlertItem, 'entityType' | 'entityId'>): string | undefined {
  const page = alert.entityType === undefined ? undefined : RECORD_PAGES[alert.entityType];
  return page === undefined || alert.entityId === undefined ? undefined : page(alert.entityId);
}

/** Record kinds whose words differ from their type name. */
const KINDS: Record<string, string> = {
  BrokerClaim: 'Claim',
  DisbursementIntake: 'Payment request',
  JournalBatch: 'Journal',
  MigBatch: 'Migration batch',
  OpsInvoice: 'Invoice',
};

/** The kind of record an alert is about, in words ("ScreeningCase" becomes "Screening case"). */
export function recordKind(entityType: string | undefined): string {
  if (entityType === undefined) {
    return '';
  }
  const known = KINDS[entityType];
  if (known !== undefined) {
    return known;
  }
  const words = entityType.replace(/([a-z])([A-Z])/g, '$1 $2').toLowerCase();
  return words.charAt(0).toUpperCase() + words.slice(1);
}

/** The record an alert is about as users read it: "Journal INV-HO-2026-000002" (an internal number is left out). */
export function recordText(alert: Pick<AlertItem, 'entityType' | 'entityId'>): string {
  const kind = recordKind(alert.entityType);
  if (alert.entityId === undefined || /^\d+$/.test(alert.entityId)) {
    return kind;
  }
  return `${kind} ${alert.entityId}`.trim();
}
