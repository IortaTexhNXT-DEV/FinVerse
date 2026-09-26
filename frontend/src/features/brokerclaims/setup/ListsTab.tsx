import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { lovApi } from '@/api/lov';
import type { LovType, LovValue } from '@/api/lov';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { EmptyState } from '@/components/ui/EmptyState';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { useToast } from '@/components/ui/toastContext';
import { LovValueDialog } from '@/features/nbadmin/LovValueDialog';
import { nextSortOrder } from '@/features/nbadmin/lovForm';
import { LovValuesTable } from '@/features/nbadmin/LovValuesTable';
import { awaitsOtherChecker } from '@/utils/makerChecker';
import { claimsSetupApi } from './api';

type Editing = { value?: LovValue } | null;

/** The values of one Claims list with their maker-checker actions. */
function ListValues({ type }: Readonly<{ type: LovType }>) {
  const { user } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState<Editing>(null);
  const values = useQuery({
    queryKey: ['lov', 'values', type.code],
    queryFn: () => lovApi.values(type.code),
  });
  const act = useMutation({
    mutationFn: ({ v, action }: { v: LovValue; action: 'authorize' | 'deactivate' }) =>
      action === 'authorize' ? lovApi.authorize(v.id) : lovApi.deactivate(v.id),
    onSuccess: async (v, { action }) => {
      toast.success(`${v.label} ${action === 'authorize' ? 'authorized' : 'deactivated'}`);
      await queryClient.invalidateQueries({ queryKey: ['lov'] });
    },
  });
  const rows = values.data ?? [];
  return (
    <div className="stack">
      <div className="worklist-toolbar">
        <p className="muted">{type.description}</p>
        {type.maintainable && (
          <div className="worklist-actions">
            <Button variant="accent" icon={<Plus size={16} />} onClick={() => setEditing({})}>
              New Value
            </Button>
          </div>
        )}
      </div>
      <ErrorAlert error={values.error ?? act.error} />
      <LovValuesTable
        values={rows}
        loading={values.isLoading}
        manage={type.maintainable}
        mayAuthorize={(v) =>
          awaitsOtherChecker({ recordStatus: v.status, maker: v.maker }, user?.username)
        }
        onAction={(v, action) => {
          if (action === 'edit') {
            setEditing({ value: v });
          } else {
            act.mutate({ v, action });
          }
        }}
      />
      {editing !== null && (
        <LovValueDialog
          type={type}
          value={editing.value}
          nextOrder={nextSortOrder(rows)}
          onClose={() => setEditing(null)}
        />
      )}
    </div>
  );
}

/**
 * The Claims lists on Claims Setup (BRCLM.010/014/017/036; design 12.1): the Unit Head adds,
 * changes and deactivates the values of the lists whose owner permission is BCL_SETUP (statuses,
 * settlement types, adjusters, catastrophe codes, units and the others) without the global list
 * administration right; another user authorizes each change.
 */
export function ListsTab() {
  const lists = useQuery({
    queryKey: ['broker-claims', 'setup', 'lists'],
    queryFn: claimsSetupApi.lists,
  });
  const [selected, setSelected] = useState('');
  const all = lists.data ?? [];
  const type = all.find((l) => l.code === selected) ?? all[0];
  return (
    <div className="stack">
      <ErrorAlert error={lists.error} />
      <Field label="List">
        {(id) => (
          <select
            id={id}
            className="select"
            value={type?.code ?? ''}
            onChange={(e) => setSelected(e.target.value)}
          >
            {all.map((l) => (
              <option key={l.code} value={l.code}>
                {l.name}
              </option>
            ))}
          </select>
        )}
      </Field>
      {type === undefined ? (
        <EmptyState message="No Claims lists" />
      ) : (
        <ListValues key={type.code} type={type} />
      )}
    </div>
  );
}
