import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { accountingApi } from '@/api/accounting';
import type { Rule } from '@/api/accounting';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { formatDate } from '@/utils/format';
import { useAccountingLookups } from './useAccountingLookups';
import { awaitsOtherChecker } from '@/utils/makerChecker';

function conditions(r: Rule): string {
  const parts = [r.businessLine ? `LoB ${r.businessLine}` : '', r.currency ?? ''];
  const text = parts.filter((p) => p !== '').join(', ');
  return text === '' ? 'Any' : text;
}

function effective(r: Rule): string {
  const to = r.effectiveTo ? ` – ${formatDate(r.effectiveTo)}` : '';
  return formatDate(r.effectiveFrom) + to;
}

function linesSummary(r: Rule): string {
  return r.lines.map((l) => `${l.side === 'DEBIT' ? 'Dr' : 'Cr'} ${l.accountCode}`).join(' · ');
}

/** Accounting rules per event type: list, authorize, open the editor. */
export default function RulesPage() {
  const { rules, eventTypes, loading, error } = useAccountingLookups();
  const { can, user } = useAuth();
  const navigate = useNavigate();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [eventType, setEventType] = useState('');
  const authorize = useMutation({
    mutationFn: (id: number) => accountingApi.authorizeRule(id),
    onSuccess: async (r) => {
      await queryClient.invalidateQueries({ queryKey: ['accounting-rules'] });
      toast.success(`Rule ${r.name} authorized`);
    },
  });
  const rows = rules
    .filter((r) => eventType === '' || r.eventType === eventType)
    .sort((a, b) => a.eventType.localeCompare(b.eventType) || a.priority - b.priority);

  return (
    <div className="stack">
      <PageHeader
        section="Accounting Engine"
        title="Accounting Rules"
        description="Which GL accounts each business event posts to. The engine picks the matching authorized rule with the lowest priority number."
        actions={
          can('ACCOUNTING_RULE_MANAGE') && (
            <Button
              variant="accent"
              icon={<Plus size={16} />}
              onClick={() => void navigate('/accounting/rules/new')}
            >
              New Rule
            </Button>
          )
        }
      />
      <Card>
        <div className="form-grid">
          <Field label="Event type">
            {(id) => (
              <select
                id={id}
                className="select"
                value={eventType}
                onChange={(e) => setEventType(e.target.value)}
              >
                <option value="">All event types</option>
                {eventTypes.map((t) => (
                  <option key={t.code} value={t.code}>
                    {t.code} – {t.name}
                  </option>
                ))}
              </select>
            )}
          </Field>
        </div>
      </Card>
      <ErrorAlert error={error ?? authorize.error} />
      <Card flush>
        <DataTable<Rule>
          loading={loading}
          rows={rows}
          rowKey={(r) => r.id}
          caption="Accounting rules"
          onRowClick={(r) => void navigate(`/accounting/rules/${r.id}`)}
          columns={[
            { key: 'e', header: 'Event Type', render: (r) => <strong>{r.eventType}</strong> },
            { key: 'n', header: 'Rule', render: (r) => r.name },
            { key: 'c', header: 'Conditions', render: conditions },
            { key: 'p', header: 'Priority', numeric: true, render: (r) => r.priority },
            {
              key: 'eff',
              header: 'Effective',
              render: effective,
            },
            { key: 'l', header: 'Lines', render: linesSummary },
            { key: 's', header: 'Status', render: (r) => <StatusBadge status={r.recordStatus} /> },
            {
              key: 'a',
              header: 'Actions',
              render: (r) =>
                awaitsOtherChecker(r, user?.username) &&
                can('MASTER_AUTHORIZE') && (
                  <Button
                    size="sm"
                    variant="secondary"
                    onClick={(e) => {
                      e.stopPropagation();
                      authorize.mutate(r.id);
                    }}
                  >
                    Authorize
                  </Button>
                ),
            },
          ]}
        />
      </Card>
    </div>
  );
}
