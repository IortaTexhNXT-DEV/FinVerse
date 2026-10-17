import type {
  CatalogueDataset,
  EnvironmentFacts,
  ChangeType,
  ConfigImport,
  DatasetDrift,
  ImportDataset,
  ImportMessages,
  ImportStatus,
  ItemView,
  PackageKind,
} from '@/api/configPromotion';
import type { Tone } from '@/components/ui/statusTones';

/** Labels and rules of the Configuration Promotion screens (kept apart from the pages for tests). */

const STATUS: Record<ImportStatus, { label: string; tone: Tone }> = {
  CHECKED: { label: 'Checked', tone: 'info' },
  SUBMITTED: { label: 'Waiting for Approval', tone: 'warning' },
  APPLIED: { label: 'Applied', tone: 'success' },
  REJECTED: { label: 'Rejected', tone: 'danger' },
  FAILED: { label: 'Failed', tone: 'danger' },
  CANCELLED: { label: 'Withdrawn', tone: 'neutral' },
};

/** Label and tone of the status of an import. */
export function importStatus(status: ImportStatus): { label: string; tone: Tone } {
  return STATUS[status];
}

const KINDS: Record<PackageKind, string> = {
  EXPORT: 'Exported here',
  UPLOAD: 'Uploaded for import',
  SNAPSHOT: 'Snapshot before an import',
};

/** What a package is, in words. */
export function packageKindLabel(kind: PackageKind): string {
  return KINDS[kind];
}

const CHANGES: Record<ChangeType, string> = {
  ADDED: 'Added',
  CHANGED: 'Changed',
  UNCHANGED: 'Unchanged',
  ONLY_IN_TARGET: 'Only in this environment',
};

/** A kind of difference in words. */
export function changeLabel(type: ChangeType): string {
  return CHANGES[type];
}

/** Drift wording: the same differences seen from the baseline. */
export function driftLabel(type: ChangeType): string {
  if (type === 'ADDED') {
    return 'New since the baseline';
  }
  if (type === 'ONLY_IN_TARGET') {
    return 'No longer present';
  }
  return CHANGES[type];
}

/** The first characters of a checksum, enough to compare two of them on screen. */
export function shortHash(sha: string | null | undefined): string {
  return sha === null || sha === undefined || sha === '' ? '—' : sha.slice(0, 12);
}

/** A file size in words. */
export function fileSize(bytes: number): string {
  if (bytes < 1024) {
    return `${bytes} bytes`;
  }
  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(1)} KB`;
  }
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

/** The kinds of action on an import. */
export type ImportAction = 'submit' | 'check' | 'withdraw' | 'approve' | 'reject' | 'rollback';

/** Who looks at an import. */
export interface Viewer {
  username: string;
  mayPrepare: boolean;
  mayApprove: boolean;
}

function same(a: string | null | undefined, b: string): boolean {
  return (a ?? '').toLowerCase() === b.toLowerCase();
}

/**
 * The actions open to a user on an import: the preparer submits, checks again, withdraws and rolls
 * back an applied import; another user with the approval right approves or rejects. Nobody
 * approves an import they prepared.
 */
export function importActions(imp: ConfigImport, who: Viewer): ImportAction[] {
  const preparer = same(imp.preparedBy, who.username);
  const actions: ImportAction[] = who.mayPrepare && preparer ? preparerActions(imp) : [];
  if (who.mayApprove && !preparer && imp.status === 'SUBMITTED') {
    actions.push('approve', 'reject');
  }
  if (who.mayPrepare && imp.status === 'APPLIED' && hasSnapshot(imp)) {
    actions.push('rollback');
  }
  return actions;
}

function preparerActions(imp: ConfigImport): ImportAction[] {
  const actions: ImportAction[] = [];
  if (imp.status === 'CHECKED' && imp.compatible && imp.blockerCount === 0) {
    actions.push('submit');
  }
  if (imp.status === 'CHECKED' || imp.status === 'FAILED') {
    actions.push('check');
  }
  if (imp.status !== 'APPLIED' && imp.status !== 'REJECTED' && imp.status !== 'CANCELLED') {
    actions.push('withdraw');
  }
  return actions;
}

function hasSnapshot(imp: ConfigImport): boolean {
  return imp.snapshotPackageId !== null && imp.snapshotPackageId !== undefined;
}

/** Whether the change window of production is open now, in words ('' when no window is set). */
export function windowState(e: Pick<EnvironmentFacts, 'changeWindow' | 'windowOpen'>): string {
  if (e.changeWindow === '') {
    return '';
  }
  return e.windowOpen ? 'Open' : 'Closed';
}

/** The rule of the change window of production, in words. */
export function windowText(e: Pick<EnvironmentFacts, 'changeWindow' | 'windowOpen'>): string {
  if (e.changeWindow === '') {
    return 'No change window is set: imports can be prepared but not applied.';
  }
  return `Imports are applied only in the change window ${e.changeWindow} (now ${windowState(e).toLowerCase()}), with a change request number.`;
}

/** Datasets of the catalogue by group, in catalogue group order. */
export function byGroup(
  datasets: readonly CatalogueDataset[],
  groups: readonly { code: string; name: string }[],
): { code: string; name: string; datasets: CatalogueDataset[] }[] {
  return groups
    .map((g) => ({ ...g, datasets: datasets.filter((d) => d.group === g.code) }))
    .filter((g) => g.datasets.length > 0);
}

/** The datasets an export of "everything" takes: all but the optional and the user datasets. */
export function defaultSelection(
  datasets: readonly CatalogueDataset[],
  includeUsers: boolean,
): string[] {
  return datasets
    .filter((d) => !d.collection && !d.optional && (!d.users || includeUsers))
    .map((d) => d.code);
}

/** The reconciliation of a dataset in words. */
export function reconciliationLabel(d: ImportDataset): { label: string; tone: Tone } {
  if (d.reconciled === null || d.reconciled === undefined) {
    return { label: 'Not applied', tone: 'neutral' };
  }
  return d.reconciled
    ? { label: 'Reconciled', tone: 'success' }
    : { label: 'Differs', tone: 'danger' };
}

/** Total number of changes of an import (added, changed and items only here). */
export function changeCount(imp: Pick<ConfigImport, 'added' | 'changed' | 'onlyInTarget'>): number {
  return imp.added + imp.changed + imp.onlyInTarget;
}

/** One line of the difference table: a field of an item. */
export interface ItemRow {
  id: string;
  first: boolean;
  key: string;
  type: ChangeType;
  field: string;
  from: string;
  to: string;
}

/** The lines of the difference table: one per field, the item named on its first line. */
export function itemRows(items: readonly ItemView[]): ItemRow[] {
  return items.flatMap((item, i) => {
    const fields =
      item.fields.length === 0 ? [{ column: '', label: '—', from: null, to: null }] : item.fields;
    return fields.map((f, j) => ({
      id: `${i}:${j}`,
      first: j === 0,
      key: item.key,
      type: item.type,
      field: f.label,
      from: f.from ?? '—',
      to: f.to ?? '—',
    }));
  });
}

/** One finding of a dry run, for the findings table. */
export interface Finding {
  id: string;
  blocking: boolean;
  dataset: string;
  item: string;
  message: string;
}

/** The findings of a dry run: refusals and blockers first, then warnings and version notes. */
export function findings(m: ImportMessages): Finding[] {
  const rows: Finding[] = [];
  m.refusals.forEach((message, i) =>
    rows.push({ id: `r${i}`, blocking: true, dataset: 'Package', item: '—', message }),
  );
  m.blockers.forEach((b, i) =>
    rows.push({
      id: `b${i}`,
      blocking: true,
      dataset: b.datasetName ?? 'Package',
      item: b.key ?? '—',
      message: b.message,
    }),
  );
  m.warnings.forEach((w, i) =>
    rows.push({
      id: `w${i}`,
      blocking: false,
      dataset: w.datasetName ?? 'Package',
      item: w.key ?? '—',
      message: w.message,
    }),
  );
  m.notes.forEach((message, i) =>
    rows.push({ id: `n${i}`, blocking: false, dataset: 'Package', item: '—', message }),
  );
  return rows;
}

/** Totals of a drift report. */
export function driftTotals(rows: readonly DatasetDrift[]): {
  datasets: number;
  added: number;
  changed: number;
  removed: number;
} {
  return rows.reduce(
    (t, d) => ({
      datasets: t.datasets + (d.added + d.changed + d.removed > 0 ? 1 : 0),
      added: t.added + d.added,
      changed: t.changed + d.changed,
      removed: t.removed + d.removed,
    }),
    { datasets: 0, added: 0, changed: 0, removed: 0 },
  );
}
