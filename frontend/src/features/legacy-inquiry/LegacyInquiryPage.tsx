import { useQuery } from '@tanstack/react-query';
import { Download, Search } from 'lucide-react';
import { useState } from 'react';
import { legacyInquiryApi } from '@/api/legacyInquiry';
import type {
  AccessReason,
  ArchiveCriteria,
  ArchiveRecord,
  InquirySettings,
} from '@/api/legacyInquiry';
import { useAuth } from '@/auth/authContext';
import { safeUrl } from '@/utils/safeUrl';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { CellStack } from '@/components/ui/CellStack';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { useCompanyId } from '@/context/workspaceContext';
import { useDownload } from '@/features/migration/common/useDownload';
import { formatAmount, formatDate, humanize } from '@/utils/format';
import { DateInput } from '@/components/ui/DateInput';
import { RecordPanel } from './RecordPanel';
import { ReasonCard } from './ReasonCard';
import { keepReason, reasonLabel, sessionReason } from './reason';
import { LegacyRecordType, LegacyStatus } from './LegacyStatus';

const TYPES = [
  'CLIENT',
  'POLICY',
  'INVOICE',
  'RECEIPT',
  'REMITTANCE',
  'ENDORSEMENT',
  'CLAIM',
  'GL_JOURNAL',
  'RENEWAL_ADVICE',
  'LETTER',
  'OTHER',
];

const TEXT_FIELDS: { key: keyof ArchiveCriteria; label: string }[] = [
  { key: 'client', label: 'Client Name or No.' },
  { key: 'policyNo', label: 'Policy / Cover No.' },
  { key: 'invoiceNo', label: 'Invoice No.' },
  { key: 'receiptNo', label: 'Receipt No.' },
  { key: 'claimNo', label: 'Claim No.' },
];

const COLUMNS: Column<ArchiveRecord>[] = [
  {
    key: 'record',
    header: 'Record',
    render: (r) => (
      <CellStack
        main={r.legacyKey}
        sub={
          <>
            {r.sourceSystem} <LegacyRecordType type={r.recordType} />
          </>
        }
      />
    ),
  },
  {
    key: 'client',
    header: 'Client',
    render: (r) => <CellStack main={r.clientName ?? ''} sub={r.clientKey ?? ''} />,
  },
  {
    key: 'refs',
    header: 'Policy / Invoice / Receipt',
    render: (r) => (
      <CellStack
        main={r.policyNo ?? ''}
        sub={[r.invoiceNo, r.receiptNo, r.claimNo].filter(Boolean).join(' / ')}
      />
    ),
  },
  { key: 'date', header: 'Date', kind: 'date', render: (r) => formatDate(r.documentDate) },
  {
    key: 'amount',
    header: 'Amount',
    kind: 'amount',
    render: (r) => (r.amount === undefined ? '' : `${r.currency ?? ''} ${formatAmount(r.amount)}`),
  },
  { key: 'status', header: 'Legacy Status', render: (r) => <LegacyStatus status={r.status} /> },
  { key: 'docs', header: 'Documents', kind: 'center', render: (r) => r.documentCount },
];

function SearchCard({
  reason,
  links,
  onSearch,
}: Readonly<{
  reason: AccessReason | undefined;
  links: InquirySettings['links'];
  onSearch: (criteria: ArchiveCriteria) => void;
}>) {
  const [draft, setDraft] = useState<ArchiveCriteria>({});
  const set = (key: keyof ArchiveCriteria, value: string) =>
    setDraft((d) => ({ ...d, [key]: value === '' ? undefined : value }));
  const input = (key: keyof ArchiveCriteria, label: string, type = 'text') => (
    <Field key={key} label={label}>
      {(id) =>
        type === 'date' ? (
          <DateInput id={id} value={draft[key] ?? ''} onChange={(e) => set(key, e.target.value)} />
        ) : (
          <input
            id={id}
            type={type}
            className="input"
            value={draft[key] ?? ''}
            onChange={(e) => set(key, e.target.value)}
          />
        )
      }
    </Field>
  );
  return (
    <Card>
      {reason !== undefined && (
        <p className="muted">
          Reason of this session: {reasonLabel(reason.reasonCode)}
          {reason.reasonText === '' ? '' : ` - ${reason.reasonText}`}
        </p>
      )}
      <div className="worklist-filters form-grid">
        {TEXT_FIELDS.map((f) => input(f.key, f.label))}
        <Field label="Record Type">
          {(id) => (
            <select
              id={id}
              className="select"
              value={draft.recordType ?? ''}
              onChange={(e) => set('recordType', e.target.value)}
            >
              <option value="">All</option>
              {TYPES.map((t) => (
                <option key={t} value={t}>
                  {humanize(t)}
                </option>
              ))}
            </select>
          )}
        </Field>
        {input('from', 'Date From', 'date')}
        {input('to', 'Date To', 'date')}
      </div>
      <Button
        variant="primary"
        icon={<Search size={14} />}
        disabled={Object.values(draft).every((v) => v === undefined)}
        onClick={() => onSearch(draft)}
      >
        Search
      </Button>
      {links.length > 0 && (
        <p className="muted">
          Read-only legacy applications:{' '}
          {links.map((l) =>
            safeUrl(l.url) === undefined ? (
              <span key={l.system}>{l.system} </span>
            ) : (
              <a key={l.system} href={safeUrl(l.url)} target="_blank" rel="noreferrer">
                {l.system}{' '}
              </a>
            ),
          )}
        </p>
      )}
    </Card>
  );
}

function Results({
  criteria,
  reason,
}: Readonly<{ criteria: ArchiveCriteria; reason: AccessReason }>) {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const download = useDownload();
  const [page, setPage] = useState(0);
  const [selected, setSelected] = useState<number>();
  const results = useQuery({
    queryKey: ['legacy-inquiry', 'records', companyId, criteria, page, reason],
    queryFn: () => legacyInquiryApi.search(companyId, criteria, reason, page),
    enabled: companyId > 0,
  });
  return (
    <>
      <Card
        title="Legacy Records"
        flush
        actions={
          can('LEGACY_INQUIRY_EXPORT') && (
            <Button
              variant="secondary"
              size="sm"
              icon={<Download size={14} />}
              busy={download.busy}
              onClick={() =>
                download.run(() => legacyInquiryApi.export(companyId, criteria, reason))
              }
            >
              Export
            </Button>
          )
        }
      >
        <ErrorAlert
          error={results.error ?? download.error}
          onRetry={() => void results.refetch()}
        />
        <DataTable<ArchiveRecord>
          loading={results.isLoading}
          rows={results.data?.content ?? []}
          rowKey={(r) => String(r.id)}
          selectedKey={selected === undefined ? undefined : String(selected)}
          onRowClick={(r) => setSelected(r.id)}
          emptyMessage="No legacy record matches the search"
          columns={COLUMNS}
        />
        <PageFooter data={results.data} noun="records" onPage={setPage} />
      </Card>
      {selected !== undefined && (
        <RecordPanel id={selected} reason={reason} onClose={() => setSelected(undefined)} />
      )}
    </>
  );
}

/**
 * Legacy Inquiry (DATA_MIGRATION_DESIGN section 16): the read-only archive of the legacy systems.
 * A reason is given once per session; each search, view, download and export is logged with it.
 */
export default function LegacyInquiryPage() {
  const [reason, setReason] = useState<AccessReason | undefined>(sessionReason);
  const [criteria, setCriteria] = useState<ArchiveCriteria>();
  const [search, setSearch] = useState(0);
  const settings = useQuery({
    queryKey: ['legacy-inquiry', 'settings'],
    queryFn: legacyInquiryApi.settings,
  });
  const needsReason = settings.data?.reasonRequired !== false && reason === undefined;
  return (
    <div className="stack">
      <PageHeader
        section="Legacy Inquiry"
        title="Legacy Inquiry"
        description="Read-only archive of the legacy systems. Every access is logged with its reason."
      />
      {needsReason ? (
        <ReasonCard
          onGiven={(r) => {
            keepReason(r);
            setReason(r);
          }}
        />
      ) : (
        <SearchCard
          reason={reason}
          links={settings.data?.links ?? []}
          onSearch={(c) => {
            setSearch((n) => n + 1);
            setCriteria(c);
          }}
        />
      )}
      {criteria !== undefined && !needsReason && (
        <Results
          key={search}
          criteria={criteria}
          reason={reason ?? { reasonCode: '', reasonText: '' }}
        />
      )}
    </div>
  );
}
