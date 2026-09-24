import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { nbadminApi } from '@/api/nbadmin';
import type { RetentionAction, RetentionRule, RetentionRuleInput } from '@/api/nbadmin';
import { Button } from '@/components/ui/Button';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { formatDate } from '@/utils/format';

/** Records currently eligible under a rule (drill-down). */
export function EligibleDialog({
  rule,
  onClose,
}: Readonly<{ rule: RetentionRule; onClose: () => void }>) {
  const navigate = useNavigate();
  const eligible = useQuery({
    queryKey: ['nbadmin', 'eligible', rule.id],
    queryFn: () => nbadminApi.retentionEligible(rule.id),
  });
  const data = eligible.data;
  return (
    <Modal open title={`${rule.recordType}: eligible records`} onClose={onClose}>
      <div className="stack">
        <ErrorAlert error={eligible.error} />
        {data && !data.providerAvailable && (
          <div className="alert warning">
            The {rule.recordType.toLowerCase()} module does not report retention candidates yet; the
            records will be counted once it does.
          </div>
        )}
        {data && (
          <p className="muted" style={{ margin: 0 }}>
            Statuses {rule.statuses}, last activity on or before {formatDate(data.cutoff)} (up to
            200 records, oldest first).
          </p>
        )}
        <DataTable
          loading={eligible.isLoading}
          rows={data?.records ?? []}
          rowKey={(r) => r.reference}
          onRowClick={(r) => {
            if (r.link) {
              void navigate(r.link);
            }
          }}
          emptyMessage="No record is eligible."
          columns={[
            {
              key: 'ref',
              header: 'Reference',
              render: (r) => <span className="mono">{r.reference}</span>,
            },
            { key: 'desc', header: 'Description', render: (r) => r.description ?? '' },
            { key: 'status', header: 'Status', render: (r) => <StatusBadge status={r.status} /> },
            { key: 'last', header: 'Last activity', render: (r) => formatDate(r.lastActivity) },
          ]}
        />
      </div>
    </Modal>
  );
}

/** Field errors of a retention rule. */
function ruleErrors(f: RetentionRuleInput): Partial<Record<keyof RetentionRuleInput, string>> {
  const errors: Partial<Record<keyof RetentionRuleInput, string>> = {};
  if (f.statuses.trim() === '') {
    errors.statuses = 'Enter at least one status';
  }
  if (!Number.isInteger(f.yearsOnline) || f.yearsOnline < 1 || f.yearsOnline > 50) {
    errors.yearsOnline = 'Between 1 and 50 years';
  }
  if (!Number.isInteger(f.yearsArchive) || f.yearsArchive < 0 || f.yearsArchive > 50) {
    errors.yearsArchive = 'Between 0 and 50 years';
  }
  if (f.description.trim() === '') {
    errors.description = 'Enter a description';
  }
  return errors;
}

/** Change a retention rule (years, statuses, action, active). */
export function RuleDialog({
  rule,
  onClose,
}: Readonly<{ rule: RetentionRule; onClose: () => void }>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [form, setForm] = useState<RetentionRuleInput>({
    statuses: rule.statuses,
    yearsOnline: rule.yearsOnline,
    yearsArchive: rule.yearsArchive,
    action: rule.action,
    active: rule.active,
    description: rule.description,
  });
  const set = (patch: Partial<RetentionRuleInput>) => setForm((f) => ({ ...f, ...patch }));
  const errors = ruleErrors(form);
  const save = useMutation({
    mutationFn: () => nbadminApi.updateRetentionRule(rule.id, form),
    onSuccess: async () => {
      toast.success(`${rule.recordType} rule saved`);
      await queryClient.invalidateQueries({ queryKey: ['nbadmin', 'retention'] });
      onClose();
    },
  });
  return (
    <Modal
      open
      title={`${rule.recordType} retention rule`}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            variant="accent"
            busy={save.isPending}
            disabled={Object.keys(errors).length > 0}
            onClick={() => save.mutate()}
          >
            Save rule
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={save.error} />
        <div className="form-grid">
          <Field
            label="Statuses"
            required
            error={errors.statuses}
            hint="Comma separated, e.g. NOT_PROCEEDED,VOIDED"
          >
            {(id) => (
              <input
                id={id}
                className="input"
                value={form.statuses}
                onChange={(e) => set({ statuses: e.target.value })}
              />
            )}
          </Field>
          <Field label="Years online" required error={errors.yearsOnline}>
            {(id) => (
              <input
                id={id}
                className="input"
                type="number"
                min={1}
                max={50}
                value={form.yearsOnline}
                onChange={(e) => set({ yearsOnline: Number(e.target.value) })}
              />
            )}
          </Field>
          <Field label="Years in archive" required error={errors.yearsArchive}>
            {(id) => (
              <input
                id={id}
                className="input"
                type="number"
                min={0}
                max={50}
                value={form.yearsArchive}
                onChange={(e) => set({ yearsArchive: Number(e.target.value) })}
              />
            )}
          </Field>
          <Field label="Action">
            {(id) => (
              <select
                id={id}
                className="select"
                value={form.action}
                onChange={(e) => set({ action: e.target.value as RetentionAction })}
              >
                <option value="REVIEW">Review</option>
                <option value="ARCHIVE">Archive</option>
              </select>
            )}
          </Field>
          <Field label="Active">
            {(id) => (
              <label className="checkbox-field" htmlFor={id}>
                <input
                  id={id}
                  type="checkbox"
                  checked={form.active}
                  onChange={(e) => set({ active: e.target.checked })}
                />
                Evaluated by the monthly review
              </label>
            )}
          </Field>
        </div>
        <Field label="Description" required error={errors.description}>
          {(id) => (
            <input
              id={id}
              className="input"
              value={form.description}
              onChange={(e) => set({ description: e.target.value })}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}
