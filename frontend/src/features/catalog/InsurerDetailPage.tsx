import { useQuery } from '@tanstack/react-query';
import { Pencil, Plus } from 'lucide-react';
import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { catalogApi } from '@/api/catalog';
import type { Commission, InsurerBranch, InsurerDetail } from '@/api/catalog';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate, humanize } from '@/utils/format';
import { DetailList } from './DetailList';
import { InsurerEditorModal } from './InsurerEditorModal';
import { BranchModal, CommissionModal } from './InsurerRatesModals';
import { RecordActions } from './RecordActions';

type Dialog = 'edit' | 'branch' | 'commission' | null;

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
        ['Placement channel', humanize(i.placementChannel)],
        ['Placement e-mails', i.placementEmails.join(', ')],
        ['Credit days', i.defaultCreditDays],
        ['Maker', i.maker],
        ['Authorized by', i.authorizedBy],
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
        { key: 'l', header: 'LGT %', numeric: true, render: (b) => b.lgtRate },
        { key: 'e', header: 'Placement e-mail', render: (b) => b.placementEmail ?? '' },
        { key: 's', header: 'Status', render: (b) => <StatusBadge status={b.recordStatus} /> },
        {
          key: 'x',
          header: 'Actions',
          render: (b) => <RecordActions kind="INSURER_BRANCH" record={b} refresh={REFRESH} />,
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
        { key: 'p', header: 'Product', render: (c) => c.productCode ?? 'All products' },
        { key: 'r', header: 'Commission %', numeric: true, render: (c) => c.rate },
        { key: 'f', header: 'From', render: (c) => formatDate(c.effectiveFrom) },
        { key: 't', header: 'To', render: (c) => formatDate(c.effectiveTo) || 'Open' },
        { key: 's', header: 'Status', render: (c) => <StatusBadge status={c.recordStatus} /> },
        {
          key: 'x',
          header: 'Actions',
          render: (c) => <RecordActions kind="COMMISSION_RATE" record={c} refresh={REFRESH} />,
        },
      ]}
    />
  );
}

/** Insurer profile: accreditation and placement, branches with LGT, commission rates. */
export default function InsurerDetailPage() {
  const id = Number(useParams().id);
  const companyId = useCompanyId();
  const { can } = useAuth();
  const [dialog, setDialog] = useState<Dialog>(null);
  const detail = useQuery({
    queryKey: ['catalog', 'insurer', id],
    queryFn: () => catalogApi.insurer(id),
  });
  if (detail.data === undefined) {
    return detail.error ? (
      <ErrorAlert error={detail.error} />
    ) : (
      <span className="spinner" aria-label="Loading" />
    );
  }
  const d = detail.data;
  const maintain = can('MASTER_MAINTAIN');
  const add = (target: Dialog, label: string) =>
    maintain && (
      <Button
        size="sm"
        variant="secondary"
        icon={<Plus size={14} />}
        onClick={() => setDialog(target)}
      >
        {label}
      </Button>
    );
  const close = () => setDialog(null);
  return (
    <div className="stack">
      <PageHeader
        section="Product Maintenance · Insurer"
        title={d.insurer.name}
        actions={
          <>
            <StatusBadge status={d.insurer.recordStatus} />
            <RecordActions kind="INSURER" record={d.insurer} refresh={REFRESH} />
            {maintain && (
              <Button
                variant="secondary"
                icon={<Pencil size={14} />}
                onClick={() => setDialog('edit')}
              >
                Edit
              </Button>
            )}
          </>
        }
      />
      <Card title="Profile">
        <Profile detail={d} />
      </Card>
      <Card title="Branches" flush actions={add('branch', 'Add branch')}>
        <Branches rows={d.branches} />
      </Card>
      <Card title="Commission rates" flush actions={add('commission', 'Add rate')}>
        <Commissions rows={d.commissions} />
      </Card>
      {dialog === 'edit' && (
        <InsurerEditorModal companyId={companyId} insurer={d.insurer} onClose={close} />
      )}
      {dialog === 'branch' && <BranchModal insurerId={id} onClose={close} />}
      {dialog === 'commission' && <CommissionModal insurerId={id} onClose={close} />}
    </div>
  );
}
