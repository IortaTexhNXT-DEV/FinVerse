import { useQuery } from '@tanstack/react-query';
import { Pencil, Plus } from 'lucide-react';
import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { catalogApi } from '@/api/catalog';
import type { Commission, InsurerBranch, InsurerDetail } from '@/api/catalog';
import type { InsurerBankAccount, InsurerContact } from '@/api/insurerPage';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate, formatRate } from '@/utils/format';
import { ChangeHistory } from '@/components/history/ChangeHistory';
import { Tabs } from '@/components/ui/Tabs';
import { useTabParam } from '@/components/ui/useTabParam';
import { DetailList } from './DetailList';
import { BankAccountModal, ContactModal, RemittanceModal } from './InsurerPageModals';
import { BankAccountsTable, ContactsTable, LinkedProductsTable } from './InsurerPageTables';
import { remittanceCycleLabel, remittanceModeLabel } from './insurerPageForm';
import { InsurerAccountsTab } from './InsurerAccountsTab';
import { InsurerEditorModal } from './InsurerEditorModal';
import { BranchModal, CommissionModal } from './InsurerRatesModals';
import { RecordActions } from './RecordActions';
import { UserName } from '@/components/ui/UserName';
import { insurerTaxStatusLabel, placementChannelLabel } from './insurerForm';
import { ProductName } from '@/components/broking/LovLabel';
import { LoadingPanel } from '@/components/ui/LoadingPanel';

type NewKind = 'branch' | 'commission' | 'remittance' | 'contact' | 'account';

type Dialog =
  | { kind: 'edit' | NewKind }
  | { kind: 'contact'; contact: InsurerContact }
  | { kind: 'account'; account: InsurerBankAccount }
  | null;

const TABS = [
  { id: 'details', label: 'Details' },
  { id: 'contacts', label: 'Contacts' },
  { id: 'remittance', label: 'Payment & Remittance' },
  { id: 'accounts', label: 'Bank Accounts' },
  { id: 'products', label: 'Products' },
  { id: 'ledger', label: 'Ledger Accounts' },
  { id: 'branches', label: 'Branches & Commissions' },
  { id: 'changes', label: 'Change History' },
] as const;

type TabId = (typeof TABS)[number]['id'];

const REFRESH = [['catalog']] as const;

function Profile({ detail }: Readonly<{ detail: InsurerDetail }>) {
  const i = detail.insurer;
  return (
    <DetailList
      rows={[
        ['Partner code', i.partyCode],
        ['Short name', i.shortName],
        ['IC accreditation', i.accreditationNo],
        ['Accredited until', formatDate(i.accreditedUntil)],
        ['Placement channel', placementChannelLabel(i.placementChannel)],
        ['Placement e-mails', i.placementEmails.join(', ')],
        ['Credit days', i.defaultCreditDays],
        ['Tax status', insurerTaxStatusLabel(i.taxStatus)],
        ['Maker', <UserName key="maker" login={i.maker} />],
        ['Authorized by', <UserName key="a" login={i.authorizedBy} />],
      ]}
    />
  );
}

function Branches({ rows }: Readonly<{ rows: InsurerBranch[] }>) {
  return (
    <DataTable<InsurerBranch>
      rows={rows}
      rowKey={(b) => b.id}
      emptyMessage="No branch recorded."
      columns={[
        { key: 'c', header: 'Code', render: (b) => <strong>{b.code}</strong> },
        { key: 'n', header: 'Name', render: (b) => b.name },
        { key: 'y', header: 'City', render: (b) => b.city ?? '' },
        { key: 'l', header: 'LGT %', numeric: true, render: (b) => formatRate(b.lgtRate) },
        { key: 'e', header: 'Placement e-mail', render: (b) => b.placementEmail ?? '' },
        { key: 's', header: 'Status', render: (b) => <StatusBadge status={b.recordStatus} /> },
        {
          key: 'x',
          header: 'Actions',
          render: (b) => (
            <RecordActions kind="INSURER_BRANCH" record={b} label={b.name} refresh={REFRESH} />
          ),
        },
      ]}
    />
  );
}

function Commissions({ rows }: Readonly<{ rows: Commission[] }>) {
  return (
    <DataTable<Commission>
      rows={rows}
      rowKey={(c) => c.id}
      emptyMessage="No commission rate: the product default applies."
      columns={[
        {
          key: 'p',
          header: 'Product',
          render: (c) => <ProductName withCode code={c.productCode} empty="All products" />,
        },
        { key: 'r', header: 'Commission %', numeric: true, render: (c) => formatRate(c.rate) },
        { key: 'f', header: 'From', render: (c) => formatDate(c.effectiveFrom) },
        { key: 't', header: 'To', render: (c) => formatDate(c.effectiveTo) || 'Open' },
        { key: 's', header: 'Status', render: (c) => <StatusBadge status={c.recordStatus} /> },
        {
          key: 'x',
          header: 'Actions',
          render: (c) => (
            <RecordActions
              kind="COMMISSION_RATE"
              record={c}
              label={`Commission rate ${c.productCode ?? 'all products'} from ${formatDate(c.effectiveFrom)}`}
              refresh={REFRESH}
            />
          ),
        },
      ]}
    />
  );
}

function Remittance({ detail }: Readonly<{ detail: InsurerDetail }>) {
  const r = detail.insurer.remittance ?? {};
  return (
    <DetailList
      rows={[
        ['Payment terms', r.paymentTerms ?? ''],
        ['Credit days', detail.insurer.defaultCreditDays],
        ['Remittance cycle', remittanceCycleLabel(r.cycle)],
        ['Remittance mode', remittanceModeLabel(r.mode)],
        ['Remittance advice e-mail', r.email ?? ''],
        ['Remittance currency', r.currency ?? ''],
      ]}
    />
  );
}

interface TabProps {
  tab: TabId;
  detail: InsurerDetail;
  maintain: boolean;
  onDialog: (dialog: Dialog) => void;
}

function TabContent({ tab, detail: d, maintain, onDialog }: Readonly<TabProps>) {
  const open = (kind: NewKind, label: string) =>
    maintain && (
      <Button
        size="sm"
        variant="secondary"
        icon={kind === 'remittance' ? <Pencil size={14} /> : <Plus size={14} />}
        onClick={() => onDialog({ kind })}
      >
        {label}
      </Button>
    );
  switch (tab) {
    case 'contacts':
      return (
        <Card title="Contacts" flush actions={open('contact', 'Add contact')}>
          <ContactsTable
            rows={d.contacts}
            onEdit={maintain ? (contact) => onDialog({ kind: 'contact', contact }) : undefined}
          />
        </Card>
      );
    case 'remittance':
      return (
        <Card title="Payment terms & remittance" actions={open('remittance', 'Edit')}>
          <Remittance detail={d} />
        </Card>
      );
    case 'accounts':
      return (
        <Card title="Bank accounts" flush actions={open('account', 'Add account')}>
          <BankAccountsTable
            rows={d.bankAccounts}
            onEdit={maintain ? (account) => onDialog({ kind: 'account', account }) : undefined}
          />
        </Card>
      );
    case 'products':
      return (
        <Card title="Products linked" flush>
          <LinkedProductsTable rows={d.products} />
        </Card>
      );
    case 'branches':
      return (
        <>
          <Card title="Branches" flush actions={open('branch', 'Add branch')}>
            <Branches rows={d.branches} />
          </Card>
          <Card title="Commission rates" flush actions={open('commission', 'Add rate')}>
            <Commissions rows={d.commissions} />
          </Card>
        </>
      );
    case 'ledger':
      return <InsurerAccountsTab insurerId={d.insurer.id} />;
    case 'changes':
      return <ChangeHistory entityType="InsurerProfile" entityId={d.insurer.id} />;
    default:
      return (
        <Card title="Profile">
          <Profile detail={d} />
        </Card>
      );
  }
}

interface DialogProps {
  dialog: Dialog;
  detail: InsurerDetail;
  companyId: number;
  onClose: () => void;
}

function InsurerDialog({ dialog, detail: d, companyId, onClose }: Readonly<DialogProps>) {
  const id = d.insurer.id;
  switch (dialog?.kind) {
    case 'edit':
      return <InsurerEditorModal companyId={companyId} insurer={d.insurer} onClose={onClose} />;
    case 'branch':
      return <BranchModal insurerId={id} onClose={onClose} />;
    case 'commission':
      return <CommissionModal insurerId={id} onClose={onClose} />;
    case 'remittance':
      return <RemittanceModal insurerId={id} terms={d.insurer.remittance} onClose={onClose} />;
    case 'contact':
      return (
        <ContactModal
          insurerId={id}
          contact={'contact' in dialog ? dialog.contact : undefined}
          onClose={onClose}
        />
      );
    case 'account':
      return (
        <BankAccountModal
          insurerId={id}
          account={'account' in dialog ? dialog.account : undefined}
          onClose={onClose}
        />
      );
    default:
      return null;
  }
}

/**
 * The full insurer page (G08): details, contacts, payment terms and remittance details, bank
 * accounts, linked products, branches with LGT and commission rates, and the field-level change
 * history. Every change is saved for authorization by another user.
 */
export default function InsurerDetailPage() {
  const id = Number(useParams().id);
  const companyId = useCompanyId();
  const { can } = useAuth();
  const [dialog, setDialog] = useState<Dialog>(null);
  const [tab, setTab] = useTabParam<TabId>(
    TABS.map((t) => t.id),
    'details',
  );
  const detail = useQuery({
    queryKey: ['catalog', 'insurer', id],
    queryFn: () => catalogApi.insurer(id),
  });
  if (detail.data === undefined) {
    return detail.error ? <ErrorAlert error={detail.error} /> : <LoadingPanel />;
  }
  const d = detail.data;
  const maintain = can('MASTER_MAINTAIN');
  return (
    <div className="stack">
      <PageHeader
        backTo="/catalog/insurers"
        section="Product Maintenance · Insurer"
        title={d.insurer.name}
        actions={
          <>
            <StatusBadge status={d.insurer.recordStatus} />
            <RecordActions
              kind="INSURER"
              record={d.insurer}
              label={d.insurer.name}
              refresh={REFRESH}
            />
            {maintain && (
              <Button
                variant="secondary"
                icon={<Pencil size={14} />}
                onClick={() => setDialog({ kind: 'edit' })}
              >
                Edit
              </Button>
            )}
          </>
        }
      />
      <Tabs tabs={TABS} active={tab} onChange={setTab} />
      <TabContent tab={tab} detail={d} maintain={maintain} onDialog={setDialog} />
      <InsurerDialog
        dialog={dialog}
        detail={d}
        companyId={companyId}
        onClose={() => setDialog(null)}
      />
    </div>
  );
}
