import { PeriodCell } from '@/components/ui/PeriodCell';
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { attachmentsApi } from '@/api/attachments';
import type { Soa, SoaFilters } from '@/api/ebMarket';
import { ebServiceApi } from '@/api/ebService';
import { useAuth } from '@/auth/authContext';
import { LovSelect } from '@/components/broking/LovSelect';
import { useFileDownload } from '@/components/broking/useFileDownload';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { CellStack } from '@/components/ui/CellStack';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { DefinitionGrid } from '@/components/ui/DefinitionGrid';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { PageFooter } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { UserName } from '@/components/ui/UserName';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate, formatDateTime } from '@/utils/format';
import { DialogFooter } from '../common/DialogFooter';
import { EB_LOV } from '../common/ebCodes';
import { useEbMutation } from '../common/useEbMutation';

/** Reject SOA: the reason from the list and remarks. */
function RejectDialog({ soa, onClose }: Readonly<{ soa: Soa; onClose: () => void }>) {
  const [reason, setReason] = useState('');
  const [remarks, setRemarks] = useState('');
  const [submitted, setSubmitted] = useState(false);
  const reject = useEbMutation(
    (c, v: { reason: string; remarks: string }) =>
      ebServiceApi.rejectSoa(c, soa.id, v.reason, v.remarks || undefined),
    `SOA ${soa.soaNo} rejected`,
    onClose,
  );
  const save = () => {
    setSubmitted(true);
    if (reason !== '') {
      reject.mutate({ reason, remarks });
    }
  };
  return (
    <Modal
      open
      title={`Reject SOA ${soa.soaNo}`}
      onClose={onClose}
      footer={
        <DialogFooter busy={reject.isPending} label="Reject" onClose={onClose} onSave={save} />
      }
    >
      <div className="stack">
        <ErrorAlert error={reject.error} />
        <Field
          label="Reason"
          required
          error={submitted && reason === '' ? 'Select the reason' : undefined}
        >
          {(id) => (
            <LovSelect id={id} type={EB_LOV.soaReject} value={reason} onChange={setReason} />
          )}
        </Field>
        <Field label="Remarks">
          {(id) => (
            <input
              id={id}
              className="input"
              value={remarks}
              onChange={(e) => setRemarks(e.target.value)}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}

/** The facts of an SOA and its invoices with their payment status. */
function SoaFacts({ s }: Readonly<{ s: Soa }>) {
  return (
    <>
      <DefinitionGrid
        columns={2}
        items={[
          { label: 'Status', value: <StatusBadge status={s.status} /> },
          { label: 'Insurer', value: s.insurerName },
          { label: "Insurer's SOA No.", value: s.insurerSoaNo },
          { label: 'Programme', value: s.programmeNo ?? '' },
          { label: 'Client', value: s.clientName ?? '' },
          {
            label: 'Period',
            value: `${formatDate(s.periodFrom)} – ${formatDate(s.periodTo)}`,
          },
          { label: 'Amount', value: <Amount value={s.amount} /> },
          { label: 'Received On', value: formatDate(s.receivedOn) },
          {
            label: 'Validated By',
            value: s.validatedBy ? <UserName login={s.validatedBy} /> : '',
          },
          { label: 'Released', value: formatDateTime(s.releasedAt) },
          { label: 'Rejection', value: s.rejectReason ?? '' },
          { label: 'Remarks', value: s.remarks ?? '' },
        ]}
      />
      <DataTable<Soa['invoices'][number]>
        rows={s.invoices}
        rowKey={(i) => i.invoiceNo}
        columns={[
          { key: 'no', header: 'Invoice', kind: 'code', render: (i) => i.invoiceNo },
          {
            key: 'pay',
            header: 'Payment',
            kind: 'status',
            render: (i) => <StatusBadge status={i.paymentStatus} />,
          },
        ]}
        emptyMessage="No invoice linked"
      />
    </>
  );
}

/** Download, and for Processing Reject, Validate or Release as the status allows. */
function SoaActions({
  s,
  process,
  busy,
  onDownload,
  onReject,
  onAct,
}: Readonly<{
  s: Soa;
  process: boolean;
  busy: boolean;
  onDownload: () => void;
  onReject: () => void;
  onAct: (action: 'validate' | 'release') => void;
}>) {
  return (
    <>
      <Button variant="secondary" onClick={onDownload}>
        Download SOA
      </Button>
      {process && s.status === 'RECEIVED' && (
        <>
          <Button variant="secondary" onClick={onReject}>
            Reject
          </Button>
          <Button busy={busy} onClick={() => onAct('validate')}>
            Validate
          </Button>
        </>
      )}
      {process && s.status === 'VALIDATED' && (
        <Button busy={busy} onClick={() => onAct('release')}>
          Release
        </Button>
      )}
    </>
  );
}

/** An SOA with its invoices and payment status; Validate, Reject and Release for Processing. */
export function SoaDetail({ id, onClose }: Readonly<{ id: number; onClose: () => void }>) {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const download = useFileDownload();
  const [rejecting, setRejecting] = useState(false);
  const soa = useQuery({
    queryKey: ['eb', 'soa', id],
    queryFn: () => ebServiceApi.soa(companyId, id),
  });
  const act = useEbMutation(
    (c, action: 'validate' | 'release') =>
      action === 'validate' ? ebServiceApi.validateSoa(c, id) : ebServiceApi.releaseSoa(c, id),
    (s: Soa) =>
      `SOA ${s.soaNo} ${s.status === 'RELEASED' ? 'released to the client and Collection' : 'validated'}`,
    () => undefined,
  );
  const s = soa.data;
  const footer = s && (
    <SoaActions
      s={s}
      process={can('EB_PROCESS')}
      busy={act.isPending}
      onDownload={() => download.mutate(() => attachmentsApi.download(s.attachmentId))}
      onReject={() => setRejecting(true)}
      onAct={(a) => act.mutate(a)}
    />
  );
  return (
    <Modal open title={s ? `SOA ${s.soaNo}` : 'SOA'} onClose={onClose} footer={footer}>
      <div className="stack">
        <ErrorAlert
          error={soa.error ?? act.error ?? download.error}
          onRetry={() => void soa.refetch()}
        />
        {s && <SoaFacts s={s} />}
      </div>
      {s && rejecting && <RejectDialog soa={s} onClose={() => setRejecting(false)} />}
    </Modal>
  );
}

function columns(showProgramme: boolean): Column<Soa>[] {
  const all: (Column<Soa> | false)[] = [
    {
      key: 'no',
      header: 'SOA',
      kind: 'code',
      render: (s) => <CellStack main={s.soaNo} sub={s.insurerSoaNo} />,
    },
    showProgramme && {
      key: 'programme',
      header: 'Programme',
      render: (s) => <CellStack main={s.clientName ?? ''} sub={s.programmeNo ?? ''} />,
    },
    { key: 'insurer', header: 'Insurer', render: (s) => s.insurerName },
    {
      key: 'period',
      header: 'Period',
      kind: 'period',
      render: (s) => <PeriodCell from={s.periodFrom} to={s.periodTo} />,
    },
    { key: 'amount', header: 'Amount', kind: 'amount', render: (s) => <Amount value={s.amount} /> },
    { key: 'received', header: 'Received', kind: 'date', render: (s) => formatDate(s.receivedOn) },
    {
      key: 'status',
      header: 'Status',
      kind: 'status',
      render: (s) => <StatusBadge status={s.status} />,
    },
    { key: 'invoices', header: 'Invoices', kind: 'center', render: (s) => s.invoices.length },
  ];
  return all.filter((c): c is Column<Soa> => c !== false);
}

/** The SOAs matching the filters, paged; a row opens the SOA. */
export function SoaTable({
  filters,
  showProgramme,
  openId,
}: Readonly<{ filters: SoaFilters; showProgramme: boolean; openId?: number }>) {
  const companyId = useCompanyId();
  const [page, setPage] = useState(0);
  const [open, setOpen] = useState<number | undefined>(openId);
  const list = useQuery({
    queryKey: ['eb', 'soas', filters, page],
    queryFn: () => ebServiceApi.soas(companyId, filters, page),
  });
  return (
    <>
      <ErrorAlert error={list.error} onRetry={() => void list.refetch()} />
      <DataTable<Soa>
        loading={list.isLoading}
        rows={list.data?.content ?? []}
        rowKey={(s) => s.id}
        columns={columns(showProgramme)}
        onRowClick={(s) => setOpen(s.id)}
        emptyMessage="No SOAs to display"
      />
      <PageFooter data={list.data} noun="SOAs" onPage={setPage} />
      {open !== undefined && <SoaDetail id={open} onClose={() => setOpen(undefined)} />}
    </>
  );
}
