import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { saveFile } from '@/api/client';
import { csfCaseApi } from '@/api/csfCases';
import type { CaseAction, CaseRecord } from '@/api/csfCases';
import { LovSelect } from '@/components/broking/LovSelect';
import { RecordHeader } from '@/components/broking/RecordHeader';
import { ReferenceChip } from '@/components/broking/ReferenceChip';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { DefinitionGrid } from '@/components/ui/DefinitionGrid';
import { EmptyState } from '@/components/ui/EmptyState';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { Tabs } from '@/components/ui/Tabs';
import { useTabParam } from '@/components/ui/useTabParam';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate, formatDateTime } from '@/utils/format';
import { CSF_SECTION } from '../csfCodes';
import { CaseActionDialog } from './CaseActionDialog';
import { ACTION_LABELS, CASE_LOV, HEADER_ACTIONS } from './caseCodes';
import { SlaTags } from './SlaTags';

const TABS = [
  { id: 'details', label: 'Details' },
  { id: 'notes', label: 'Notes' },
  { id: 'documents', label: 'Documents' },
  { id: 'linked', label: 'Linked Cases' },
  { id: 'history', label: 'History' },
] as const;

type TabId = (typeof TABS)[number]['id'];

function Details({ r }: Readonly<{ r: CaseRecord }>) {
  return (
    <Card title="Details">
      <DefinitionGrid
        columns={2}
        collapseEmpty
        items={[
          { label: 'Subject', value: r.row.subject },
          { label: 'Channel', value: r.channel.label },
          {
            label: 'Case Type',
            value: `${r.row.caseType.label} (${String(r.row.tatDays)} working days)`,
          },
          { label: 'Nature of Transaction', value: r.row.nature.label },
          { label: 'Product Type', value: r.productType.label },
          { label: 'Market Segment', value: r.row.marketSegment.label },
          { label: 'Priority', value: r.row.priority.label },
          { label: 'Logged by', value: r.row.agent },
          { label: 'Caller Contact', value: r.callerContact },
          { label: 'Description', value: r.description },
          { label: 'Return Reason', value: r.returnReason?.label },
          { label: 'Instruction to the Agent', value: r.instruction },
          { label: 'Resolution', value: r.resolution },
          { label: 'Closure Reason', value: r.closureReason?.label },
          {
            label: 'Closed',
            value: r.closedAt ? `${formatDateTime(r.closedAt)} by ${r.closedBy ?? ''}` : null,
          },
        ]}
      />
    </Card>
  );
}

function Notes({ companyId, r }: Readonly<{ companyId: number; r: CaseRecord }>) {
  const queryClient = useQueryClient();
  const [text, setText] = useState('');
  const notes = useQuery({
    queryKey: ['csf-cases', 'notes', r.row.id],
    queryFn: () => csfCaseApi.notes(companyId, r.row.id),
  });
  const add = useMutation({
    mutationFn: () => csfCaseApi.addNote(companyId, r.row.id, text),
    onSuccess: () => {
      setText('');
      void queryClient.invalidateQueries({ queryKey: ['csf-cases'] });
    },
  });
  return (
    <Card title="Notes">
      <div className="stack">
        <ErrorAlert error={notes.error ?? add.error} />
        <DataTable
          columns={[
            { key: 'at', header: 'Date / Time', render: (n) => formatDateTime(n.createdAt) },
            { key: 'author', header: 'Author', render: (n) => n.author },
            { key: 'note', header: 'Note', render: (n) => n.note },
          ]}
          rows={notes.data ?? []}
          rowKey={(n) => `${n.createdAt}-${n.author}-${n.note}`}
          loading={notes.isLoading}
          emptyMessage="No notes yet"
          caption="Notes"
        />
        {r.actions.includes('NOTE') && (
          <>
            <Field label="Note">
              {(id) => (
                <textarea
                  id={id}
                  className="textarea"
                  rows={3}
                  maxLength={4000}
                  value={text}
                  onChange={(e) => setText(e.target.value)}
                />
              )}
            </Field>
            <div>
              <Button
                disabled={text.trim() === ''}
                busy={add.isPending}
                onClick={() => add.mutate()}
              >
                {ACTION_LABELS.NOTE}
              </Button>
            </div>
          </>
        )}
      </div>
    </Card>
  );
}

function Documents({ companyId, r }: Readonly<{ companyId: number; r: CaseRecord }>) {
  const queryClient = useQueryClient();
  const [type, setType] = useState('');
  const [file, setFile] = useState<File | null>(null);
  const docs = useQuery({
    queryKey: ['csf-cases', 'documents', r.row.id],
    queryFn: () => csfCaseApi.documents(companyId, r.row.id),
  });
  const upload = useMutation({
    mutationFn: (f: File) => csfCaseApi.upload(companyId, r.row.id, type, f),
    onSuccess: () => {
      setFile(null);
      void queryClient.invalidateQueries({ queryKey: ['csf-cases'] });
    },
  });
  const open = async (id: number) => {
    const f = await csfCaseApi.download(companyId, r.row.id, id);
    saveFile(f.blob, f.fileName);
  };
  return (
    <Card title="Documents">
      <div className="stack">
        <ErrorAlert error={docs.error ?? upload.error} />
        <DataTable
          columns={[
            {
              key: 'file',
              header: 'File',
              render: (d) => (
                <button type="button" className="link-button" onClick={() => void open(d.id)}>
                  {d.fileName}
                </button>
              ),
            },
            { key: 'type', header: 'Document Type', render: (d) => d.documentType },
            { key: 'by', header: 'Uploaded by', render: (d) => d.uploadedBy },
            { key: 'at', header: 'Uploaded', render: (d) => formatDateTime(d.uploadedAt) },
          ]}
          rows={docs.data ?? []}
          rowKey={(d) => d.id}
          loading={docs.isLoading}
          emptyMessage="No documents yet"
          caption="Documents"
        />
        {r.actions.includes('UPLOAD') && (
          <div className="filter-bar">
            <Field label="Document Type">
              {(id) => (
                <LovSelect id={id} type={CASE_LOV.documentType} value={type} onChange={setType} />
              )}
            </Field>
            <Field label="File">
              {(id) => (
                <input id={id} type="file" onChange={(e) => setFile(e.target.files?.[0] ?? null)} />
              )}
            </Field>
            <Button
              disabled={type === '' || file === null}
              busy={upload.isPending}
              onClick={() => {
                if (file !== null) {
                  upload.mutate(file);
                }
              }}
            >
              {ACTION_LABELS.UPLOAD}
            </Button>
          </div>
        )}
      </div>
    </Card>
  );
}

function Linked({ r }: Readonly<{ r: CaseRecord }>) {
  return (
    <Card title="Linked Cases">
      {r.linked.length === 0 ? (
        <EmptyState message="No linked cases" />
      ) : (
        <DataTable
          columns={[
            {
              key: 'no',
              header: 'Case ID',
              render: (l) => <Link to={`/csf/cases/${String(l.id)}`}>{l.caseNo}</Link>,
            },
            {
              key: 'relation',
              header: 'Link',
              render: (l) => (l.relation === 'ORIGINAL' ? 'Original case' : 'Follow-up case'),
            },
            { key: 'status', header: 'Status', render: (l) => l.statusLabel },
            { key: 'reason', header: 'Reason', render: (l) => l.reason },
          ]}
          rows={r.linked}
          rowKey={(l) => l.id}
          caption="Linked cases"
        />
      )}
    </Card>
  );
}

function History({ companyId, r }: Readonly<{ companyId: number; r: CaseRecord }>) {
  const history = useQuery({
    queryKey: ['csf-cases', 'history', r.row.id],
    queryFn: () => csfCaseApi.history(companyId, r.row.id),
  });
  return (
    <Card title="History">
      <ErrorAlert error={history.error} />
      <DataTable
        columns={[
          { key: 'at', header: 'Date / Time', render: (h) => formatDateTime(h.at) },
          { key: 'user', header: 'User', render: (h) => h.user },
          { key: 'summary', header: 'Action', render: (h) => h.summary },
          { key: 'from', header: 'From', render: (h) => h.from },
          { key: 'to', header: 'To', render: (h) => h.to },
        ]}
        rows={history.data ?? []}
        rowKey={(h) => `${h.at}-${h.summary}-${h.to ?? ''}`}
        loading={history.isLoading}
        caption="Case history"
      />
    </Card>
  );
}

function HeaderActions({
  r,
  onAction,
}: Readonly<{ r: CaseRecord; onAction: (a: CaseAction) => void }>) {
  return (
    <>
      {HEADER_ACTIONS.filter((a) => r.actions.includes(a)).map((a) => (
        <Button
          key={a}
          variant={a === 'RESOLVE' || a === 'REENDORSE' ? 'accent' : 'secondary'}
          onClick={() => onAction(a)}
        >
          {ACTION_LABELS[a]}
        </Button>
      ))}
    </>
  );
}

function TabBody({
  tab,
  companyId,
  r,
}: Readonly<{ tab: TabId; companyId: number; r: CaseRecord }>) {
  switch (tab) {
    case 'notes':
      return <Notes companyId={companyId} r={r} />;
    case 'documents':
      return <Documents companyId={companyId} r={r} />;
    case 'linked':
      return <Linked r={r} />;
    case 'history':
      return <History companyId={companyId} r={r} />;
    default:
      return <Details r={r} />;
  }
}

function facts(r: CaseRecord) {
  return [
    { label: 'Client / Caller', value: r.row.client },
    {
      label: 'Account / Policy',
      value: [r.row.arn, r.row.policyNo].filter(Boolean).join(' / ') || null,
    },
    { label: 'Case Type', value: `${r.row.caseType.label}, TAT ${String(r.row.tatDays)} days` },
    { label: 'Due Date', value: formatDate(r.row.dueDate) },
    { label: 'Fulfilment Unit', value: r.row.fulfilmentUnit.label },
    { label: 'Owner', value: r.row.owner },
    { label: 'Status Group', value: r.row.statusGroup },
    {
      label: 'Escalated',
      value: r.row.escalated
        ? `${r.row.escalated.reason.label}, ${formatDateTime(r.row.escalated.at)}`
        : 'No',
    },
  ];
}

/**
 * Case record (FR-CSF-051 to 058, 060): the header with the Case ID, status, case type and
 * turnaround time, due date, SLA indicator, client, fulfilment unit and owner; the actions the
 * user may take in the status of the case; the tabs Details, Notes, Documents, Linked Cases and
 * History.
 */
export default function CaseRecordPage() {
  const { id } = useParams<{ id: string }>();
  const caseId = Number(id);
  const companyId = useCompanyId();
  const [tab, setTab] = useTabParam<TabId>(
    TABS.map((t) => t.id),
    'details',
  );
  const [action, setAction] = useState<CaseAction | null>(null);
  const record = useQuery({
    queryKey: ['csf-cases', 'record', companyId, caseId],
    queryFn: () => csfCaseApi.get(companyId, caseId),
    enabled: Number.isFinite(caseId),
  });
  const r = record.data;
  return (
    <div className="stack">
      <PageHeader
        section={CSF_SECTION}
        backTo="/csf/cases"
        title={r ? `Case ${r.row.caseNo}` : 'Case'}
        description={r?.row.subject}
        actions={r && <HeaderActions r={r} onAction={setAction} />}
      />
      <ErrorAlert error={record.error} onRetry={() => void record.refetch()} />
      {r && (
        <>
          <RecordHeader
            chips={<ReferenceChip value={r.row.caseNo} label="Case ID" />}
            statuses={[{ label: 'Status', status: r.row.status, text: r.row.statusLabel }]}
            flags={<SlaTags row={r.row} />}
            facts={facts(r)}
          />
          <Tabs tabs={TABS} active={tab} onChange={setTab} />
          <TabBody tab={tab} companyId={companyId} r={r} />
          {action && (
            <CaseActionDialog
              companyId={companyId}
              record={r}
              action={action}
              onClose={() => setAction(null)}
            />
          )}
        </>
      )}
    </div>
  );
}
