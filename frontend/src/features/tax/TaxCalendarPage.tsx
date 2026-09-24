import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { taxApi } from '@/api/tax';
import type { CalendarEntry } from '@/api/tax';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Kpi } from '@/components/ui/Kpi';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate, humanize, today } from '@/utils/format';
import { dueBadge } from './taxDisplay';

/** Filing calendar of a year: every form and period with its due date and due / overdue badge. */
export default function TaxCalendarPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [year, setYear] = useState(() => Number(today().slice(0, 4)));
  const [onlyOpen, setOnlyOpen] = useState(false);
  const calendar = useQuery({
    queryKey: ['tax-calendar', companyId, year],
    queryFn: () => taxApi.calendar(companyId, year),
    enabled: companyId > 0,
  });
  const prepare = useMutation({
    mutationFn: (e: CalendarEntry) => taxApi.createReturn(companyId, e.formCode, e.periodStart),
    onSuccess: async (r) => {
      await queryClient.invalidateQueries({ queryKey: ['tax-calendar'] });
      await queryClient.invalidateQueries({ queryKey: ['tax-returns'] });
      toast.success(`Return ${r.returnNo} prepared as draft`);
    },
  });
  const entries = calendar.data ?? [];
  const count = (state: string) => entries.filter((e) => e.dueState === state).length;
  const rows = onlyOpen
    ? entries.filter((e) => e.dueState === 'OVERDUE' || e.dueState === 'DUE_SOON')
    : entries;

  return (
    <div className="stack">
      <PageHeader
        section="Tax & Statutory"
        title="Tax Calendar"
        description="BIR, LGU and BFP filing obligations with due dates. Alerts are raised for returns due soon and overdue."
      />
      <Card>
        <div className="row">
          <Field label="Year">
            {(id) => (
              <input
                id={id}
                className="input"
                type="number"
                value={year}
                onChange={(e) => setYear(Number(e.target.value))}
              />
            )}
          </Field>
          <label className="row">
            <input
              type="checkbox"
              checked={onlyOpen}
              onChange={(e) => setOnlyOpen(e.target.checked)}
            />
            Due soon and overdue only
          </label>
        </div>
      </Card>
      <div className="grid-4">
        <Kpi label="Overdue" value={count('OVERDUE')} accent={count('OVERDUE') > 0} />
        <Kpi label="Due soon" value={count('DUE_SOON')} />
        <Kpi label="Paid" value={count('PAID')} />
        <Kpi label="Upcoming" value={count('UPCOMING')} />
      </div>
      <ErrorAlert error={calendar.error ?? prepare.error} />
      <Card flush>
        <DataTable<CalendarEntry>
          rows={rows}
          loading={calendar.isLoading}
          rowKey={(e) => `${e.formCode}-${e.periodStart}`}
          caption="Tax calendar"
          columns={[
            { key: 'f', header: 'Form', render: (e) => <strong>{e.formCode}</strong> },
            { key: 'n', header: 'Return', render: (e) => e.formName },
            { key: 'a', header: 'Authority', render: (e) => e.authority },
            { key: 'p', header: 'Period', render: (e) => e.periodLabel },
            { key: 'd', header: 'Due Date', render: (e) => formatDate(e.dueDate) },
            {
              key: 'b',
              header: 'Due',
              render: (e) => {
                const badge = dueBadge(e.dueState, e.daysToDue);
                return <span className={`badge ${badge.tone}`}>{badge.text}</span>;
              },
            },
            {
              key: 's',
              header: 'Return Status',
              render: (e) =>
                e.returnStatus === 'NOT_PREPARED' ? (
                  <span className="muted">{humanize(e.returnStatus)}</span>
                ) : (
                  <StatusBadge status={e.returnStatus} />
                ),
            },
            { key: 'r', header: 'Return No.', render: (e) => e.returnNo ?? '' },
            {
              key: 'x',
              header: 'Actions',
              render: (e) =>
                e.tracked && e.returnStatus === 'NOT_PREPARED' && can('TAX_MANAGE') ? (
                  <Button
                    size="sm"
                    variant="secondary"
                    busy={prepare.isPending && prepare.variables === e}
                    onClick={() => prepare.mutate(e)}
                  >
                    Prepare
                  </Button>
                ) : null,
            },
          ]}
        />
      </Card>
    </div>
  );
}
