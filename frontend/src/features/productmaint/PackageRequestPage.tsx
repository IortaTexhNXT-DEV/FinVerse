import { useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Building2,
  CalendarRange,
  FileDown,
  Layers,
  Pencil,
  Percent,
  Tag,
  UserRound,
} from 'lucide-react';
import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { PACKAGE_REQUEST_ENTITY, productMaintApi } from '@/api/productmaint';
import type { PackageRequest } from '@/api/productmaint';
import { useAuth } from '@/auth/authContext';
import { Attachments } from '@/components/attachments/Attachments';
import { RecordSummary } from '@/components/broking/RecordSummary';
import type { Fact } from '@/components/broking/RecordSummary';
import { ReferenceChip } from '@/components/broking/ReferenceChip';
import { SentMessages } from '@/components/broking/SentMessages';
import { useFileDownload } from '@/components/broking/useFileDownload';
import { WorkflowPanel } from '@/components/broking/WorkflowPanel';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { formatDate } from '@/utils/format';
import { AdvisoriesTab } from './AdvisoriesTab';
import { ComparativeTab } from './ComparativeTab';
import { NegotiationTab } from './NegotiationTab';
import { PackageActions } from './PackageActions';
import { typeLabel } from './packageRequest';
import { RequirementsTab } from './RequirementsTab';
import { DetailsTab, HistoryTab, SetupTab } from './RequestTabs';
import { TermsView } from './TermsView';
import '@/styles/quotation.css';

const TABS = [
  { id: 'details', label: 'Details' },
  { id: 'terms', label: 'Requested Terms' },
  { id: 'negotiation', label: 'Negotiation' },
  { id: 'comparative', label: 'Comparative' },
  { id: 'requirements', label: 'Requirements & Sign-off' },
  { id: 'setup', label: 'Set-up' },
  { id: 'advisories', label: 'Advisories' },
  { id: 'documents', label: 'Documents' },
  { id: 'emails', label: 'E-mails' },
  { id: 'history', label: 'History' },
] as const;

type TabId = (typeof TABS)[number]['id'];

function productLabel(p: PackageRequest): string {
  const product = p.productCode ?? 'new package';
  const version = p.resultingVersionNo === undefined ? '' : ` v${p.resultingVersionNo}`;
  return `${p.lineCode} · ${product}${version}`;
}

function facts(p: PackageRequest): Fact[] {
  const terms = p.proposedTerms ?? p.requestedTerms;
  return [
    { icon: Tag, label: 'Request type', value: typeLabel(p.requestType) },
    { icon: UserRound, label: 'Client / programme', value: p.clientName ?? 'Generic programme' },
    { icon: Layers, label: 'Line / product', value: productLabel(p) },
    {
      icon: Percent,
      label: 'Scheme rate',
      value: terms.scheme.defaultRate === undefined ? '—' : `${terms.scheme.defaultRate}%`,
    },
    {
      icon: CalendarRange,
      label: 'Package term',
      value: `${formatDate(terms.dates.packageStartDate)} – ${formatDate(terms.dates.packageEndDate)}`,
    },
    {
      icon: Building2,
      label: p.chosenInsurers.length > 0 ? 'Chosen insurers' : 'Target insurers',
      value:
        (p.chosenInsurers.length > 0
          ? p.chosenInsurers
          : p.requestedTerms.insurers.map((i) => i.insurerCode)
        ).join(', ') || '—',
    },
  ];
}

function canEdit(p: PackageRequest, can: (permission: string) => boolean): boolean {
  return (
    (p.status === 'DRAFT' && can('PKG_REQUEST')) ||
    (p.status === 'FOR_TSU_REVIEW' && can('PKG_TSU_RECOMMEND'))
  );
}

function TabBody({ tab, request }: Readonly<{ tab: TabId; request: PackageRequest }>) {
  switch (tab) {
    case 'details':
      return <DetailsTab request={request} />;
    case 'terms':
      return <TermsView terms={request.requestedTerms} title="Requested terms" />;
    case 'negotiation':
      return <NegotiationTab request={request} />;
    case 'comparative':
      return <ComparativeTab request={request} />;
    case 'requirements':
      return <RequirementsTab request={request} />;
    case 'setup':
      return <SetupTab request={request} />;
    case 'advisories':
      return <AdvisoriesTab request={request} />;
    case 'documents':
      return (
        <Attachments
          entityType={PACKAGE_REQUEST_ENTITY}
          entityId={request.id}
          title="Documents"
          reference={request.requestNo}
        />
      );
    case 'emails':
      return (
        <Card title="E-mails" flush>
          <SentMessages entityType={PACKAGE_REQUEST_ENTITY} entityId={request.id} />
        </Card>
      );
    default:
      return <HistoryTab requestId={request.id} />;
  }
}

/**
 * One package request (BRPM.008-017): header with the request number, stage pill and scope flag;
 * the workflow panel with the business actions of each stage; and tabs for the details, requested
 * terms, negotiation rounds, comparative outputs, requirements and ManCom sign-off, MBS set-up,
 * advisories, documents, e-mails and history.
 */
export default function PackageRequestPage() {
  const id = Number(useParams().id);
  const { can } = useAuth();
  const queryClient = useQueryClient();
  const download = useFileDownload();
  const [tab, setTab] = useState<TabId>('details');
  const request = useQuery({
    queryKey: ['package-request', id],
    queryFn: () => productMaintApi.get(id),
  });
  if (request.data === undefined) {
    return request.error ? (
      <ErrorAlert error={request.error} />
    ) : (
      <span className="spinner" aria-label="Loading" />
    );
  }
  const p = request.data;
  return (
    <div className="stack">
      <PageHeader
        backTo="/product-maintenance/requests"
        section="Product Maintenance · Package Request"
        title={p.requestNo}
        description={`${typeLabel(p.requestType)}: ${p.title}`}
        actions={
          <>
            <Button
              variant="secondary"
              icon={<FileDown size={16} />}
              busy={download.isPending}
              onClick={() => download.mutate(() => productMaintApi.formPdf(p.id))}
            >
              Request Form
            </Button>
            {canEdit(p, can) && (
              <Link className="btn btn-secondary" to={`/product-maintenance/requests/${p.id}/edit`}>
                <Pencil size={16} aria-hidden="true" /> Edit
              </Link>
            )}
          </>
        }
      />
      <RecordSummary
        title={p.title}
        chips={
          <>
            <ReferenceChip label="Request" value={p.requestNo} />
            <StatusBadge status={p.status} />
          </>
        }
        flags={
          <>
            <span className="tag">
              {p.scope === 'CLIENT_SPECIFIC' ? 'Client-specific' : 'Generic'}
            </span>
            {!p.negotiationRequired && <span className="tag">No negotiation</span>}
          </>
        }
        facts={facts(p)}
      />
      <ErrorAlert error={download.error} />
      <WorkflowPanel
        entityType={PACKAGE_REQUEST_ENTITY}
        entityId={p.id}
        showHistory={false}
        onChanged={() => void queryClient.invalidateQueries({ queryKey: ['package-request', id] })}
        renderBusinessActions={(actions) => <PackageActions request={p} actions={actions} />}
      />
      <Tabs tabs={TABS} active={tab} onChange={setTab} />
      <TabBody tab={tab} request={p} />
    </div>
  );
}
