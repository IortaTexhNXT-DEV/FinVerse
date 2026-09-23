import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { alertsApi } from '@/api/alerts';
import type { AlertSeverity, ExceptionCodeInfo } from '@/api/alerts';
import { useAuth } from '@/auth/authContext';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { formatDateTime } from '@/utils/format';
import { SeverityBadge } from './SeverityBadge';

const SEVERITIES: AlertSeverity[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

function optionalNumber(text: string): number | undefined {
  return text.trim() === '' ? undefined : Number(text);
}

/**
 * Exception Codes Master: the monitored exception conditions with severity and thresholds. The
 * rules themselves are part of the system; administrators tune severity, thresholds and activation.
 */
export default function ExceptionCodesPage() {
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState<ExceptionCodeInfo | null>(null);
  const codes = useQuery({ queryKey: ['exception-codes'], queryFn: alertsApi.codes });
  const canEdit = can('SYSTEM_PARAMETER_MANAGE');

  const save = useMutation({
    mutationFn: (c: ExceptionCodeInfo) =>
      alertsApi.configure(c.code, {
        severity: c.severity,
        thresholdAmount: c.thresholdAmount,
        thresholdDays: c.thresholdDays,
        active: c.active,
      }),
    onSuccess: async (c) => {
      setEditing(null);
      await queryClient.invalidateQueries({ queryKey: ['exception-codes'] });
      toast.success(`${c.code} updated`);
    },
  });

  return (
    <div className="stack">
      <PageHeader
        section="Administration"
        title="Exception Codes"
        description="Conditions monitored by the alert engine. Thresholds apply immediately to postings and to the next daily check."
      />
      <ErrorAlert error={codes.error} />
      <Card flush>
        <DataTable<ExceptionCodeInfo>
          loading={codes.isLoading}
          rows={codes.data ?? []}
          rowKey={(c) => c.code}
          onRowClick={canEdit ? (c) => setEditing(c) : undefined}
          columns={[
            { key: 'c', header: 'Code', render: (c) => <strong>{c.code}</strong> },
            { key: 'n', header: 'Name', render: (c) => c.name },
            { key: 'm', header: 'Module', render: (c) => c.module },
            {
              key: 's',
              header: 'Severity',
              render: (c) => <SeverityBadge severity={c.severity} />,
            },
            {
              key: 'a',
              header: 'Threshold amount',
              numeric: true,
              render: (c) =>
                c.thresholdAmount === undefined ? '' : <Amount value={c.thresholdAmount} />,
            },
            {
              key: 'd',
              header: 'Threshold days',
              numeric: true,
              render: (c) => c.thresholdDays ?? '',
            },
            {
              key: 'x',
              header: 'Status',
              render: (c) => <StatusBadge status={c.active ? 'ACTIVE' : 'INACTIVE'} />,
            },
            { key: 'u', header: 'Last changed', render: (c) => formatDateTime(c.updatedAt) },
          ]}
        />
      </Card>
      <Modal
        title={`Exception code ${editing?.code ?? ''}`}
        open={editing !== null}
        onClose={() => setEditing(null)}
        footer={
          <Button
            variant="accent"
            busy={save.isPending}
            onClick={() => editing && save.mutate(editing)}
          >
            Save
          </Button>
        }
      >
        <ErrorAlert error={save.error} />
        {editing !== null && (
          <div className="stack">
            <p className="muted">{editing.description}</p>
            <div className="form-grid">
              <Field label="Severity" required>
                {(id) => (
                  <select
                    id={id}
                    className="select"
                    value={editing.severity}
                    onChange={(e) =>
                      setEditing({ ...editing, severity: e.target.value as AlertSeverity })
                    }
                  >
                    {SEVERITIES.map((s) => (
                      <option key={s} value={s}>
                        {s}
                      </option>
                    ))}
                  </select>
                )}
              </Field>
              <Field label="Threshold amount" hint="Blank = not used">
                {(id) => (
                  <input
                    id={id}
                    className="input num"
                    type="number"
                    min={0}
                    value={editing.thresholdAmount ?? ''}
                    onChange={(e) =>
                      setEditing({ ...editing, thresholdAmount: optionalNumber(e.target.value) })
                    }
                  />
                )}
              </Field>
              <Field label="Threshold days" hint="Blank = not used">
                {(id) => (
                  <input
                    id={id}
                    className="input num"
                    type="number"
                    min={0}
                    value={editing.thresholdDays ?? ''}
                    onChange={(e) =>
                      setEditing({ ...editing, thresholdDays: optionalNumber(e.target.value) })
                    }
                  />
                )}
              </Field>
            </div>
            <label className="checkbox">
              <input
                type="checkbox"
                checked={editing.active}
                onChange={(e) => setEditing({ ...editing, active: e.target.checked })}
              />
              Active (monitored)
            </label>
          </div>
        )}
      </Modal>
    </div>
  );
}
