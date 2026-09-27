import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Copy, Save } from 'lucide-react';
import { useState } from 'react';
import type { Currency, ExchangeRate } from '@/api/masters';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { useToast } from '@/components/ui/toastContext';
import { formatDate, today } from '@/utils/format';
import { frbsSetupApi } from './frbsSetupApi';

const previousMonth = (): string => {
  const d = new Date(`${today()}T00:00:00`);
  d.setDate(0);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
};

/**
 * Monthly revaluation rate (FRBS 2.2.0): the GL Team Head enters the month-end rate of each
 * currency, kept as the CLOSING rate on the last day of the month and used by the FX revaluation;
 * it becomes the Operations BOOK rate of the next month (job BOOK_RATE_FROM_CLOSING, or Copy to
 * BOOK here).
 */
export function RevaluationRatesCard({ currencies }: Readonly<{ currencies: Currency[] }>) {
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const maintainer = can('REVALUATION_RATE_MAINTAIN');
  const [month, setMonth] = useState(previousMonth());
  const [currency, setCurrency] = useState('USD');
  const [rate, setRate] = useState('');
  const year = Number(month.slice(0, 4));

  const rates = useQuery({
    queryKey: ['revaluation-rates', year],
    queryFn: () => frbsSetupApi.revaluationRates(year),
    enabled: year > 0,
  });
  const refresh = () => queryClient.invalidateQueries({ queryKey: ['revaluation-rates'] });
  const save = useMutation({
    mutationFn: () => frbsSetupApi.saveRevaluationRate(currency, month, Number(rate)),
    onSuccess: async (r) => {
      setRate('');
      await refresh();
      toast.success(`${r.currencyCode} revaluation rate of ${month} saved`);
    },
  });
  const copy = useMutation({
    mutationFn: (m: string) => frbsSetupApi.copyToBook(m),
    onSuccess: async (created) => {
      await queryClient.invalidateQueries({ queryKey: ['rates'] });
      toast.success(`${created.length} BOOK rate(s) created for the next month`);
    },
  });
  const rateOk = Number(rate) > 0;
  const rateError = rate !== '' && !rateOk ? 'Enter a positive rate' : undefined;

  return (
    <Card title="Monthly revaluation rates" flush>
      <div className="stack" style={{ padding: 16 }}>
        <ErrorAlert error={rates.error ?? save.error ?? copy.error} />
        {maintainer && (
          <div className="form-grid">
            <Field label="Month" required>
              {(id) => (
                <input
                  id={id}
                  className="input"
                  type="month"
                  value={month}
                  onChange={(e) => setMonth(e.target.value)}
                />
              )}
            </Field>
            <Field label="Currency" required>
              {(id) => (
                <select
                  id={id}
                  className="select"
                  value={currency}
                  onChange={(e) => setCurrency(e.target.value)}
                >
                  {currencies.map((c) => (
                    <option key={c.code} value={c.code}>
                      {c.code} – {c.name}
                    </option>
                  ))}
                </select>
              )}
            </Field>
            <Field label="Month-end rate" required error={rateError}>
              {(id) => (
                <input
                  id={id}
                  className="input num"
                  type="number"
                  step="0.000001"
                  value={rate}
                  onChange={(e) => setRate(e.target.value)}
                />
              )}
            </Field>
            <Button
              variant="accent"
              icon={<Save size={16} />}
              busy={save.isPending}
              disabled={!rateOk || month === ''}
              onClick={() => save.mutate()}
              style={{ alignSelf: 'end' }}
            >
              Save Rate
            </Button>
          </div>
        )}
      </div>
      <DataTable<ExchangeRate>
        loading={rates.isLoading}
        rows={rates.data ?? []}
        rowKey={(r) => r.id}
        caption="Revaluation rates"
        emptyMessage={`No revaluation rate entered for ${year}`}
        columns={[
          { key: 'm', header: 'Month End', render: (r) => formatDate(r.effectiveDate) },
          { key: 'c', header: 'Currency', render: (r) => <strong>{r.currencyCode}</strong> },
          { key: 'r', header: 'Rate', numeric: true, render: (r) => r.rate.toFixed(6) },
          { key: 'b', header: 'Entered By', render: (r) => r.createdBy },
          {
            key: 'copy',
            header: '',
            render: (r) =>
              maintainer && (
                <Button
                  size="sm"
                  variant="ghost"
                  icon={<Copy size={14} />}
                  busy={copy.isPending}
                  onClick={() => copy.mutate(r.effectiveDate.slice(0, 7))}
                >
                  Copy to BOOK
                </Button>
              ),
          },
        ]}
      />
    </Card>
  );
}
