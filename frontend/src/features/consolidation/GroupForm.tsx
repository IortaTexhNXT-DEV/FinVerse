import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Plus, Trash2 } from 'lucide-react';
import { useState } from 'react';
import { consolidationApi } from '@/api/consolidation';
import type { GroupMember } from '@/api/consolidation';
import type { Company } from '@/api/types';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { useToast } from '@/components/ui/toastContext';

interface Props {
  open: boolean;
  onClose: () => void;
  companies: Company[];
  parentCompanyId: number;
}

interface MemberRow {
  companyId: number;
  ownershipPct: string;
  investmentAccount: string;
  equityAccounts: string;
}

const GROUP_ACCOUNTS = [
  ['ctaAccount', 'Translation reserve (CTA)'],
  ['nciAccount', 'Non-controlling interest'],
  ['goodwillAccount', 'Goodwill'],
] as const;

function toMember(m: MemberRow): GroupMember {
  return {
    companyId: m.companyId,
    ownershipPct: Number(m.ownershipPct),
    investmentAccount: m.investmentAccount.trim() || undefined,
    equityAccounts: m.equityAccounts
      .split(',')
      .map((s) => s.trim())
      .filter(Boolean),
  };
}

/** Creates a consolidation group: parent (current company), subsidiaries and group accounts. */
export function GroupForm({ open, onClose, companies, parentCompanyId }: Readonly<Props>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const subsidiaries = companies.filter((c) => c.id !== parentCompanyId);
  const parent = companies.find((c) => c.id === parentCompanyId);
  const [form, setForm] = useState({
    code: '',
    name: '',
    ctaAccount: '3450',
    nciAccount: '3600',
    goodwillAccount: '1850',
  });
  const [members, setMembers] = useState<MemberRow[]>([]);
  const create = useMutation({
    mutationFn: () =>
      consolidationApi.createGroup({
        ...form,
        parentCompanyId,
        currency: parent?.baseCurrency ?? 'PHP',
        active: true,
        members: members.map(toMember),
      }),
    onSuccess: async (g) => {
      await queryClient.invalidateQueries({ queryKey: ['con-groups'] });
      toast.success(`Group ${g.code} created`);
      onClose();
    },
  });
  const updateMember = (i: number, patch: Partial<MemberRow>) =>
    setMembers(members.map((m, j) => (j === i ? { ...m, ...patch } : m)));

  return (
    <Modal
      title="New consolidation group"
      open={open}
      onClose={onClose}
      footer={
        <Button
          variant="accent"
          busy={create.isPending}
          disabled={form.code === '' || form.name === ''}
          onClick={() => create.mutate()}
        >
          Create group
        </Button>
      }
    >
      <ErrorAlert error={create.error} />
      <p className="muted">
        Parent: {parent?.code} · consolidation currency {parent?.baseCurrency}. Members use the
        parent&apos;s account codes.
      </p>
      <div className="form-grid">
        <Field label="Code" required>
          {(id) => (
            <input
              id={id}
              className="input"
              value={form.code}
              onChange={(e) => setForm({ ...form, code: e.target.value.toUpperCase() })}
            />
          )}
        </Field>
        <Field label="Name" required>
          {(id) => (
            <input
              id={id}
              className="input"
              value={form.name}
              onChange={(e) => setForm({ ...form, name: e.target.value })}
            />
          )}
        </Field>
        {GROUP_ACCOUNTS.map(([key, label]) => (
          <Field key={key} label={label} required>
            {(id) => (
              <input
                id={id}
                className="input"
                value={form[key]}
                onChange={(e) => setForm({ ...form, [key]: e.target.value })}
              />
            )}
          </Field>
        ))}
      </div>
      {members.map((m, i) => (
        <div className="form-grid" key={`${m.companyId}-${i}`}>
          <Field label="Subsidiary" required>
            {(id) => (
              <select
                id={id}
                className="select"
                value={m.companyId}
                onChange={(e) => updateMember(i, { companyId: Number(e.target.value) })}
              >
                {subsidiaries.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.code}
                  </option>
                ))}
              </select>
            )}
          </Field>
          <Field label="Ownership %" required>
            {(id) => (
              <input
                id={id}
                className="input num"
                type="number"
                value={m.ownershipPct}
                onChange={(e) => updateMember(i, { ownershipPct: e.target.value })}
              />
            )}
          </Field>
          <Field label="Investment account (parent)">
            {(id) => (
              <input
                id={id}
                className="input"
                value={m.investmentAccount}
                onChange={(e) => updateMember(i, { investmentAccount: e.target.value })}
              />
            )}
          </Field>
          <Field label="Capital accounts (subsidiary)" hint="Comma separated">
            {(id) => (
              <input
                id={id}
                className="input"
                value={m.equityAccounts}
                onChange={(e) => updateMember(i, { equityAccounts: e.target.value })}
              />
            )}
          </Field>
          <Button
            variant="ghost"
            aria-label="Remove subsidiary"
            icon={<Trash2 size={14} />}
            onClick={() => setMembers(members.filter((_, j) => j !== i))}
            style={{ alignSelf: 'end' }}
          />
        </div>
      ))}
      <Button
        variant="secondary"
        size="sm"
        icon={<Plus size={14} />}
        disabled={subsidiaries.length === 0}
        onClick={() =>
          setMembers([
            ...members,
            {
              companyId: subsidiaries[0]?.id ?? 0,
              ownershipPct: '100',
              investmentAccount: '1506',
              equityAccounts: '3100',
            },
          ])
        }
      >
        Add subsidiary
      </Button>
    </Modal>
  );
}
