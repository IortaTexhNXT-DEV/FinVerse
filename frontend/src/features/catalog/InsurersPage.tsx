import { useQuery } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { catalogApi } from '@/api/catalog';
import type { Insurer } from '@/api/catalog';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate, humanize, today } from '@/utils/format';
import { InsurerEditorModal } from './InsurerEditorModal';
import { RecordActions } from './RecordActions';

function accreditation(i: Insurer) {
  const expired = i.accreditedUntil !== undefined && i.accreditedUntil < today();
  return (
    <span className={expired ? 'field-error' : undefined}>
      {formatDate(i.accreditedUntil)}
      {expired && ' (expired)'}
    </span>
  );
}

/** Insurer panel (BRNB.005-006): accredited insurers, their placement channel and contacts. */
export default function InsurersPage() {
  const companyId = useCompanyId();
  const navigate = useNavigate();
  const { can } = useAuth();
  const [creating, setCreating] = useState(false);
  const insurers = useQuery({
    queryKey: ['catalog', 'insurers', companyId],
    queryFn: () => catalogApi.insurers(companyId),
  });
  return (
    <div className="stack">
      <PageHeader
        section="Products & Insurers"
        title="Insurers"
        description="The insurer panel: accreditation, placement channel and e-mails, branches with their local government tax, and commission rates."
        actions={
          can('MASTER_MAINTAIN') && (
            <Button variant="accent" icon={<Plus size={16} />} onClick={() => setCreating(true)}>
              New insurer
            </Button>
          )
        }
      />
      <ErrorAlert error={insurers.error} />
      <Card flush>
        <DataTable<Insurer>
          loading={insurers.isLoading}
          rows={insurers.data ?? []}
          rowKey={(i) => i.id}
          onRowClick={(i) => void navigate(`/catalog/insurers/${i.id}`)}
          emptyMessage="No insurer on the panel yet."
          columns={[
            { key: 'c', header: 'Code', render: (i) => <strong>{i.partyCode}</strong> },
            { key: 'n', header: 'Name', render: (i) => i.name },
            { key: 'a', header: 'Accredited until', render: accreditation },
            { key: 'p', header: 'Placement', render: (i) => humanize(i.placementChannel) },
            { key: 'e', header: 'Placement e-mails', render: (i) => i.placementEmails.join(', ') },
            { key: 'd', header: 'Credit days', numeric: true, render: (i) => i.defaultCreditDays },
            { key: 's', header: 'Status', render: (i) => <StatusBadge status={i.recordStatus} /> },
            {
              key: 'x',
              header: 'Actions',
              render: (i) => (
                <RecordActions kind="INSURER" record={i} refresh={[['catalog', 'insurers']]} />
              ),
            },
          ]}
        />
      </Card>
      {creating && <InsurerEditorModal companyId={companyId} onClose={() => setCreating(false)} />}
    </div>
  );
}
