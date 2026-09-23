import { useMutation } from '@tanstack/react-query';
import { Play } from 'lucide-react';
import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { accountingApi } from '@/api/accounting';
import type { JournalLineInput } from '@/api/gl';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { useWorkspace } from '@/context/workspaceContext';
import { today } from '@/utils/format';
import { components, optionalHeader, roles, simulationTotals, toAmounts } from './ruleModel';
import { useAccountingLookups } from './useAccountingLookups';

/**
 * Rule simulator: enter sample amounts for an event and preview the journal lines the engine would
 * post, without posting anything.
 */
export default function SimulatorPage() {
  const [params] = useSearchParams();
  const { eventTypes, rules, companyId } = useAccountingLookups();
  const { branches, branchId, company } = useWorkspace();
  const [eventType, setEventType] = useState(params.get('eventType') ?? '');
  const [header, setHeader] = useState({
    valueDate: today(),
    currency: '',
    businessLine: '',
    partyCode: '',
  });
  const [amounts, setAmounts] = useState<Record<string, string>>({});
  const [accounts, setAccounts] = useState<Record<string, string>>({});
  const selected = eventTypes.find((t) => t.code === eventType) ?? eventTypes[0];
  const comps = components(selected);
  const roleNames = roles(
    rules.filter((r) => r.eventType === selected?.code).flatMap((r) => r.lines),
  );
  const run = useMutation({ mutationFn: accountingApi.simulate });
  const simulate = () => {
    run.mutate({
      ...optionalHeader(header, company?.baseCurrency),
      companyId,
      branchId: branchId ?? branches[0]?.id ?? 0,
      eventType: selected?.code ?? '',
      amounts: toAmounts(comps, amounts),
      accounts,
    });
  };
  const lines = run.data?.lines ?? [];
  const totals = simulationTotals(lines);

  return (
    <div className="stack">
      <PageHeader
        section="Accounting Engine"
        title="Rule Simulator"
        description="Preview the journal an event would generate with the rules in force. Nothing is posted."
        actions={
          <Button
            variant="accent"
            icon={<Play size={16} />}
            busy={run.isPending}
            onClick={simulate}
          >
            Simulate
          </Button>
        }
      />
      <Card title="Sample event">
        <div className="form-grid">
          <Field label="Event type" required>
            {(id) => (
              <select
                id={id}
                className="select"
                value={selected?.code ?? ''}
                onChange={(e) => setEventType(e.target.value)}
              >
                {eventTypes.map((t) => (
                  <option key={t.code} value={t.code}>
                    {t.code} – {t.name}
                  </option>
                ))}
              </select>
            )}
          </Field>
          {(['valueDate', 'currency', 'businessLine', 'partyCode'] as const).map((key) => (
            <Field key={key} label={FIELD_LABELS[key]}>
              {(id) => (
                <input
                  id={id}
                  className="input"
                  type={key === 'valueDate' ? 'date' : 'text'}
                  value={header[key]}
                  onChange={(e) => setHeader({ ...header, [key]: e.target.value })}
                />
              )}
            </Field>
          ))}
        </div>
      </Card>
      <div className="grid-2">
        <Card title="Amount components">
          <div className="form-grid">
            {comps.map((c) => (
              <Field key={c} label={c}>
                {(id) => (
                  <input
                    id={id}
                    className="input"
                    type="number"
                    step="0.01"
                    value={amounts[c] ?? ''}
                    onChange={(e) => setAmounts({ ...amounts, [c]: e.target.value })}
                  />
                )}
              </Field>
            ))}
          </div>
        </Card>
        <Card title="Account roles">
          {roleNames.length === 0 && <p className="muted">The rules of this event use no roles.</p>}
          <div className="form-grid">
            {roleNames.map((r) => (
              <Field key={r} label={`@${r} account`}>
                {(id) => (
                  <input
                    id={id}
                    className="input"
                    value={accounts[r] ?? ''}
                    onChange={(e) => setAccounts({ ...accounts, [r]: e.target.value.trim() })}
                  />
                )}
              </Field>
            ))}
          </div>
        </Card>
      </div>
      <ErrorAlert error={run.error} />
      {run.data !== undefined && (
        <Card
          flush
          title={`Rule applied: ${run.data.ruleName}`}
          actions={
            <span className="muted">
              Debits <Amount value={totals.debit} /> · Credits <Amount value={totals.credit} />
            </span>
          }
        >
          <DataTable<JournalLineInput & { n: number }>
            rows={lines.map((l, i) => ({ ...l, n: i + 1 }))}
            rowKey={(l) => l.n}
            caption="Simulated journal lines"
            columns={[
              { key: 'n', header: '#', render: (l) => l.n },
              { key: 'a', header: 'Account', render: (l) => <strong>{l.accountCode}</strong> },
              { key: 's', header: 'Dr / Cr', render: (l) => (l.side === 'DEBIT' ? 'Dr' : 'Cr') },
              {
                key: 'm',
                header: 'Amount',
                numeric: true,
                render: (l) => <Amount value={l.amount} />,
              },
              { key: 'c', header: 'Currency', render: (l) => l.currency ?? '' },
              { key: 'p', header: 'Party', render: (l) => l.partyCode ?? '' },
              { key: 'b', header: 'LoB', render: (l) => l.businessLine ?? '' },
              { key: 'r', header: 'Narration', render: (l) => l.narration ?? '' },
            ]}
          />
        </Card>
      )}
    </div>
  );
}

const FIELD_LABELS = {
  valueDate: 'Value date',
  currency: 'Currency (blank = base)',
  businessLine: 'Line of business',
  partyCode: 'Party code',
} as const;
