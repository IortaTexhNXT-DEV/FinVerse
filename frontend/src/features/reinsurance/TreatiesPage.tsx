import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { reinsuranceApi } from '@/api/reinsurance';
import type { Treaty } from '@/api/reinsurance';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { humanize } from '@/utils/format';
import { TreatyEditor } from './TreatyEditor';
import { blankTreaty, capacityLabel, toForm, toRequest, treatyProblems } from './treatyForm';
import type { TreatyForm } from './treatyForm';
import { useRiLookups } from './useRiLookups';
import { awaitsOtherChecker } from '@/utils/makerChecker';

/** Treaty programme per class and underwriting year (maker-checker master data). */
export default function TreatiesPage() {
  const lookups = useRiLookups();
  const { can, user } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [form, setForm] = useState<TreatyForm | null>(null);
  const refresh = () => queryClient.invalidateQueries({ queryKey: ['ri-treaties'] });

  const save = useMutation({
    mutationFn: (f: TreatyForm) =>
      f.id === undefined
        ? reinsuranceApi.createTreaty(toRequest(f))
        : reinsuranceApi.updateTreaty(f.id, toRequest(f)),
    onSuccess: async (t) => {
      await refresh();
      setForm(null);
      toast.success(`Treaty ${t.code} saved – pending authorization`);
    },
  });
  const authorize = useMutation({
    mutationFn: (id: number) => reinsuranceApi.authorizeTreaty(id),
    onSuccess: async (t) => {
      await refresh();
      toast.success(`Treaty ${t.code} authorized`);
    },
  });
  const problems = form === null ? [] : treatyProblems(form);
  const newTreaty = () =>
    setForm(blankTreaty(lookups.companyId, lookups.baseCurrency, new Date().getFullYear()));

  return (
    <div className="stack">
      <PageHeader
        section="Reinsurance"
        title="Treaties"
        description="Quota share, surplus and excess of loss treaties per class and underwriting year, with participants and layers. Changes need authorization."
        actions={
          can('REINSURANCE_MAINTAIN') && (
            <Button variant="accent" icon={<Plus size={16} />} onClick={newTreaty}>
              New Treaty
            </Button>
          )
        }
      />
      <ErrorAlert error={authorize.error} />
      <Card flush>
        <DataTable<Treaty>
          rows={lookups.treaties}
          loading={lookups.treatiesLoading}
          rowKey={(t) => t.id}
          onRowClick={can('REINSURANCE_MAINTAIN') ? (t) => setForm(toForm(t)) : undefined}
          caption="Treaties"
          columns={[
            { key: 'c', header: 'Code', render: (t) => <strong>{t.code}</strong> },
            { key: 'n', header: 'Name', render: (t) => t.name },
            { key: 'y', header: 'UW Year', render: (t) => t.uwYear },
            { key: 'l', header: 'Class', render: (t) => t.businessLine },
            { key: 't', header: 'Type', render: (t) => humanize(t.treatyType) },
            { key: 'k', header: 'Capacity', render: (t) => capacityLabel(t) },
            {
              key: 'p',
              header: 'Participants',
              render: (t) =>
                t.participants.map((p) => `${p.reinsurerCode} ${p.sharePct}%`).join(', '),
            },
            { key: 'b', header: 'Broker', render: (t) => t.brokerCode ?? 'Direct' },
            { key: 's', header: 'Status', render: (t) => <StatusBadge status={t.recordStatus} /> },
            {
              key: 'a',
              header: 'Actions',
              render: (t) =>
                awaitsOtherChecker(t, user?.username) &&
                can('REINSURANCE_AUTHORIZE') && (
                  <Button
                    size="sm"
                    variant="secondary"
                    busy={authorize.isPending}
                    onClick={(e) => {
                      e.stopPropagation();
                      authorize.mutate(t.id);
                    }}
                  >
                    Authorize
                  </Button>
                ),
            },
          ]}
        />
      </Card>
      <Modal
        title={form?.id === undefined ? 'New treaty' : `Edit treaty ${form.code}`}
        open={form !== null}
        onClose={() => setForm(null)}
        footer={
          <Button
            variant="accent"
            busy={save.isPending}
            disabled={problems.length > 0}
            onClick={() => form && save.mutate(form)}
          >
            Save for Authorization
          </Button>
        }
      >
        <ErrorAlert error={save.error} />
        {problems.length > 0 && (
          <div className="alert warning" role="status">
            {problems.map((p) => (
              <div key={p}>{p}</div>
            ))}
          </div>
        )}
        {form !== null && (
          <TreatyEditor
            form={form}
            reinsurers={lookups.reinsurers}
            brokers={lookups.brokers}
            businessLines={lookups.businessLines}
            onChange={setForm}
          />
        )}
      </Modal>
    </div>
  );
}
