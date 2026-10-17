import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { claimsSetupApi, STATUS_LIST } from './api';
import type { ValueAttributes } from './api';
import { SettlementAttributesDialog, StatusAttributesDialog } from './AttributeDialogs';
import { describeSettlement, describeStatus } from './setupLogic';
import { displayNameOf } from '@/api/users';
import { ConfirmButton } from '@/components/ui/ConfirmButton';

/**
 * Status or settlement type attributes on Claims Setup (FR-CM-040/043): each value with its
 * current attributes and a pending proposal; the Unit Head proposes, another user authorizes.
 */
export function AttributesTab({ list }: Readonly<{ list: string }>) {
  const { user } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState<ValueAttributes>();
  const isStatus = list === STATUS_LIST;
  const describe = isStatus ? describeStatus : describeSettlement;
  const values = useQuery({
    queryKey: ['broker-claims', 'setup', 'attributes', list],
    queryFn: () => claimsSetupApi.attributes(list),
  });
  const done = async (message: string) => {
    setEditing(undefined);
    await queryClient.invalidateQueries({ queryKey: ['broker-claims', 'setup'] });
    toast.success(message);
  };
  const propose = useMutation({
    mutationFn: (run: () => Promise<unknown>) => run(),
    onSuccess: () => done('Attributes submitted for authorization'),
  });
  const decide = useMutation({
    mutationFn: ({ code, approve, reason }: { code: string; approve: boolean; reason?: string }) =>
      approve
        ? claimsSetupApi.authorizeAttributes(list, code)
        : claimsSetupApi.rejectAttributes(list, code, reason),
    onSuccess: (_, v) =>
      done(v.approve ? 'Attributes authorized' : 'Proposal withdrawn or rejected'),
  });
  const mine = (v: ValueAttributes) => v.pendingBy?.toLowerCase() === user?.username.toLowerCase();
  return (
    <>
      <ErrorAlert error={values.error ?? decide.error} />
      <DataTable<ValueAttributes>
        caption={isStatus ? 'Status attributes' : 'Settlement type attributes'}
        loading={values.isLoading}
        rows={values.data ?? []}
        rowKey={(v) => v.code}
        emptyMessage="No values"
        columns={[
          {
            key: 'value',
            header: isStatus ? 'Status' : 'Settlement Type',
            render: (v) => (
              <>
                <strong>{v.label}</strong>
                <div className="muted">{v.code}</div>
              </>
            ),
          },
          { key: 'rec', header: 'Value', render: (v) => <StatusBadge status={v.valueStatus} /> },
          { key: 'attr', header: 'Attributes', render: (v) => describe(v.attributes) || '—' },
          {
            key: 'pending',
            header: 'Pending Change',
            render: (v) =>
              v.pendingBy === undefined
                ? ''
                : `${describe(v.pending)} (by ${displayNameOf(v.pendingBy)})`,
          },
          {
            key: 'act',
            header: '',
            render: (v) => (
              <span className="tag-list">
                <Button size="sm" variant="ghost" onClick={() => setEditing(v)}>
                  Edit
                </Button>
                {v.pendingBy !== undefined && !mine(v) && (
                  <ConfirmButton
                    size="sm"
                    variant="ghost"
                    confirm={{
                      title: 'Authorize Attributes',
                      record: v.label,
                      effect: 'The attribute change takes effect.',
                    }}
                    onConfirm={() => decide.mutateAsync({ code: v.code, approve: true })}
                  >
                    Authorize
                  </ConfirmButton>
                )}
                {v.pendingBy !== undefined && (
                  <ConfirmButton
                    size="sm"
                    variant="ghost"
                    confirm={
                      mine(v)
                        ? {
                            title: 'Withdraw Attribute Change',
                            record: v.label,
                            effect:
                              'Your pending change is withdrawn; the current attributes stay.',
                          }
                        : {
                            title: 'Reject Attribute Change',
                            record: v.label,
                            effect: 'The pending change is rejected; the current attributes stay.',
                            reason: 'required',
                            destructive: true,
                          }
                    }
                    onConfirm={(reason) =>
                      decide.mutateAsync({ code: v.code, approve: false, reason })
                    }
                  >
                    {mine(v) ? 'Withdraw' : 'Reject'}
                  </ConfirmButton>
                )}
              </span>
            ),
          },
        ]}
      />
      {editing !== undefined && isStatus && (
        <StatusAttributesDialog
          value={editing}
          busy={propose.isPending}
          error={propose.error}
          onClose={() => setEditing(undefined)}
          onSave={(input) =>
            propose.mutate(() => claimsSetupApi.proposeStatus(editing.code, input))
          }
        />
      )}
      {editing !== undefined && !isStatus && (
        <SettlementAttributesDialog
          value={editing}
          busy={propose.isPending}
          error={propose.error}
          onClose={() => setEditing(undefined)}
          onSave={(input) =>
            propose.mutate(() => claimsSetupApi.proposeSettlement(editing.code, input))
          }
        />
      )}
    </>
  );
}
