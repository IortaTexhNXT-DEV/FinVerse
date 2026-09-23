import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { PARTY_TYPES, partiesApi } from '@/api/parties';
import type { Party, PartyType } from '@/api/parties';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { useWorkspace } from '@/context/workspaceContext';
import { humanize } from '@/utils/format';
import { PartyEditorModal } from './PartyEditorModal';
import { emptyParty } from './partyForm';
import type { PartyForm } from './partyForm';
import { awaitsOtherChecker } from '@/utils/makerChecker';

/**
 * Business partner master (policyholders, intermediaries, reinsurers, coinsurers, suppliers...):
 * search by type, create and edit (maker) and authorize (checker).
 */
export default function PartiesPage() {
  const { company } = useWorkspace();
  const companyId = company?.id ?? 0;
  const { can, user } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [type, setType] = useState<PartyType | ''>('');
  const [search, setSearch] = useState('');
  const [editing, setEditing] = useState<PartyForm | null>(null);

  const query = useQuery({
    queryKey: ['parties', companyId, type, search],
    queryFn: () => partiesApi.search(companyId, type === '' ? [] : [type], search),
    enabled: companyId > 0,
  });
  const authorize = useMutation({
    mutationFn: (id: number) => partiesApi.authorize(id),
    onSuccess: async (p) => {
      await queryClient.invalidateQueries({ queryKey: ['parties'] });
      toast.success(`Party ${p.code} authorized`);
    },
  });
  const maintain = can('MASTER_MAINTAIN');

  return (
    <div className="stack">
      <PageHeader
        section="Setup"
        title="Business Partners"
        description="Policyholders, agents, brokers, reinsurers, coinsurers and suppliers used as sub-ledger parties. Changes need authorization."
        actions={
          maintain && (
            <Button
              variant="accent"
              icon={<Plus size={16} />}
              onClick={() => setEditing(emptyParty(companyId, company?.baseCurrency))}
            >
              New party
            </Button>
          )
        }
      />
      <Card>
        <div className="form-grid">
          <Field label="Party type">
            {(id) => (
              <select
                id={id}
                className="select"
                value={type}
                onChange={(e) => setType(e.target.value as PartyType | '')}
              >
                <option value="">All types</option>
                {PARTY_TYPES.map((t) => (
                  <option key={t} value={t}>
                    {humanize(t)}
                  </option>
                ))}
              </select>
            )}
          </Field>
          <Field label="Search code or name">
            {(id) => (
              <input
                id={id}
                className="input"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            )}
          </Field>
        </div>
      </Card>
      <ErrorAlert error={query.error ?? authorize.error} />
      <Card flush>
        <DataTable<Party>
          loading={query.isLoading}
          rows={query.data ?? []}
          rowKey={(p) => p.id}
          caption="Business partners"
          onRowClick={maintain ? (p) => setEditing(p) : undefined}
          columns={[
            { key: 'c', header: 'Code', render: (p) => <strong>{p.code}</strong> },
            { key: 'n', header: 'Name', render: (p) => p.name },
            { key: 't', header: 'Type', render: (p) => humanize(p.partyType) },
            { key: 'cur', header: 'Currency', render: (p) => p.defaultCurrency },
            { key: 'cd', header: 'Credit days', numeric: true, render: (p) => p.creditDays },
            { key: 'tin', header: 'TIN', render: (p) => p.taxId ?? '' },
            { key: 's', header: 'Status', render: (p) => <StatusBadge status={p.recordStatus} /> },
            {
              key: 'a',
              header: 'Actions',
              render: (p) =>
                awaitsOtherChecker(p, user?.username) &&
                can('MASTER_AUTHORIZE') && (
                  <Button
                    size="sm"
                    variant="secondary"
                    onClick={(e) => {
                      e.stopPropagation();
                      authorize.mutate(p.id);
                    }}
                  >
                    Authorize
                  </Button>
                ),
            },
          ]}
        />
      </Card>
      {editing !== null && (
        <PartyEditorModal
          key={editing.id ?? 'new'}
          initial={editing}
          onClose={() => setEditing(null)}
        />
      )}
    </div>
  );
}
