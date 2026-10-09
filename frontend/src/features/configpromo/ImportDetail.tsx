import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { CheckCircle2, Download, RefreshCw, Send } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { configPromotionApi } from '@/api/configPromotion';
import type { ConfigImport, ImportDataset } from '@/api/configPromotion';
import { saveFile } from '@/api/client';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { DataTable } from '@/components/ui/DataTable';
import { DefinitionGrid } from '@/components/ui/DefinitionGrid';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Kpi } from '@/components/ui/Kpi';
import { Notice } from '@/components/ui/Notice';
import { PageHeader } from '@/components/ui/PageHeader';
import { RowActionMenu } from '@/components/ui/RowActionMenu';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { UserName } from '@/components/ui/UserName';
import { useToast } from '@/components/ui/toastContext';
import { formatDateTime } from '@/utils/format';
import { DifferenceViewer } from './DifferenceViewer';
import { FindingsTable } from './FindingsTable';
import { importActions, importStatus, reconciliationLabel, shortHash } from './promotion';
import type { ImportAction } from './promotion';

const LIST = '/admin/config-promotion/imports';

/** Choices of a new dry run: datasets to import and datasets whose extra items are deactivated. */
interface Choices {
  skip: string[];
  deactivate: string[];
}

/**
 * One import: the facts, the findings of the dry run, the datasets with their differences, and
 * the actions of the preparer (submit, check again with other choices, withdraw, roll back) and of
 * the approver (approve and apply, reject). After the apply each dataset shows its reconciliation.
 */
export function ImportDetail({ id }: Readonly<{ id: number }>) {
  const { can, user } = useAuth();
  const navigate = useNavigate();
  const imp = useQuery({
    queryKey: ['config-promotion', 'import', id],
    queryFn: () => configPromotionApi.importOf(id),
  });
  const datasets = useQuery({
    queryKey: ['config-promotion', 'import', id, 'datasets'],
    queryFn: () => configPromotionApi.datasets(id),
  });
  const [viewing, setViewing] = useState<ImportDataset | null>(null);
  const [action, setAction] = useState<ImportAction | null>(null);
  const [choices, setChoices] = useState<Choices | null>(null);
  if (imp.data === undefined) {
    return <ErrorAlert error={imp.error} />;
  }
  const i = imp.data;
  const actions = importActions(i, {
    username: user?.username ?? '',
    mayPrepare: can('CONFIG_IMPORT_PREPARE'),
    mayApprove: can('CONFIG_IMPORT_APPROVE'),
  });
  const current: Choices = choices ?? {
    skip: [],
    deactivate: i.options.deactivate,
  };
  const editable = actions.includes('check');
  return (
    <div className="stack">
      <PageHeader
        section="Configuration Promotion"
        title={`Import ${i.importNo}`}
        backTo={LIST}
        description={`Package ${i.packageNo} from ${i.sourceEnvironment}`}
        actions={
          <ActionButtons
            imp={i}
            actions={actions}
            onAction={setAction}
            changedChoices={choices !== null}
          />
        }
      />
      <ImportFacts imp={i} />
      <FindingsTable messages={i.messages} />
      {editable && (
        <Notice tone="info" title="Choose what to import">
          Clear Import to leave a dataset out; tick Deactivate to deactivate the items only in this
          environment (never deleted; refused for items that transactions use). Then Check Again.
        </Notice>
      )}
      <Card title="Datasets" flush>
        <ErrorAlert error={datasets.error} />
        <DataTable<ImportDataset>
          callout="import-datasets"
          loading={datasets.isLoading}
          rows={datasets.data ?? []}
          rowKey={(d) => d.code}
          selectedKey={viewing?.code}
          columns={[
            { key: 'name', header: 'Dataset', render: (d) => <strong>{d.name}</strong> },
            { key: 'added', header: 'Added', numeric: true, width: '90px', render: (d) => d.added },
            {
              key: 'changed',
              header: 'Changed',
              numeric: true,
              width: '90px',
              render: (d) => d.changed,
            },
            {
              key: 'only',
              header: 'Only Here',
              numeric: true,
              width: '100px',
              render: (d) => d.onlyInTarget,
            },
            {
              key: 'same',
              header: 'Unchanged',
              numeric: true,
              width: '100px',
              render: (d) => d.unchanged,
            },
            {
              key: 'import',
              header: 'Import',
              kind: 'center',
              width: '80px',
              render: (d) => (
                <input
                  type="checkbox"
                  aria-label={`Import ${d.name}`}
                  checked={!current.skip.includes(d.code)}
                  disabled={!editable}
                  onChange={() => setChoices(toggle(current, 'skip', d.code))}
                />
              ),
            },
            {
              key: 'deactivate',
              header: 'Deactivate',
              kind: 'center',
              width: '100px',
              render: (d) => (
                <input
                  type="checkbox"
                  aria-label={`Deactivate items of ${d.name} only in this environment`}
                  checked={current.deactivate.includes(d.code)}
                  disabled={!editable || d.collection || d.onlyInTarget === 0}
                  onChange={() => setChoices(toggle(current, 'deactivate', d.code))}
                />
              ),
            },
            {
              key: 'reconciled',
              header: 'Reconciliation',
              kind: 'status',
              width: '160px',
              render: (d) => {
                const r = reconciliationLabel(d);
                return (
                  <span
                    title={`Package ${shortHash(d.packageSha256)}, here ${shortHash(d.targetSha256)}`}
                  >
                    <StatusBadge status={r.label.toUpperCase()} label={r.label} tone={r.tone} />
                  </span>
                );
              },
            },
            {
              key: 'actions',
              header: <span className="visually-hidden">Actions</span>,
              width: '64px',
              render: (d) => (
                <RowActionMenu
                  label={d.name}
                  actions={[{ label: 'View Differences', onSelect: () => setViewing(d) }]}
                />
              ),
            },
          ]}
        />
      </Card>
      {viewing !== null && (
        <DifferenceViewer importId={i.id} dataset={viewing} onClose={() => setViewing(null)} />
      )}
      {action !== null && (
        <ActionDialog
          imp={i}
          action={action}
          options={{
            datasets: (datasets.data ?? [])
              .map((d) => d.code)
              .filter((c) => !current.skip.includes(c)),
            deactivate: current.deactivate,
            includeUsers: i.options.includeUsers,
          }}
          onClose={() => setAction(null)}
          onDone={(next) => {
            setAction(null);
            setChoices(null);
            if (next.id !== i.id) {
              void navigate(`${LIST}?import=${next.id}`);
            }
          }}
        />
      )}
    </div>
  );
}

function toggle(c: Choices, field: keyof Choices, code: string): Choices {
  const list = c[field];
  return { ...c, [field]: list.includes(code) ? list.filter((x) => x !== code) : [...list, code] };
}

function download(packageId: number) {
  void configPromotionApi.file(packageId).then((f) => saveFile(f.blob, f.fileName));
}

const BUTTONS: Record<ImportAction, { label: string; accent?: boolean }> = {
  submit: { label: 'Submit for Approval', accent: true },
  check: { label: 'Check Again' },
  withdraw: { label: 'Withdraw' },
  approve: { label: 'Approve and Apply', accent: true },
  reject: { label: 'Reject' },
  rollback: { label: 'Roll Back' },
};

function ActionButtons({
  imp,
  actions,
  onAction,
  changedChoices,
}: Readonly<{
  imp: ConfigImport;
  actions: ImportAction[];
  onAction: (a: ImportAction) => void;
  changedChoices: boolean;
}>) {
  return (
    <div className="row">
      <Button
        variant="secondary"
        icon={<Download size={16} />}
        onClick={() => download(imp.packageId)}
      >
        Download Package
      </Button>
      {actions.map((a) => (
        <Button
          key={a}
          variant={
            BUTTONS[a].accent === true || (a === 'check' && changedChoices) ? 'accent' : 'secondary'
          }
          icon={iconOf(a)}
          disabled={a === 'submit' && changedChoices}
          onClick={() => onAction(a)}
        >
          {BUTTONS[a].label}
        </Button>
      ))}
    </div>
  );
}

function iconOf(a: ImportAction) {
  if (a === 'submit') {
    return <Send size={16} />;
  }
  if (a === 'approve') {
    return <CheckCircle2 size={16} />;
  }
  return a === 'check' ? <RefreshCw size={16} /> : undefined;
}

const EFFECTS: Record<ImportAction, string> = {
  submit:
    'The import waits for the approval of a second user with the approval right; nothing changes until then.',
  check:
    'The package is compared again with this environment with the chosen datasets and deactivations.',
  withdraw: 'The import is closed without changing this environment.',
  approve:
    'The configuration of this environment is kept as a snapshot, then the package is applied in one step in dependency order and reconciled. A failure keeps nothing.',
  reject: 'The import is closed without changing this environment; the preparer sees the reason.',
  rollback:
    'A new import of the snapshot taken before this import is prepared: it puts back the values this import changed and deactivates the items it added. It is approved like any import.',
};

function ActionDialog({
  imp,
  action,
  options,
  onClose,
  onDone,
}: Readonly<{
  imp: ConfigImport;
  action: ImportAction;
  options: { datasets: string[]; deactivate: string[]; includeUsers: boolean };
  onClose: () => void;
  onDone: (next: ConfigImport) => void;
}>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const act = useMutation({
    mutationFn: (reason: string) => run(imp.id, action, reason, options),
    onSuccess: async (next) => {
      await queryClient.invalidateQueries({ queryKey: ['config-promotion'] });
      toast.success(
        `${BUTTONS[action].label}: ${next.importNo} ${importStatus(next.status).label.toLowerCase()}`,
      );
      onDone(next);
    },
  });
  return (
    <ConfirmDialog
      title={`${BUTTONS[action].label} – ${imp.importNo}`}
      record={`Package ${imp.packageNo} from ${imp.sourceEnvironment}`}
      effect={EFFECTS[action]}
      confirmLabel={BUTTONS[action].label}
      destructive={action === 'reject' || action === 'withdraw'}
      reason={reasonOf(action)}
      busy={act.isPending}
      error={act.error}
      onConfirm={act.mutate}
      onClose={onClose}
    />
  );
}

function reasonOf(action: ImportAction): 'required' | 'optional' | undefined {
  if (action === 'reject') {
    return 'required';
  }
  return action === 'approve' ? 'optional' : undefined;
}

function run(
  id: number,
  action: ImportAction,
  reason: string,
  options: { datasets: string[]; deactivate: string[]; includeUsers: boolean },
): Promise<ConfigImport> {
  switch (action) {
    case 'submit':
      return configPromotionApi.submit(id);
    case 'check':
      return configPromotionApi.check(id, options);
    case 'withdraw':
      return configPromotionApi.cancel(id);
    case 'approve':
      return configPromotionApi.approve(id, reason);
    case 'reject':
      return configPromotionApi.reject(id, reason);
    default:
      return configPromotionApi.rollback(id);
  }
}

/** The facts of an import: the failure of its apply, the counts of the dry run and who did what. */
function ImportFacts({ imp: i }: Readonly<{ imp: ConfigImport }>) {
  const status = importStatus(i.status);
  return (
    <>
      {i.status === 'FAILED' && i.errorMessage !== null && i.errorMessage !== undefined && (
        <Notice tone="error" title="The apply failed; nothing of it was kept">
          {i.errorMessage}
        </Notice>
      )}
      <div className="grid-4">
        <Kpi label="Added" value={i.added} accent />
        <Kpi label="Changed" value={i.changed} />
        <Kpi label="Only in This Environment" value={i.onlyInTarget} />
        <Kpi label="Unchanged" value={i.unchanged} />
      </div>
      <Card
        title="Import"
        actions={<StatusBadge status={i.status} label={status.label} tone={status.tone} />}
      >
        <DefinitionGrid
          columns={2}
          items={[
            { label: 'Package', value: i.packageNo },
            { label: 'From environment', value: i.sourceEnvironment },
            { label: 'Change request', value: i.changeReference ?? '' },
            { label: 'Reason', value: i.reason ?? '' },
            { label: 'Prepared by', value: <UserName login={i.preparedBy} /> },
            { label: 'Prepared on', value: formatDateTime(i.preparedAt) },
            { label: 'Approved by', value: i.decidedBy ? <UserName login={i.decidedBy} /> : '' },
            { label: 'Decided on', value: formatDateTime(i.decidedAt) },
            { label: 'Approver remarks', value: i.decisionNote ?? '', wide: true },
            { label: 'Applied on', value: formatDateTime(i.appliedAt) },
            {
              label: 'Snapshot before the apply',
              value:
                i.snapshotPackageId === null || i.snapshotPackageId === undefined ? (
                  ''
                ) : (
                  <Button
                    variant="ghost"
                    size="sm"
                    icon={<Download size={14} />}
                    onClick={() => download(i.snapshotPackageId ?? 0)}
                  >
                    Download Snapshot
                  </Button>
                ),
            },
          ]}
        />
      </Card>
    </>
  );
}
