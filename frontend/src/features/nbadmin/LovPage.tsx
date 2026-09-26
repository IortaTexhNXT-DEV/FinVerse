import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { lovApi } from '@/api/lov';
import type { LovType, LovValue } from '@/api/lov';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { useToast } from '@/components/ui/toastContext';
import { awaitsOtherChecker } from '@/utils/makerChecker';
import { LovValueDialog } from './LovValueDialog';
import { nextSortOrder } from './lovForm';
import { LovValuesTable } from './LovValuesTable';

type Editing = { value?: LovValue } | null;

function TypeList({
  types,
  current,
  onSelect,
}: Readonly<{ types: LovType[]; current: string; onSelect: (code: string) => void }>) {
  return (
    <Card title="Lists" flush>
      <ul className="nav-list">
        {types.map((t) => (
          <li key={t.code}>
            <button
              type="button"
              className={t.code === current ? 'nav-list-item active' : 'nav-list-item'}
              onClick={() => onSelect(t.code)}
            >
              {t.name}
              <span className="muted mono">{t.code}</span>
              {!t.maintainable && <span className="badge neutral nav-list-count">System list</span>}
            </button>
          </li>
        ))}
      </ul>
    </Card>
  );
}

function ValuesPanel({ type }: Readonly<{ type: LovType }>) {
  const { can, user } = useAuth();
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
      await queryClient.invalidateQueries({ queryKey: ['approvals'] });
    },
  });
  const rows = values.data ?? [];
  const manage = can('LOV_MANAGE') && type.maintainable;
  const pending = rows.filter((v) => v.status === 'PENDING_AUTHORIZATION').length;
  return (
    <Card
      title={type.name}
      flush
      actions={
        <div className="row">
          {pending > 0 && <span className="badge warning">{pending} pending authorization</span>}
          {manage && (
            <Button
              size="sm"
              variant="accent"
              icon={<Plus size={14} />}
              onClick={() => setEditing({})}
            >
              New Value
            </Button>
          )}
        </div>
      }
    >
      <ErrorAlert error={values.error ?? act.error} />
      {type.description && (
        <p className="muted" style={{ margin: '12px 16px 0' }}>
          {type.description}
        </p>
      )}
      <LovValuesTable
        values={rows}
        loading={values.isLoading}
        manage={manage}
        mayAuthorize={(v) =>
          can('MASTER_AUTHORIZE') &&
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
    </Card>
  );
}

/**
 * Lists of Values (BRNB.083): the coded values of every broking list with their effectivity and
 * maker-checker status. Business administrators add, change and deactivate values; a checker
 * authorizes them before they can be used.
 */
export default function LovPage() {
  const [params, setParams] = useSearchParams();
  const types = useQuery({ queryKey: ['lov', 'types'], queryFn: lovApi.types });
  const all = types.data ?? [];
  const current = params.get('type') ?? all[0]?.code ?? '';
  const type = all.find((t) => t.code === current);
  return (
    <div className="stack">
      <PageHeader
        section="Broking Setup"
        title="Lists of Values"
        description="Coded values used on broking screens (segments, reasons, document types, tags...). Changes take effect from their date once authorized by a checker."
      />
      <ErrorAlert error={types.error} />
      <div className="split">
        <TypeList
          types={all}
          current={current}
          onSelect={(code) => setParams({ type: code }, { replace: true })}
        />
        {type === undefined ? (
          <Card title="Values">
            <p className="muted">{types.isLoading ? 'Loading lists…' : 'Choose a list.'}</p>
          </Card>
        ) : (
          <ValuesPanel key={type.code} type={type} />
        )}
      </div>
    </div>
  );
}
