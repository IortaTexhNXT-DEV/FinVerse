import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { renewalApi } from '@/api/renewal';
import type { RiskCodeView } from '@/api/renewal';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { DateInput } from '@/components/ui/DateInput';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate } from '@/utils/format';
import { ApprovalCell } from './setupBits';
import { pending } from './setupCodes';
import { ConfirmButton } from '@/components/ui/ConfirmButton';

function blankToUndefined(value: string): string | undefined {
  return value.trim() === '' ? undefined : value.trim();
}

function initialForm(row: RiskCodeView | null) {
  return {
    code: row?.riskCode ?? '',
    line: row?.lineCode ?? '',
    reason: row?.reason ?? '',
    from: row?.effectiveFrom ?? '',
    to: row?.effectiveTo ?? '',
  };
}

function RiskCodeDialog({
  row,
  onClose,
}: Readonly<{ row: RiskCodeView | null; onClose: () => void }>) {
  const companyId = useCompanyId();
  const queryClient = useQueryClient();
  const initial = initialForm(row);
  const [code, setCode] = useState(initial.code);
  const [line, setLine] = useState(initial.line);
  const [reason, setReason] = useState(initial.reason);
  const [from, setFrom] = useState(initial.from);
  const [to, setTo] = useState(initial.to);
  const save = useMutation({
    mutationFn: () =>
      renewalApi.saveRiskCode(companyId, row?.id ?? null, {
        riskCode: code.trim(),
        lineCode: blankToUndefined(line),
        reason: reason.trim(),
        effectiveFrom: from,
        effectiveTo: blankToUndefined(to),
      }),
    onSuccess: async () => {
      onClose();
      await queryClient.invalidateQueries({ queryKey: ['renewal', 'setup'] });
    },
  });
  return (
    <Modal
      open
      title={row === null ? 'New non-renewable risk code' : `Edit ${row.riskCode}`}
      onClose={onClose}
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            Cancel
          </Button>
          <Button
            busy={save.isPending}
            disabled={code.trim() === '' || reason.trim() === '' || from === ''}
            onClick={() => save.mutate()}
          >
            Save for Authorization
          </Button>
        </>
      }
    >
      <ErrorAlert error={save.error} />
      <div className="form-grid">
        <Field label="Risk Code" required>
          {(id) => (
            <input
              id={id}
              className="input"
              value={code}
              onChange={(e) => setCode(e.target.value)}
            />
          )}
        </Field>
        <Field label="Line of Business">
          {(id) => (
            <input
              id={id}
              className="input"
              value={line}
              onChange={(e) => setLine(e.target.value)}
            />
          )}
        </Field>
        <Field label="Effective From" required>
          {(id) => <DateInput id={id} value={from} onChange={(e) => setFrom(e.target.value)} />}
        </Field>
        <Field label="Effective To">
          {(id) => <DateInput id={id} value={to} onChange={(e) => setTo(e.target.value)} />}
        </Field>
      </div>
      <Field label="Reason" required>
        {(id) => (
          <input
            id={id}
            className="input"
            maxLength={200}
            value={reason}
            onChange={(e) => setReason(e.target.value)}
          />
        )}
      </Field>
    </Modal>
  );
}

/** Non-renewable risk codes (BRRN.009): maker-checker, end-dated, never deleted. */
export function RiskCodesTab() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState<RiskCodeView | null>();
  const rows = useQuery({
    queryKey: ['renewal', 'setup', 'risk-codes', companyId],
    queryFn: () => renewalApi.riskCodes(companyId),
    enabled: companyId > 0,
  });
  const act = useMutation({
    mutationFn: ({ id, action }: { id: number; action: 'AUTHORIZE' | 'DEACTIVATE' }) =>
      renewalApi.riskCodeAction(companyId, id, action),
    onSuccess: async (r) => {
      toast.success(
        `${r.riskCode} ${r.approval.recordStatus === 'ACTIVE' ? 'authorized' : 'deactivated'}`,
      );
      await queryClient.invalidateQueries({ queryKey: ['renewal', 'setup'] });
    },
  });
  const maintain = can('RNW_SETUP');
  return (
    <Card
      title="Non-renewable risk codes"
      flush
      actions={
        maintain && (
          <Button icon={<Plus size={16} />} onClick={() => setEditing(null)}>
            New Risk Code
          </Button>
        )
      }
    >
      <ErrorAlert error={rows.error ?? act.error} />
      <DataTable<RiskCodeView>
        loading={rows.isLoading}
        rows={rows.data ?? []}
        rowKey={(r) => r.id}
        emptyMessage="No non-renewable risk codes"
        columns={[
          { key: 'code', header: 'Risk Code', kind: 'code', render: (r) => r.riskCode },
          { key: 'line', header: 'Line', render: (r) => r.lineCode ?? 'All' },
          { key: 'reason', header: 'Reason', render: (r) => r.reason },
          { key: 'from', header: 'From', kind: 'date', render: (r) => formatDate(r.effectiveFrom) },
          { key: 'to', header: 'To', kind: 'date', render: (r) => formatDate(r.effectiveTo) },
          {
            key: 'status',
            header: 'Status',
            render: (r) => <ApprovalCell approval={r.approval} />,
          },
          {
            key: 'act',
            header: '',
            render: (r) =>
              maintain && (
                <span className="rnw-actions">
                  {pending(r.approval) && (
                    <ConfirmButton
                      size="sm"
                      confirm={{
                        title: 'Authorize Risk Code',
                        effect: 'The risk code change takes effect.',
                      }}
                      onConfirm={() => act.mutateAsync({ id: r.id, action: 'AUTHORIZE' })}
                    >
                      Authorize
                    </ConfirmButton>
                  )}
                  <Button size="sm" variant="ghost" onClick={() => setEditing(r)}>
                    Edit
                  </Button>
                  {r.approval.recordStatus === 'ACTIVE' && (
                    <ConfirmButton
                      size="sm"
                      variant="ghost"
                      confirm={{
                        title: 'Deactivate Risk Code',
                        effect: 'The risk code is no longer used for new renewals.',
                        destructive: true,
                      }}
                      onConfirm={() => act.mutateAsync({ id: r.id, action: 'DEACTIVATE' })}
                    >
                      Deactivate
                    </ConfirmButton>
                  )}
                </span>
              ),
          },
        ]}
      />
      {editing !== undefined && (
        <RiskCodeDialog row={editing} onClose={() => setEditing(undefined)} />
      )}
    </Card>
  );
}
