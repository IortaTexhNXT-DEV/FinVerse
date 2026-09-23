import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { holidaysApi } from '@/api/holidays';
import type { Holiday, HolidayInput } from '@/api/holidays';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { useToast } from '@/components/ui/toastContext';
import { useWorkspace } from '@/context/workspaceContext';
import { formatDate } from '@/utils/format';

const WEEKDAY = new Intl.DateTimeFormat('en-PH', { weekday: 'long', timeZone: 'UTC' });

function weekday(iso: string): string {
  return WEEKDAY.format(new Date(`${iso}T00:00:00Z`));
}

/**
 * Holiday calendar per company (optionally per branch). Holidays and the branches' weekly
 * holidays drive working-day checks such as the WEEKEND_POSTING exception.
 */
export default function HolidaysPage() {
  const { company, branches } = useWorkspace();
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [year, setYear] = useState(new Date().getFullYear());
  const [form, setForm] = useState<HolidayInput | null>(null);
  const companyId = company?.id ?? 0;

  const holidays = useQuery({
    queryKey: ['holidays', companyId, year],
    queryFn: () => holidaysApi.list(companyId, year),
    enabled: companyId > 0,
  });
  const add = useMutation({
    mutationFn: holidaysApi.add,
    onSuccess: async (h) => {
      setForm(null);
      await queryClient.invalidateQueries({ queryKey: ['holidays'] });
      toast.success(`${h.description} added`);
    },
  });
  const branchName = (id?: number) =>
    id === undefined ? 'All branches' : (branches.find((b) => b.id === id)?.name ?? `#${id}`);

  return (
    <div className="stack">
      <PageHeader
        section="Setup"
        title="Holiday Calendar"
        description="Public and company holidays. Leave the branch empty for a company-wide holiday."
        actions={
          <>
            <label className="visually-hidden" htmlFor="holiday-year">
              Year
            </label>
            <select
              id="holiday-year"
              className="select"
              value={year}
              onChange={(e) => setYear(Number(e.target.value))}
            >
              {[year - 1, year, year + 1].map((y) => (
                <option key={y} value={y}>
                  {y}
                </option>
              ))}
            </select>
            {can('MASTER_MAINTAIN') && (
              <Button
                variant="accent"
                icon={<Plus size={16} />}
                onClick={() =>
                  setForm({ companyId, holidayDate: `${year}-01-01`, description: '' })
                }
              >
                Add holiday
              </Button>
            )}
          </>
        }
      />
      <ErrorAlert error={holidays.error} />
      <Card flush>
        <DataTable<Holiday>
          loading={holidays.isLoading}
          rows={holidays.data ?? []}
          rowKey={(h) => h.id}
          emptyMessage={`No holidays defined for ${year}.`}
          columns={[
            {
              key: 'd',
              header: 'Date',
              render: (h) => <strong>{formatDate(h.holidayDate)}</strong>,
            },
            { key: 'w', header: 'Weekday', render: (h) => weekday(h.holidayDate) },
            { key: 'n', header: 'Holiday', render: (h) => h.description },
            { key: 'b', header: 'Applies to', render: (h) => branchName(h.branchId) },
          ]}
        />
      </Card>
      <Modal
        title="Add holiday"
        open={form !== null}
        onClose={() => setForm(null)}
        footer={
          <Button
            variant="accent"
            busy={add.isPending}
            disabled={form === null || form.description.trim() === ''}
            onClick={() => form && add.mutate(form)}
          >
            Save
          </Button>
        }
      >
        <ErrorAlert error={add.error} />
        {form !== null && (
          <div className="form-grid">
            <Field label="Date" required>
              {(id) => (
                <input
                  id={id}
                  className="input"
                  type="date"
                  value={form.holidayDate}
                  onChange={(e) => setForm({ ...form, holidayDate: e.target.value })}
                />
              )}
            </Field>
            <Field label="Description" required>
              {(id) => (
                <input
                  id={id}
                  className="input"
                  maxLength={120}
                  value={form.description}
                  onChange={(e) => setForm({ ...form, description: e.target.value })}
                />
              )}
            </Field>
            <Field label="Branch" hint="Empty = all branches">
              {(id) => (
                <select
                  id={id}
                  className="select"
                  value={form.branchId ?? ''}
                  onChange={(e) =>
                    setForm({
                      ...form,
                      branchId: e.target.value === '' ? undefined : Number(e.target.value),
                    })
                  }
                >
                  <option value="">All branches</option>
                  {branches.map((b) => (
                    <option key={b.id} value={b.id}>
                      {b.code} – {b.name}
                    </option>
                  ))}
                </select>
              )}
            </Field>
          </div>
        )}
      </Modal>
    </div>
  );
}
