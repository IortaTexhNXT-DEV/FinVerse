import { useQuery } from '@tanstack/react-query';
import { ClipboardPlus, Forward, PhoneCall } from 'lucide-react';
import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { csfApi } from '@/api/csf';
import { csfCaseApi } from '@/api/csfCases';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import type { ClientSummary } from '@/api/csf';
import { useAuth } from '@/auth/authContext';
import { LovLabel } from '@/components/broking/LovLabel';
import { RecordHeader } from '@/components/broking/RecordHeader';
import { ReferenceChip } from '@/components/broking/ReferenceChip';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Notice } from '@/components/ui/Notice';
import { PageHeader } from '@/components/ui/PageHeader';
import { Tabs } from '@/components/ui/Tabs';
import { Tag } from '@/components/ui/Tag';
import { useTabParam } from '@/components/ui/useTabParam';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDateTime } from '@/utils/format';
import { CSF_SECTION } from '../csfCodes';
import { CASE_COLUMNS } from '../cases/caseColumns';
import { LogCaseDialog } from '../cases/LogCaseDialog';
import { ReferralDialog } from '../dialogs/ReferralDialog';
import { UpdateContactDialog } from '../dialogs/UpdateContactDialog';
import { AccountsTab } from './AccountsTab';
import { AdvicesTab, DocumentsTab } from './DocumentsTab';
import { EpoliciesTab } from './EpoliciesTab';
import { HistoryTab } from './HistoryTab';
import { PaymentsTab } from './PaymentsTab';
import { groupLabel } from '@/context/clientNames';

const TABS = [
  { id: 'accounts', label: 'Accounts' },
  { id: 'payments', label: 'Payments' },
  { id: 'advices', label: 'Renewal Advice' },
  { id: 'epolicies', label: 'E-policies' },
  { id: 'documents', label: 'Documents' },
  { id: 'history', label: 'Contact History' },
  { id: 'cases', label: 'Cases' },
] as const;

type TabId = (typeof TABS)[number]['id'];

function addressOf(c: ClientSummary['contact']): string {
  return [c.addressLine, c.city, c.province, c.postalCode].filter((p) => p).join(', ');
}

function verificationText(s: ClientSummary): string | null {
  const v = s.verification;
  return v ? `Verified until ${formatDateTime(v.validUntil)}` : null;
}

function TabBody({
  tab,
  companyId,
  summary,
}: Readonly<{ tab: TabId; companyId: number; summary: ClientSummary }>) {
  const accounts = useQuery({
    queryKey: ['csf', 'accounts', companyId, summary.id],
    queryFn: () => csfApi.accounts(companyId, summary.id),
    enabled: tab === 'payments' || tab === 'documents',
  });
  const lines = accounts.data ?? [];
  switch (tab) {
    case 'payments':
      return (
        <PaymentsTab companyId={companyId} clientId={summary.id} arns={lines.map((a) => a.arn)} />
      );
    case 'advices':
      return <AdvicesTab companyId={companyId} clientId={summary.id} clientCode={summary.code} />;
    case 'epolicies':
      return <EpoliciesTab companyId={companyId} clientId={summary.id} />;
    case 'documents':
      return (
        <DocumentsTab
          companyId={companyId}
          clientId={summary.id}
          clientCode={summary.code}
          accounts={lines}
        />
      );
    case 'history':
      return <HistoryTab companyId={companyId} clientId={summary.id} />;
    case 'cases':
      return <CasesTab companyId={companyId} clientId={summary.id} />;
    default:
      return <AccountsTab companyId={companyId} clientId={summary.id} />;
  }
}

type DialogId = 'contact' | 'referral' | 'case';

function ServicingActions({
  mayLog,
  mayChange,
  onOpen,
}: Readonly<{ mayLog: boolean; mayChange: boolean; onOpen: (d: DialogId) => void }>) {
  return (
    <>
      {mayLog && (
        <Button
          variant="secondary"
          icon={<ClipboardPlus size={16} aria-hidden="true" />}
          onClick={() => onOpen('case')}
        >
          Log Case
        </Button>
      )}
      {mayChange && (
        <>
          <Button
            variant="secondary"
            icon={<Forward size={16} aria-hidden="true" />}
            onClick={() => onOpen('referral')}
          >
            Refer to Fulfilment Unit
          </Button>
          <Button
            variant="accent"
            icon={<PhoneCall size={16} aria-hidden="true" />}
            onClick={() => onOpen('contact')}
          >
            Update Contact
          </Button>
        </>
      )}
    </>
  );
}

function CasesTab({ companyId, clientId }: Readonly<{ companyId: number; clientId: number }>) {
  const cases = useQuery({
    queryKey: ['csf-cases', 'client', companyId, clientId],
    queryFn: () => csfCaseApi.ofClient(companyId, clientId),
  });
  return (
    <Card flush>
      <DataTable
        columns={CASE_COLUMNS}
        rows={cases.data ?? []}
        rowKey={(c) => c.id}
        loading={cases.isLoading}
        emptyMessage="No cases for this client"
        caption="Cases of the client"
      />
    </Card>
  );
}

/**
 * Servicing View (FR-CSF-011 to 013, 020 to 022, 030 to 033; BRCSF-002, 005, 008, 009): the client
 * summary with the contact details and the actions Update Contact and Refer to Fulfilment Unit,
 * Log Case (FR-CSF-050), then the tabs Accounts, Payments, Renewal Advice, E-policies, Documents,
 * Contact History and Cases,
 * each loaded on its own so one source that does not answer leaves the others working.
 */
export default function ServicingViewPage() {
  const { clientId } = useParams<{ clientId: string }>();
  const id = Number(clientId);
  const companyId = useCompanyId();
  const { can } = useAuth();
  const [tab, setTab] = useTabParam<TabId>(
    TABS.map((t) => t.id),
    'accounts',
  );
  const [dialog, setDialog] = useState<DialogId | null>(null);
  const summary = useQuery({
    queryKey: ['csf', 'summary', companyId, id],
    queryFn: () => csfApi.summary(companyId, id),
    enabled: Number.isFinite(id),
  });
  const s = summary.data;
  return (
    <div className="stack">
      <PageHeader
        section={CSF_SECTION}
        backTo="/csf"
        title={s ? s.name : 'Servicing View'}
        description={s ? `Client ${s.code}` : undefined}
        actions={
          s && (
            <ServicingActions
              mayLog={can('CSF_CASE_CREATE')}
              mayChange={can('CSF_CONTACT_UPDATE')}
              onOpen={setDialog}
            />
          )
        }
      />
      <ErrorAlert error={summary.error} onRetry={() => void summary.refetch()} />
      {s && (
        <>
          <RecordHeader
            chips={
              <>
                <ReferenceChip value={s.code} label="Client" />
                {s.prospectCode !== s.code && (
                  <ReferenceChip value={s.prospectCode} label="Prospect" />
                )}
              </>
            }
            status={s.status}
            statuses={[{ label: 'KYC', status: s.kycStatus }]}
            flags={
              <>
                {s.bankClient && <Tag>{groupLabel('Client')}</Tag>}
                {s.banner.tags.map((t) => (
                  <Tag key={t.code}>{t.label}</Tag>
                ))}
              </>
            }
            facts={[
              {
                label: 'Client Type',
                value: s.clientType === 'CORPORATE' ? 'Corporate' : 'Individual',
              },
              {
                label: 'Segment',
                value: <LovLabel type="MARKET_SEGMENT" code={s.marketSegment} />,
              },
              { label: 'Accounts', value: String(s.accounts) },
              { label: 'Caller', value: verificationText(s) },
              { label: 'E-mail', value: s.contact.email },
              { label: 'Mobile', value: s.contact.mobile },
              { label: 'Phone', value: s.contact.phone },
              { label: 'Address', value: addressOf(s.contact) },
            ]}
          />
          {s.banner.instructions.length > 0 && (
            <Notice
              tone="info"
              title="Special instructions"
              items={s.banner.instructions.map((i) => `${i.typeLabel}: ${i.text}`)}
            />
          )}
          <Tabs tabs={TABS} active={tab} onChange={setTab} />
          <TabBody tab={tab} companyId={companyId} summary={s} />
          {dialog === 'contact' && (
            <UpdateContactDialog
              companyId={companyId}
              clientId={s.id}
              clientName={s.name}
              contact={s.contact}
              verification={s.verification}
              onClose={() => setDialog(null)}
            />
          )}
          {dialog === 'case' && (
            <LogCaseDialog
              companyId={companyId}
              client={{ id: s.id, name: s.name }}
              onClose={() => setDialog(null)}
            />
          )}
          {dialog === 'referral' && (
            <ReferralDialog
              companyId={companyId}
              clientId={s.id}
              clientName={s.name}
              onClose={() => setDialog(null)}
            />
          )}
        </>
      )}
    </div>
  );
}
