import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Building2,
  CalendarClock,
  CalendarRange,
  FileSpreadsheet,
  FileText,
  Layers,
  Pencil,
  ShieldCheck,
  User,
  Wallet,
} from 'lucide-react';
import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { saveFile } from '@/api/client';
import type { DownloadedFile } from '@/api/client';
import { quotationsApi, QUOTATION_ENTITY } from '@/api/quotations';
import type { Quotation } from '@/api/quotations';
import { useAuth } from '@/auth/authContext';
import { Attachments } from '@/components/attachments/Attachments';
import { InstructionsBanner } from '@/components/broking/InstructionsBanner';
import { ReferenceChip } from '@/components/broking/ReferenceChip';
import { SentMessages } from '@/components/broking/SentMessages';
import { WorkflowPanel } from '@/components/broking/WorkflowPanel';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { formatAmount, formatDate } from '@/utils/format';
import { QuotationActions } from './QuotationActions';
import { DetailsTab, HistoryTab, ItemsTab, VersionsTab } from './QuotationTabs';
import { RecordSummary } from './RecordSummary';
import type { Fact } from './RecordSummary';

const TABS = [
  { id: 'details', label: 'Details' },
  { id: 'items', label: 'Items & Premium' },
  { id: 'versions', label: 'Versions' },
  { id: 'documents', label: 'Documents' },
  { id: 'emails', label: 'E-mails' },
  { id: 'history', label: 'History' },
] as const;

type TabId = (typeof TABS)[number]['id'];

function facts(q: Quotation): Fact[] {
  const c = q.content;
  return [
    { icon: User, label: 'Client', value: `${q.clientCode} – ${q.clientName}` },
    { icon: Layers, label: 'Product', value: `${q.productCode} (${q.lineCode})` },
    { icon: Building2, label: 'Insurer', value: c.insurerCode ?? 'To be advised' },
    {
      icon: CalendarRange,
      label: 'Period',
      value: `${formatDate(c.periodFrom)} – ${formatDate(c.periodTo)}`,
    },
    { icon: CalendarClock, label: 'Valid until', value: formatDate(c.validUntil) },
    {
      icon: Wallet,
      label: 'Gross premium',
      value: c.rated ? `${q.currency} ${formatAmount(c.premium.grossPremium)}` : 'Not rated',
    },
  ];
}

function VersionSelect({
  quotation,
  value,
  onChange,
}: Readonly<{ quotation: Quotation; value: number; onChange: (v: number) => void }>) {
  const versions = Array.from({ length: quotation.currentVersion }, (_, i) => i + 1).reverse();
  return (
    <label className="checkbox">
      Version{' '}
      <select className="select" value={value} onChange={(e) => onChange(Number(e.target.value))}>
        {versions.map((v) => (
          <option key={v} value={v}>
            v{v}
            {v === quotation.currentVersion ? ' (current)' : ''}
          </option>
        ))}
      </select>
    </label>
  );
}

function ItemsOfVersion({
  quotation,
  version,
}: Readonly<{ quotation: Quotation; version: number }>) {
  const past = useQuery({
    queryKey: ['quotation', quotation.id, 'version', version],
    queryFn: () => quotationsApi.version(quotation.id, version),
    enabled: version !== quotation.currentVersion,
  });
  const current = version === quotation.currentVersion;
  const content = current ? quotation.content : past.data?.content;
  if (content === undefined) {
    return past.error ? (
      <ErrorAlert error={past.error} />
    ) : (
      <span className="spinner" aria-label="Loading" />
    );
  }
  return (
    <ItemsTab
      content={content}
      currency={quotation.currency}
      versionLabel={`version ${version}${current ? ' (current)' : ' (read-only)'}`}
    />
  );
}

function TabBody({
  tab,
  quotation,
  version,
}: Readonly<{ tab: TabId; quotation: Quotation; version: number }>) {
  const [compare, setCompare] = useState({
    from: Math.max(1, quotation.currentVersion - 1),
    to: quotation.currentVersion,
  });
  switch (tab) {
    case 'details':
      return <DetailsTab quotation={quotation} />;
    case 'items':
      return <ItemsOfVersion quotation={quotation} version={version} />;
    case 'versions':
      return (
        <VersionsTab
          quotation={quotation}
          from={compare.from}
          to={compare.to}
          onCompare={(from, to) => setCompare({ from, to })}
        />
      );
    case 'documents':
      return (
        <Attachments
          entityType={QUOTATION_ENTITY}
          entityId={quotation.id}
          title="Documents"
          reference={quotation.arn}
        />
      );
    case 'emails':
      return (
        <Card title="E-mails" flush>
          <SentMessages entityType={QUOTATION_ENTITY} entityId={quotation.id} />
        </Card>
      );
    default:
      return <HistoryTab entityId={quotation.id} />;
  }
}

function Downloads({ id }: Readonly<{ id: number }>) {
  const download = useMutation({
    mutationFn: (fetch: () => Promise<DownloadedFile>) => fetch(),
    onSuccess: (f) => saveFile(f.blob, f.fileName),
  });
  return (
    <>
      <Button
        variant="secondary"
        icon={<FileText size={16} />}
        onClick={() => download.mutate(() => quotationsApi.pdf(id))}
      >
        PDF
      </Button>
      <Button
        variant="secondary"
        icon={<FileSpreadsheet size={16} />}
        onClick={() => download.mutate(() => quotationsApi.xlsx(id))}
      >
        Excel
      </Button>
    </>
  );
}

/**
 * One quotation (BRNB.020/043/045): header with the Proposal No., ARN chip, status and version
 * selector; the client's special instructions; the workflow panel with the business actions; and
 * tabs for details, items and premium, versions and diff, documents, e-mails and history.
 */
export default function QuotationDetailPage() {
  const id = Number(useParams().id);
  const { can } = useAuth();
  const queryClient = useQueryClient();
  const [tab, setTab] = useState<TabId>('details');
  const [version, setVersion] = useState<number>();
  const quotation = useQuery({ queryKey: ['quotation', id], queryFn: () => quotationsApi.get(id) });
  if (quotation.data === undefined) {
    return quotation.error ? (
      <ErrorAlert error={quotation.error} />
    ) : (
      <span className="spinner" aria-label="Loading" />
    );
  }
  const q = quotation.data;
  const shown = version ?? q.currentVersion;
  const editable = q.status === 'DRAFT' && can('QUOTE_MAINTAIN');
  return (
    <div className="stack">
      <PageHeader
        backTo="/quotations"
        section="Quotation / Proposal · Quotation"
        title={q.quotationNo}
        description={`${q.productCode} quotation for ${q.clientName}`}
        actions={
          <>
            <VersionSelect quotation={q} value={shown} onChange={setVersion} />
            <Downloads id={q.id} />
            {editable && (
              <Link className="btn btn-secondary" to={`/quotations/${q.id}/edit`}>
                <Pencil size={16} aria-hidden="true" /> Edit
              </Link>
            )}
          </>
        }
      />
      <RecordSummary
        title={q.clientName}
        chips={
          <>
            <ReferenceChip label="ARN" value={q.arn} />
            <StatusBadge status={q.status} />
            {q.content.directPayment && <StatusBadge status="DIRECT_PAYMENT" />}
            {q.tsuRequired && (
              <span className="badge warning" title={q.tsuReason}>
                <ShieldCheck size={12} aria-hidden="true" /> TSU rule applies
              </span>
            )}
          </>
        }
        facts={facts(q)}
      />
      <InstructionsBanner clientId={q.clientId} />
      <WorkflowPanel
        entityType={QUOTATION_ENTITY}
        entityId={q.id}
        showHistory={false}
        onChanged={() => void queryClient.invalidateQueries({ queryKey: ['quotation', id] })}
        renderBusinessActions={(actions) => <QuotationActions quotation={q} actions={actions} />}
      />
      <Tabs tabs={TABS} active={tab} onChange={setTab} />
      <TabBody tab={tab} quotation={q} version={shown} />
    </div>
  );
}
