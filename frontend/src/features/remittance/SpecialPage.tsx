import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/auth/authContext';
import { LovSelect } from '@/components/broking/LovSelect';
import { WorklistToolbar } from '@/components/broking/WorklistToolbar';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate } from '@/utils/format';
import { remittanceApi } from './api';
import type { Special } from './api';
import { TemplateButton, UploadForm } from './RemittanceParts';
import { SPECIAL_TABS, SPECIAL_TEMPLATE, stagesOf } from './remittanceLabels';
import type { SpecialTab } from './remittanceLabels';
import './remittance.css';

const COLUMNS: Column<Special>[] = [
  {
    key: 'no',
    header: 'Request No.',
    render: (s) => (
      <>
        <strong>{s.requestNo}</strong>
        <div className="remit-muted">{formatDate(s.createdAt)}</div>
      </>
    ),
  },
  { key: 'inv', header: 'Invoice No.', render: (s) => s.invoiceNo },
  { key: 'assured', header: 'Name of Assured', render: (s) => s.assuredName },
  { key: 'ins', header: 'Insurer', render: (s) => s.insurerCode },
  { key: 'cond', header: 'Condition', render: (s) => s.conditionCode },
  { key: 'by', header: 'Requestor', render: (s) => s.requestedBy },
  { key: 'batch', header: 'Batch', render: (s) => s.batchNo ?? '' },
  { key: 'stage', header: 'Status', render: (s) => <StatusBadge status={s.stage} /> },
];

/** A special remittance request (MKTID.009): invoice, condition and remarks. */
function NewSpecialDialog({
  busy,
  error,
  onClose,
  onSave,
}: Readonly<{
  busy: boolean;
  error: unknown;
  onClose: () => void;
  onSave: (v: { invoiceNo: string; conditionCode: string; remarks?: string }) => void;
}>) {
  const [invoiceNo, setInvoiceNo] = useState('');
  const [condition, setCondition] = useState('');
  const [remarks, setRemarks] = useState('');
  const [errors, setErrors] = useState<{ invoiceNo?: string; condition?: string }>({});
  const save = () => {
    const next = {
      invoiceNo: invoiceNo.trim() === '' ? 'Invoice No. is required' : undefined,
      condition: condition === '' ? 'Select a condition' : undefined,
    };
    setErrors(next);
    if (next.invoiceNo === undefined && next.condition === undefined) {
      onSave({
        invoiceNo: invoiceNo.trim(),
        conditionCode: condition,
        remarks: remarks.trim() || undefined,
      });
    }
  };
  return (
    <Modal
      title="New Special Remittance"
      open
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button busy={busy} onClick={save}>
            Submit Request
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={error} />
        <p>The request is checked at once: the invoice must be paid, cleared and not on hold.</p>
        <Field label="Invoice No." required error={errors.invoiceNo}>
          {(id) => (
            <input
              id={id}
              className="input"
              maxLength={40}
              value={invoiceNo}
              onChange={(e) => setInvoiceNo(e.target.value)}
            />
          )}
        </Field>
        <Field label="Condition" required error={errors.condition}>
          {(id) => (
            <LovSelect
              id={id}
              type="SPECIAL_REMIT_CONDITION"
              value={condition}
              onChange={setCondition}
              required
            />
          )}
        </Field>
        <Field label="Remarks">
          {(id) => (
            <textarea
              id={id}
              className="textarea"
              rows={3}
              maxLength={500}
              value={remarks}
              onChange={(e) => setRemarks(e.target.value)}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}

/**
 * Special remittance (MKTID.009, RMTID.030/033): requests from Marketing to remit an invoice
 * outside the schedule (claims, renewal, installment due, immediate OR). Validated at once,
 * approved by the Remittance team leader, then remitted through their own special batch.
 */
export default function SpecialPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [tab, setTab] = useState<SpecialTab>('APPROVAL');
  const [query, setQuery] = useState('');
  const [page, setPage] = useState(0);
  const [creating, setCreating] = useState(false);
  const [uploading, setUploading] = useState(false);
  const rows = useQuery({
    queryKey: ['remittance', 'specials', companyId, tab, query, page],
    queryFn: () => remittanceApi.specials(companyId, stagesOf(SPECIAL_TABS, tab), query, page),
    enabled: companyId > 0,
  });
  const create = useMutation({
    mutationFn: (v: { invoiceNo: string; conditionCode: string; remarks?: string }) =>
      remittanceApi.createSpecial({ companyId, ...v }),
    onSuccess: async (s) => {
      setCreating(false);
      await queryClient.invalidateQueries({ queryKey: ['remittance'] });
      toast.success(`${s.requestNo} sent for approval`);
      void navigate(`/remittance/special/${s.id}`);
    },
  });
  const upload = useMutation({
    mutationFn: (file: File) => remittanceApi.uploadSpecials(file),
    onSuccess: async (run) => {
      setUploading(false);
      await queryClient.invalidateQueries({ queryKey: ['remittance'] });
      toast.success(`${run.runNo}: ${run.message ?? run.status}`);
    },
  });
  return (
    <div className="stack">
      <PageHeader
        section="Remittance"
        title="Special Remittance"
        description="Invoices Marketing asks to remit outside the regular schedule, from request to Disbursement."
        actions={
          can('SPECIAL_REMIT_REQUEST') ? (
            <>
              <Button variant="secondary" onClick={() => setUploading(!uploading)}>
                Upload Request File
              </Button>
              <Button icon={<Plus size={16} />} onClick={() => setCreating(true)}>
                New Special Remittance
              </Button>
            </>
          ) : undefined
        }
      />
      <ErrorAlert error={rows.error ?? upload.error} />
      {uploading && (
        <Card
          title="Upload Collection Special Remittance File"
          actions={
            <TemplateButton name="special-remittance-template.csv" content={SPECIAL_TEMPLATE} />
          }
        >
          <UploadForm
            label="Request file (invoiceNo, conditionCode, remarks)"
            busy={upload.isPending}
            onUpload={(f) => upload.mutate(f)}
          />
        </Card>
      )}
      <Card flush>
        <div>
          <Tabs
            tabs={SPECIAL_TABS}
            active={tab}
            onChange={(t) => {
              setTab(t);
              setPage(0);
            }}
          />
          <WorklistToolbar
            placeholder="Search Request or Invoice No."
            onSearch={(text) => {
              setQuery(text);
              setPage(0);
            }}
          />
          <DataTable
            caption="Special remittance requests"
            columns={COLUMNS}
            rows={rows.data?.content ?? []}
            rowKey={(s) => s.id}
            loading={rows.isLoading}
            onRowClick={(s) => void navigate(`/remittance/special/${s.id}`)}
          />
          <PageFooter data={rows.data} noun="requests" onPage={setPage} />
        </div>
      </Card>
      {creating && (
        <NewSpecialDialog
          busy={create.isPending}
          error={create.error}
          onClose={() => setCreating(false)}
          onSave={(v) => create.mutate(v)}
        />
      )}
    </div>
  );
}
