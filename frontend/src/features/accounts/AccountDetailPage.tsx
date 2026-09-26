import { useQuery } from '@tanstack/react-query';
import { Building2, CalendarRange, Layers, Pencil, User, UserCheck, Wallet } from 'lucide-react';
import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { accountsApi, ACCOUNT_ENTITY } from '@/api/accounts';
import type { Account } from '@/api/accounts';
import { useAuth } from '@/auth/authContext';
import { Attachments } from '@/components/attachments/Attachments';
import { RecordSummary } from '@/components/broking/RecordSummary';
import type { Fact } from '@/components/broking/RecordSummary';
import { ReferenceChip } from '@/components/broking/ReferenceChip';
import { SentMessages } from '@/components/broking/SentMessages';
import { WorkflowPanel } from '@/components/broking/WorkflowPanel';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { PolicyPanel } from '@/features/issuance/PolicyPanel';
import { PlacementPanel } from '@/features/placement/PlacementPanel';
import { formatAmount, formatDate } from '@/utils/format';
import { AccountActions } from './AccountActions';
import { AccountCheckPanel } from './AccountCheckPanel';
import { DetailsPanel, HistoryPanel, ItemsPanel } from './AccountPanels';
import { AccountTagsCard } from './AccountTagsCard';
import { PremiumSummary } from './PremiumSummary';
import { useAccountRefresh } from './useAccountRefresh';

const TABS = [
  { id: 'details', label: 'Details' },
  { id: 'items', label: 'Risk Items' },
  { id: 'premium', label: 'Premium' },
  { id: 'documents', label: 'Documents' },
  { id: 'emails', label: 'E-mails' },
  // Placement and issuance tabs: mounted here until the account page offers an extension point.
  { id: 'placement', label: 'Placement' },
  { id: 'policy', label: 'Policy' },
  { id: 'history', label: 'History' },
] as const;

type TabId = (typeof TABS)[number]['id'];

const EDITABLE = new Set(['DRAFT', 'RETURNED_TO_MARKETING']);

/** Key facts of the account summary card (BDO record page pattern). */
function accountFacts(a: Account): Fact[] {
  return [
    { icon: User, label: 'Client', value: `${a.clientCode ?? '—'} – ${a.clientName}` },
    { icon: Layers, label: 'Product', value: `${a.productCode} (${a.lineCode})` },
    { icon: Building2, label: 'Insurer', value: a.insurerCode ?? 'To be advised' },
    {
      icon: CalendarRange,
      label: 'Period',
      value: `${formatDate(a.periodFrom)} – ${formatDate(a.periodTo)}`,
    },
    {
      icon: Wallet,
      label: 'Gross Premium',
      value: `${a.currency} ${formatAmount(a.premium.grossPremium)}`,
    },
    { icon: UserCheck, label: 'Account Officer', value: a.sales.accountOfficer },
  ];
}

/** The flags of the record summary: renewal (BT0), Free First Year, direct payment. */
function accountFlags(a: Account) {
  if (!a.freeFirstYear.active && !a.directPayment && a.businessType !== 'RENEWAL') {
    return undefined;
  }
  return (
    <>
      {a.businessType === 'RENEWAL' && (
        <span className="tag" title={a.renewalOfRef ? `Renews ${a.renewalOfRef}` : undefined}>
          Renewal
        </span>
      )}
      {a.freeFirstYear.active && <span className="tag">FFY</span>}
      {a.directPayment && <span className="tag">Direct Payment</span>}
    </>
  );
}

function TabBody({ tab, account }: Readonly<{ tab: TabId; account: Account }>) {
  switch (tab) {
    case 'details':
      return (
        <>
          <AccountTagsCard account={account} />
          <DetailsPanel account={account} />
        </>
      );
    case 'items':
      return <ItemsPanel account={account} />;
    case 'premium':
      return <PremiumSummary accountId={account.id} />;
    case 'documents':
      return (
        <Attachments
          entityType={ACCOUNT_ENTITY}
          entityId={account.id}
          title="Documents"
          reference={account.arn}
        />
      );
    case 'emails':
      return (
        <Card title="E-mails" flush>
          <SentMessages entityType={ACCOUNT_ENTITY} entityId={account.id} />
        </Card>
      );
    case 'placement':
      return <PlacementPanel arn={account.arn} />;
    case 'policy':
      return <PolicyPanel arn={account.arn} />;
    default:
      return <HistoryPanel accountId={account.id} />;
  }
}

/**
 * One account (BRNB.050): status and work case with the actions the user may take, readiness
 * check while it is open for changes, and tabs for details, items, premium, documents, e-mails
 * and history.
 */
export default function AccountDetailPage() {
  const id = Number(useParams().id);
  const { can } = useAuth();
  const [tab, setTab] = useState<TabId>('details');
  const refresh = useAccountRefresh(id);
  const account = useQuery({ queryKey: ['account', id], queryFn: () => accountsApi.get(id) });
  if (account.data === undefined) {
    return account.error ? (
      <ErrorAlert error={account.error} />
    ) : (
      <span className="spinner" aria-label="Loading" />
    );
  }
  const a = account.data;
  const editable = EDITABLE.has(a.status) && can('ACCOUNT_MAINTAIN');
  return (
    <div className="stack">
      <PageHeader
        backTo="/accounts"
        section="Accounts & Placement · Account"
        title={a.arn}
        description={`${a.productCode} account of ${a.clientName}`}
        actions={
          editable && (
            <Link className="btn btn-secondary" to={`/accounts/${a.id}/edit`}>
              <Pencil size={16} aria-hidden="true" /> Edit Account
            </Link>
          )
        }
      />
      <RecordSummary
        title={a.clientName}
        chips={
          <>
            <ReferenceChip label="ARN" value={a.arn} />
            <StatusBadge status={a.status} />
          </>
        }
        flags={accountFlags(a)}
        facts={accountFacts(a)}
      />
      <WorkflowPanel
        entityType={ACCOUNT_ENTITY}
        entityId={a.id}
        showHistory={false}
        onChanged={() => void refresh()}
        renderBusinessActions={(actions) => <AccountActions account={a} actions={actions} />}
      />
      {EDITABLE.has(a.status) && <AccountCheckPanel accountId={a.id} />}
      <Tabs tabs={TABS} active={tab} onChange={setTab} />
      <TabBody tab={tab} account={a} />
    </div>
  );
}
