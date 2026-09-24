import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { mastersApi } from '@/api/masters';
import type { ExchangeRate, RateType } from '@/api/masters';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { useToast } from '@/components/ui/toastContext';
import { formatDate, today } from '@/utils/format';

const RATE_TYPES: RateType[] = ['SPOT', 'CLOSING', 'AVERAGE', 'BUDGET', 'BOOK'];

/** Exchange rate maintenance by currency, rate type and effective date. */
export default function CurrencyRatesPage() {
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [date, setDate] = useState(today());
  const [form, setForm] = useState({
    currencyCode: 'USD',
    rateType: 'SPOT' as RateType,
    effectiveDate: today(),
    rate: '',
  });

  const currencies = useQuery({ queryKey: ['currencies'], queryFn: mastersApi.currencies });
  const rates = useQuery({
    queryKey: ['rates', date],
    queryFn: () => mastersApi.rates(date, date),
  });
  const save = useMutation({
    mutationFn: () => mastersApi.saveRate({ ...form, rate: Number(form.rate) }),
    onSuccess: async (r) => {
      await queryClient.invalidateQueries({ queryKey: ['rates'] });
      toast.success(`${r.currencyCode} ${r.rateType} rate saved`);
    },
  });

  return (
    <div className="stack">
      <PageHeader
        section="Setup"
        title="Currencies & Exchange Rates"
        description="Rates are base-currency units per one unit of foreign currency."
      />
      {can('MASTER_MAINTAIN') && (
        <Card title="Maintain rate">
          <ErrorAlert error={save.error} />
          <div className="form-grid">
            <Field label="Currency" required>
              {(id) => (
                <select
                  id={id}
                  className="select"
                  value={form.currencyCode}
                  onChange={(e) => setForm({ ...form, currencyCode: e.target.value })}
                >
                  {(currencies.data ?? []).map((c) => (
                    <option key={c.code} value={c.code}>
                      {c.code} – {c.name}
                    </option>
                  ))}
                </select>
              )}
            </Field>
            <Field label="Rate type" required>
              {(id) => (
                <select
                  id={id}
                  className="select"
                  value={form.rateType}
                  onChange={(e) => setForm({ ...form, rateType: e.target.value as RateType })}
                >
                  {RATE_TYPES.map((t) => (
                    <option key={t}>{t}</option>
                  ))}
                </select>
              )}
            </Field>
            <Field label="Effective date" required>
              {(id) => (
                <input
                  id={id}
                  className="input"
                  type="date"
                  value={form.effectiveDate}
                  onChange={(e) => setForm({ ...form, effectiveDate: e.target.value })}
                />
              )}
            </Field>
            <Field label="Rate" required>
              {(id) => (
                <input
                  id={id}
                  className="input num"
                  type="number"
                  step="0.000001"
                  value={form.rate}
                  onChange={(e) => setForm({ ...form, rate: e.target.value })}
                />
              )}
            </Field>
            <Button
              variant="accent"
              busy={save.isPending}
              disabled={Number(form.rate) <= 0}
              onClick={() => save.mutate()}
              style={{ alignSelf: 'end' }}
            >
              Save rate
            </Button>
          </div>
        </Card>
      )}
      <Card
        title="Rates in force"
        flush
        actions={
          <input
            className="input"
            type="date"
            aria-label="Rate date"
            value={date}
            onChange={(e) => setDate(e.target.value)}
          />
        }
      >
        <DataTable<ExchangeRate>
          loading={rates.isLoading}
          rows={rates.data ?? []}
          rowKey={(r) => r.id}
          columns={[
            { key: 'c', header: 'Currency', render: (r) => <strong>{r.currencyCode}</strong> },
            { key: 't', header: 'Type', render: (r) => r.rateType },
            { key: 'd', header: 'Effective', render: (r) => formatDate(r.effectiveDate) },
            { key: 'r', header: 'Rate', numeric: true, render: (r) => r.rate.toFixed(6) },
            { key: 'b', header: 'Maintained by', render: (r) => r.createdBy },
          ]}
        />
      </Card>
    </div>
  );
}
