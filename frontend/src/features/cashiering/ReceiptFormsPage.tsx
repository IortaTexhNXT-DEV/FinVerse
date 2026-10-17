import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { PenLine } from 'lucide-react';
import { useState } from 'react';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { CellStack } from '@/components/ui/CellStack';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { RowActionMenu } from '@/components/ui/RowActionMenu';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { UserName } from '@/components/ui/UserName';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate, formatDateTime } from '@/utils/format';
import { TextField } from './CashFields';
import type { FormKind, FormText, ReceiptFormVersion } from './printApi';
import { formInUse, printApi } from './printApi';

const KINDS: readonly { id: FormKind; label: string }[] = [
  { id: 'AR', label: 'AR Form' },
  { id: 'OR', label: 'OR Form' },
];

const STATUS_LABELS: Record<ReceiptFormVersion['status'], string> = {
  PENDING_APPROVAL: 'For approval',
  APPROVED: 'Approved',
  REJECTED: 'Rejected',
};

const FIELDS: readonly { key: keyof FormText; label: string; hint?: string }[] = [
  { key: 'companyName', label: 'Company Name', hint: 'Blank for the company record' },
  { key: 'companyDescription', label: 'Company Description' },
  { key: 'companyAddress', label: 'Company Address', hint: 'Blank for the company record' },
  { key: 'companyVat', label: 'Company VAT TIN', hint: 'Blank for the company record' },
  { key: 'noteLine', label: 'Note Line (AR)' },
  { key: 'footer1', label: 'Footer Line 1' },
  { key: 'footer2', label: 'Footer Line 2' },
  { key: 'footer3', label: 'Footer Line 3' },
  { key: 'footer4', label: 'Footer Line 4' },
];

const today = () => new Date().toISOString().slice(0, 10);

function FormEditor({
  kind,
  start,
  busy,
  error,
  onSave,
  onClose,
}: Readonly<{
  kind: FormKind;
  start: FormText;
  busy: boolean;
  error: unknown;
  onSave: (text: FormText, effectiveFrom: string) => void;
  onClose: () => void;
}>) {
  const [text, setText] = useState<FormText>(start);
  const [effective, setEffective] = useState(today());
  return (
    <Modal
      open
      size="lg"
      title={`Change ${kind} Form`}
      onClose={onClose}
      helper="Tokens: {COMPANY_NAME}, {CERTIFICATE_NO}, {PRINT_DATE}, {SERIES_RANGE}, {COPY_LABEL}. The change is printed from its effective date once another authorised user approves it."
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button busy={busy} onClick={() => onSave(text, effective)}>
            Submit for Approval
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={error} />
        <div className="form-grid">
          {FIELDS.filter((f) => kind === 'AR' || f.key !== 'noteLine').map((f) => (
            <TextField
              key={f.key}
              label={f.label}
              hint={f.hint}
              value={text[f.key] ?? ''}
              maxLength={f.key.startsWith('company') ? 300 : 500}
              onChange={(v) => setText((t) => ({ ...t, [f.key]: v }))}
            />
          ))}
          <TextField
            label="Effective From"
            type="date"
            required
            value={effective}
            onChange={setEffective}
          />
        </div>
      </div>
    </Modal>
  );
}

/**
 * AR and OR Forms (FRS.CSH.02.06.01 to 02.06.03): the header, note and footer lines of the forms
 * with their versions and effective dates; a change waits for the approval of another authorised
 * user before it is printed.
 */
export default function ReceiptFormsPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [kind, setKind] = useState<FormKind>('AR');
  const [editing, setEditing] = useState(false);
  const [rejecting, setRejecting] = useState<ReceiptFormVersion>();
  const list = useQuery({
    queryKey: ['cashiering', 'receipt-forms', companyId],
    queryFn: () => printApi.forms(companyId),
    enabled: companyId > 0,
  });
  const done = async (v: ReceiptFormVersion) => {
    setEditing(false);
    setRejecting(undefined);
    toast.success(`${v.formKind} form version ${v.versionNo}: ${STATUS_LABELS[v.status]}`);
    await queryClient.invalidateQueries({ queryKey: ['cashiering', 'receipt-forms'] });
  };
  const act = useMutation({
    mutationFn: (fn: () => Promise<ReceiptFormVersion>) => fn(),
    onSuccess: done,
  });
  const versions = (list.data ?? []).filter((v) => v.formKind === kind);
  const inUse = formInUse(list.data ?? [], kind, today());
  const approver = can('MASTER_AUTHORIZE') || can('CASH_SERIES_MANAGE');
  const columns: Column<ReceiptFormVersion>[] = [
    { key: 'version', header: 'Version', render: (v) => <strong>{v.versionNo}</strong> },
    { key: 'effective', header: 'Effective From', render: (v) => formatDate(v.effectiveFrom) },
    { key: 'footer', header: 'Footer Line 1', truncate: true, render: (v) => v.text.footer1 ?? '' },
    {
      key: 'by',
      header: 'Changed By',
      render: (v) => (
        <CellStack main={<UserName login={v.createdBy} />} sub={formatDateTime(v.createdAt)} />
      ),
    },
    {
      key: 'decided',
      header: 'Approved / Rejected By',
      render: (v) => (v.decidedBy ? <UserName login={v.decidedBy} /> : ''),
    },
    {
      key: 'status',
      header: 'Status',
      render: (v) => (
        <StatusBadge
          status={v.status}
          label={v.id === inUse?.id ? 'In use' : STATUS_LABELS[v.status]}
        />
      ),
    },
    {
      key: 'actions',
      header: '',
      width: '56px',
      render: (v) => (
        <RowActionMenu
          label={`version ${v.versionNo}`}
          actions={[
            {
              label: 'Approve',
              disabled: v.status !== 'PENDING_APPROVAL' || !approver,
              onSelect: () => act.mutate(() => printApi.approveForm(v.id)),
            },
            {
              label: 'Reject',
              danger: true,
              disabled: v.status !== 'PENDING_APPROVAL' || !approver,
              onSelect: () => setRejecting(v),
            },
          ]}
        />
      ),
    },
  ];
  return (
    <div className="stack">
      <PageHeader
        section="Cashiering"
        title="AR and OR Forms"
        description="Header, note and footer lines of the printed receipts, with their versions."
        actions={
          can('CASH_SERIES_MANAGE') && (
            <Button variant="accent" icon={<PenLine size={16} />} onClick={() => setEditing(true)}>
              Change {kind} Form
            </Button>
          )
        }
      />
      <ErrorAlert error={list.error ?? act.error} />
      <Tabs tabs={KINDS} active={kind} onChange={setKind} />
      <Card title={`${kind} Form Versions`} flush>
        <DataTable
          caption={`${kind} form versions`}
          columns={columns}
          rows={versions}
          rowKey={(v) => v.id}
          loading={list.isLoading}
          emptyMessage="No version of this form yet"
        />
      </Card>
      {editing && (
        <FormEditor
          key={kind}
          kind={kind}
          start={inUse?.text ?? {}}
          busy={act.isPending}
          error={act.error}
          onClose={() => setEditing(false)}
          onSave={(text, effectiveFrom) =>
            act.mutate(() => printApi.proposeForm(companyId, kind, text, effectiveFrom))
          }
        />
      )}
      {rejecting && (
        <ConfirmDialog
          title={`Reject ${rejecting.formKind} Form Version ${rejecting.versionNo}`}
          effect="The change is not printed; the version in use stays."
          confirmLabel="Reject Version"
          reason="required"
          busy={act.isPending}
          error={act.error}
          onClose={() => setRejecting(undefined)}
          onConfirm={(reason) => act.mutate(() => printApi.rejectForm(rejecting.id, reason))}
        />
      )}
    </div>
  );
}
