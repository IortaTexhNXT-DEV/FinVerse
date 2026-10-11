import { Pencil, Plus, Send, Trash2 } from 'lucide-react';
import { useState } from 'react';
import type { ProgrammeView } from '@/api/eb';
import { ebMarketApi } from '@/api/ebMarket';
import type { Tor, TorItem, TorItemInput } from '@/api/ebMarket';
import { useAuth } from '@/auth/authContext';
import { LovSelect } from '@/components/broking/LovSelect';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Modal } from '@/components/ui/Modal';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { UserName } from '@/components/ui/UserName';
import { formatDateTime } from '@/utils/format';
import { DialogFooter } from '../common/DialogFooter';
import { EbLov } from '../common/EbLabels';
import { EB_LOV } from '../common/ebCodes';
import { orNone } from '../common/formValues';
import { useCycleList } from '../common/useCycleList';
import { useEbMutation } from '../common/useEbMutation';
import { Notice } from '@/components/ui/Notice';

const BLANK: TorItemInput = { benefitLine: '', planCode: '', description: '', requirement: '' };

/** Whether a TOR row is complete. */
function torRowComplete(r: TorItemInput): boolean {
  return r.benefitLine !== '' && r.description.trim() !== '' && r.requirement.trim() !== '';
}

/** Edit TOR: the requirements the insurers answer, one row per benefit, plan and requirement. */
function EditDialog({
  cycleId,
  items,
  onClose,
}: Readonly<{ cycleId: number; items: TorItem[]; onClose: () => void }>) {
  const [rows, setRows] = useState<TorItemInput[]>(
    items.length > 0
      ? items.map((i) => ({
          benefitLine: i.benefitLine,
          planCode: i.planCode ?? '',
          description: i.description,
          requirement: i.requirement,
        }))
      : [BLANK],
  );
  const [submitted, setSubmitted] = useState(false);
  const save = useEbMutation(
    (companyId, v: TorItemInput[]) => ebMarketApi.saveTor(companyId, cycleId, v),
    (t: Tor) => `TOR version ${String(t.versionNo)} saved`,
    onClose,
  );
  const set = (index: number, patch: Partial<TorItemInput>) =>
    setRows(rows.map((r, i) => (i === index ? { ...r, ...patch } : r)));
  const complete = rows.length > 0 && rows.every(torRowComplete);
  const submit = () => {
    setSubmitted(true);
    if (complete) {
      save.mutate(rows.map((r) => ({ ...r, planCode: orNone(r.planCode) })));
    }
  };
  return (
    <Modal
      open
      title="Terms of Reference"
      onClose={onClose}
      footer={<DialogFooter busy={save.isPending} label="Save" onClose={onClose} onSave={submit} />}
    >
      <div className="stack">
        <ErrorAlert error={save.error} />
        {submitted && !complete && (
          <Notice tone="error">Each row needs its benefit line, item and requirement</Notice>
        )}
        <table className="table eb-edit-table">
          <thead>
            <tr>
              <th>Benefit Line</th>
              <th>Plan</th>
              <th>Item</th>
              <th>Requirement</th>
              <th aria-label="Remove" />
            </tr>
          </thead>
          <tbody>
            {rows.map((r, i) => (
              <tr key={i}>
                <td>
                  <LovSelect
                    id={`tor-line-${String(i)}`}
                    type={EB_LOV.benefitLine}
                    value={r.benefitLine}
                    onChange={(v) => set(i, { benefitLine: v })}
                  />
                </td>
                <td>
                  <input
                    className="input"
                    aria-label={`Plan ${String(i + 1)}`}
                    value={r.planCode ?? ''}
                    onChange={(e) => set(i, { planCode: e.target.value })}
                  />
                </td>
                <td>
                  <input
                    className="input"
                    aria-label={`Item ${String(i + 1)}`}
                    value={r.description}
                    onChange={(e) => set(i, { description: e.target.value })}
                  />
                </td>
                <td>
                  <input
                    className="input"
                    aria-label={`Requirement ${String(i + 1)}`}
                    value={r.requirement}
                    onChange={(e) => set(i, { requirement: e.target.value })}
                  />
                </td>
                <td>
                  <Button
                    variant="ghost"
                    size="sm"
                    aria-label={`Remove row ${String(i + 1)}`}
                    icon={<Trash2 size={14} />}
                    onClick={() => setRows(rows.filter((_, j) => j !== i))}
                  />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        <div>
          <Button
            variant="secondary"
            size="sm"
            icon={<Plus size={14} />}
            onClick={() => setRows([...rows, BLANK])}
          >
            Add Row
          </Button>
        </div>
      </div>
    </Modal>
  );
}

const COLUMNS: Column<TorItem>[] = [
  {
    key: 'line',
    header: 'Benefit Line',
    render: (i) => <EbLov type={EB_LOV.benefitLine} code={i.benefitLine} />,
  },
  { key: 'plan', header: 'Plan', kind: 'code', render: (i) => i.planCode ?? '' },
  { key: 'item', header: 'Item', render: (i) => i.description },
  { key: 'req', header: 'Requirement', render: (i) => i.requirement },
];

/** The latest version of the TOR. */
function latestOf(tors: Tor[]): Tor | undefined {
  return tors.reduce<Tor | undefined>(
    (a, t) => (a === undefined || t.versionNo > a.versionNo ? t : a),
    undefined,
  );
}

/** Status and release of the TOR version shown. */
function TorStatus({ tor }: Readonly<{ tor: Tor | undefined }>) {
  if (!tor) {
    return null;
  }
  return (
    <p className="muted">
      <StatusBadge status={tor.status} />{' '}
      {tor.releasedAt && (
        <>
          Released {formatDateTime(tor.releasedAt)} by <UserName login={tor.releasedBy} />
        </>
      )}
    </p>
  );
}

/** Edit TOR, and Release TOR while the latest version is a draft. */
function TorActions({
  draft,
  onMode,
}: Readonly<{ draft: boolean; onMode: (m: 'edit' | 'release') => void }>) {
  return (
    <>
      <Button
        variant="secondary"
        size="sm"
        icon={<Pencil size={14} />}
        onClick={() => onMode('edit')}
      >
        Edit TOR
      </Button>
      {draft && (
        <Button size="sm" icon={<Send size={14} />} onClick={() => onMode('release')}>
          Release TOR
        </Button>
      )}
    </>
  );
}

/** Edit TOR or Release TOR. */
function TorDialogs({
  mode,
  cycleId,
  latest,
  title,
  onClose,
}: Readonly<{
  mode: 'edit' | 'release';
  cycleId: number;
  latest: Tor | undefined;
  title: string;
  onClose: () => void;
}>) {
  const release = useEbMutation(
    (c, id: number) => ebMarketApi.releaseTor(c, id),
    (t: Tor) => `TOR version ${String(t.versionNo)} released`,
    onClose,
  );
  if (mode === 'edit') {
    const items = latest && latest.status !== 'SUPERSEDED' ? latest.items : [];
    return <EditDialog cycleId={cycleId} items={items} onClose={onClose} />;
  }
  return (
    <ConfirmDialog
      title="Release TOR"
      record={title}
      effect="The TOR is frozen; later changes make a new version."
      confirmLabel="Release"
      busy={release.isPending}
      error={release.error}
      onConfirm={() => release.mutate(cycleId)}
      onClose={onClose}
    />
  );
}

/**
 * The Terms of Reference of the current cycle: the latest version with its items; the AO edits
 * the draft and releases it before the requests go to the insurers.
 */
export function TorCard({ programme }: Readonly<{ programme: ProgrammeView }>) {
  const { can } = useAuth();
  const cycleId = programme.currentCycleId ?? undefined;
  const [mode, setMode] = useState<'edit' | 'release'>();
  const close = () => setMode(undefined);
  const tors = useCycleList('tor', cycleId, ebMarketApi.tors);
  const latest = latestOf(tors.rows);
  const market = can('EB_MARKET') && cycleId !== undefined;
  const title = latest
    ? `Terms of Reference – Version ${String(latest.versionNo)}`
    : 'Terms of Reference';
  return (
    <Card
      title={title}
      actions={market && <TorActions draft={latest?.status === 'DRAFT'} onMode={setMode} />}
    >
      <ErrorAlert error={tors.error} onRetry={() => void tors.refetch()} />
      <TorStatus tor={latest} />
      <DataTable<TorItem>
        loading={tors.isLoading}
        rows={latest?.items ?? []}
        rowKey={(i) => i.id}
        columns={COLUMNS}
        emptyMessage="No terms of reference yet"
      />
      {mode !== undefined && cycleId !== undefined && (
        <TorDialogs mode={mode} cycleId={cycleId} latest={latest} title={title} onClose={close} />
      )}
    </Card>
  );
}
