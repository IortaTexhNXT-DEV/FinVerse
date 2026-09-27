import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { submittedApi } from '@/api/submitted';
import type { ExtractionView, PolicyData } from '@/api/submitted';
import { LovSelect } from '@/components/broking/LovSelect';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { DataTable } from '@/components/ui/DataTable';
import { DateInput } from '@/components/ui/DateInput';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { FileDropZone } from '@/components/ui/FileDropZone';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDateTime } from '@/utils/format';
import { SBM_LOV, SUBMITTED_SECTION } from '../common/submittedCodes';

/** The fields of a proposal shown side by side with the current record. */
const FIELDS: {
  key: string;
  label: string;
  read: (d: PolicyData) => string | number | null;
  write: (d: PolicyData, v: string) => PolicyData;
}[] = [
  {
    key: 'ASSURED',
    label: 'Assured',
    read: (d) => d.assured.assuredName,
    write: (d, v) => ({ ...d, assured: { ...d.assured, assuredName: v } }),
  },
  {
    key: 'PN_NO',
    label: 'PN No.',
    read: (d) => d.loan.pnNo,
    write: (d, v) => ({ ...d, loan: { ...d.loan, pnNo: v } }),
  },
  {
    key: 'INSURER',
    label: 'Insurer',
    read: (d) => d.terms.insurerCode,
    write: (d, v) => ({ ...d, terms: { ...d.terms, insurerCode: v } }),
  },
  {
    key: 'POLICY_NUMBER',
    label: 'Policy No.',
    read: (d) => d.terms.policyNo,
    write: (d, v) => ({ ...d, terms: { ...d.terms, policyNo: v } }),
  },
  {
    key: 'PERIOD_FROM',
    label: 'Inception',
    read: (d) => d.terms.inceptionDate,
    write: (d, v) => ({ ...d, terms: { ...d.terms, inceptionDate: v } }),
  },
  {
    key: 'PERIOD_TO',
    label: 'Expiry',
    read: (d) => d.terms.expiryDate,
    write: (d, v) => ({ ...d, terms: { ...d.terms, expiryDate: v } }),
  },
  {
    key: 'SUM_INSURED',
    label: 'Sum Insured',
    read: (d) => d.terms.sumInsured,
    write: (d, v) => ({ ...d, terms: { ...d.terms, sumInsured: v === '' ? null : Number(v) } }),
  },
  {
    key: 'UNIT',
    label: 'Unit',
    read: (d) => d.risk.unitDescription,
    write: (d, v) => ({ ...d, risk: { ...d.risk, unitDescription: v } }),
  },
  {
    key: 'PLATE_NO',
    label: 'Plate No.',
    read: (d) => d.risk.plateNo,
    write: (d, v) => ({ ...d, risk: { ...d.risk, plateNo: v } }),
  },
  {
    key: 'LOCATION',
    label: 'Location',
    read: (d) => d.risk.propertyLocation,
    write: (d, v) => ({ ...d, risk: { ...d.risk, propertyLocation: v } }),
  },
];

function Review({ x, onDone }: Readonly<{ x: ExtractionView; onDone: () => void }>) {
  const toast = useToast();
  const [data, setData] = useState<PolicyData>(x.proposed);
  const [rejecting, setRejecting] = useState(false);
  const confirm = useMutation({
    mutationFn: () => submittedApi.confirm(x.id, data),
    onSuccess: (p) => {
      toast.success(`${p.row.sbmNo} saved from ${x.extractionNo}`);
      onDone();
    },
  });
  const reject = useMutation({
    mutationFn: (reason: string) => submittedApi.reject(x.id, reason),
    onSuccess: () => {
      setRejecting(false);
      onDone();
    },
  });
  const decided = x.decidedAt !== null;
  return (
    <Card
      title={`${x.extractionNo} · ${x.fileName}`}
      actions={
        !decided && (
          <>
            <Button variant="secondary" onClick={() => setRejecting(true)}>
              Reject
            </Button>
            <Button disabled={confirm.isPending} onClick={() => confirm.mutate()}>
              Confirm
            </Button>
          </>
        )
      }
    >
      <ErrorAlert error={confirm.error} />
      {!x.readable && (
        <p className="muted">The document has no readable text: enter the fields by hand.</p>
      )}
      {x.note && <p className="muted">{x.note}</p>}
      <DataTable
        rows={FIELDS}
        rowKey={(f) => f.key}
        columns={[
          { key: 'field', header: 'Field', render: (f) => f.label },
          {
            key: 'proposed',
            header: 'Proposed',
            render: (f) =>
              f.key.startsWith('PERIOD_') ? (
                <DateInput
                  aria-label={f.label}
                  disabled={decided}
                  value={String(f.read(data) ?? '')}
                  onChange={(e) => setData(f.write(data, e.target.value))}
                />
              ) : (
                <input
                  className="input"
                  aria-label={f.label}
                  disabled={decided}
                  value={String(f.read(data) ?? '')}
                  onChange={(e) => setData(f.write(data, e.target.value))}
                />
              ),
          },
          {
            key: 'confidence',
            header: 'Confidence',
            kind: 'amount',
            render: (f) => {
              const c = x.fields[f.key]?.confidence;
              return c === null || c === undefined ? '—' : `${String(Math.round(c * 100))}%`;
            },
          },
          {
            key: 'current',
            header: 'Current Record',
            render: (f) => (x.current ? String(f.read(x.current) ?? '—') : '—'),
          },
        ]}
      />
      {rejecting && (
        <ConfirmDialog
          title="Reject Proposal"
          record={x.extractionNo}
          effect="The proposal is closed; the masterlist is not changed."
          confirmLabel="Reject"
          reason="required"
          destructive
          busy={reject.isPending}
          error={reject.error}
          onConfirm={(reason) => reject.mutate(reason)}
          onClose={() => setRejecting(false)}
        />
      )}
    </Card>
  );
}

/**
 * Extraction Review (FR-SP-002): policy documents read by extraction, each field proposed with its
 * confidence next to the current record, confirmed or rejected by the handler.
 */
export default function ExtractionPage() {
  const companyId = useCompanyId();
  const queryClient = useQueryClient();
  const toast = useToast();
  const [tab, setTab] = useState<'open' | 'decided'>('open');
  const [open, setOpen] = useState<number>();
  const [segment, setSegment] = useState('NONCBG_CORPORATE');
  const [businessType, setBusinessType] = useState('NB');
  const list = useQuery({
    queryKey: ['submitted', 'extractions', companyId, tab],
    queryFn: () => submittedApi.extractions(companyId, tab === 'decided'),
    enabled: companyId > 0,
  });
  const upload = useMutation({
    mutationFn: (file: File) => submittedApi.extract(companyId, file, segment, businessType),
    onSuccess: (x) => {
      toast.success(`${x.extractionNo} proposed`);
      setOpen(x.id);
      void queryClient.invalidateQueries({ queryKey: ['submitted', 'extractions'] });
    },
  });
  const rows = list.data?.content ?? [];
  const selected = rows.find((x) => x.id === open);
  return (
    <div className="stack">
      <PageHeader
        section={SUBMITTED_SECTION}
        title="Extraction Review"
        description="Policy documents read by extraction, confirmed or rejected field by field."
      />
      <Card title="Upload a Policy Document">
        <ErrorAlert error={upload.error} />
        <div className="form-grid">
          <Field label="Segment" required>
            {(id) => (
              <LovSelect id={id} type={SBM_LOV.segment} value={segment} onChange={setSegment} />
            )}
          </Field>
          <Field label="Business Type" required>
            {(id) => (
              <select
                id={id}
                className="select"
                value={businessType}
                onChange={(e) => setBusinessType(e.target.value)}
              >
                <option value="NB">New business</option>
                <option value="RB">Renewal business</option>
              </select>
            )}
          </Field>
        </div>
        <FileDropZone
          accept="application/pdf"
          busy={upload.isPending}
          label="Drop the policy PDF here or browse"
          onChange={(files) => {
            const file = files[0];
            if (file) {
              upload.mutate(file);
            }
          }}
        />
      </Card>
      <Tabs
        tabs={[
          { id: 'open', label: 'To Confirm' },
          { id: 'decided', label: 'Decided' },
        ]}
        active={tab}
        onChange={(t) => {
          setTab(t);
          setOpen(undefined);
        }}
      />
      <ErrorAlert error={list.error} onRetry={() => void list.refetch()} />
      <Card flush>
        <DataTable<ExtractionView>
          loading={list.isLoading}
          rows={rows}
          rowKey={(x) => x.id}
          selectedKey={open}
          onRowClick={(x) => setOpen(x.id)}
          emptyMessage={tab === 'open' ? 'No proposal to confirm' : 'No proposal decided'}
          columns={[
            { key: 'no', header: 'Extraction', kind: 'code', render: (x) => x.extractionNo },
            { key: 'file', header: 'Document', render: (x) => x.fileName },
            {
              key: 'status',
              header: 'Status',
              kind: 'status',
              render: (x) => <StatusBadge status={x.status} />,
            },
            {
              key: 'at',
              header: 'Uploaded',
              kind: 'datetime',
              render: (x) => formatDateTime(x.createdAt),
            },
          ]}
        />
      </Card>
      {selected && (
        <Review
          key={selected.id}
          x={selected}
          onDone={() => {
            setOpen(undefined);
            void queryClient.invalidateQueries({ queryKey: ['submitted'] });
          }}
        />
      )}
    </div>
  );
}
