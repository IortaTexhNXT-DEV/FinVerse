import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Plus, ShieldCheck, Snowflake } from 'lucide-react';
import { useMemo, useState } from 'react';
import { glApi } from '@/api/gl';
import type { GlAccount, GlAccountRequest } from '@/api/gl';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { AccountForm } from './AccountForm';
import { blankAccount, toRequest } from './accountModel';
import { useGlLookups } from './useLookups';
import { awaitsOtherChecker } from '@/utils/makerChecker';

const INDENT: Record<string, number> = { GROUP: 0, MAIN: 1, SUB: 2, MICRO: 3 };

/** Chart of accounts: multi-tier GL heads with maker-checker maintenance and freeze. */
export default function ChartOfAccountsPage() {
  const { companyId, accounts } = useGlLookups();
  const { can, user } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [search, setSearch] = useState('');
  const [editing, setEditing] = useState<GlAccount | null>(null);
  const [form, setForm] = useState<GlAccountRequest | null>(null);

  const rows = useMemo(() => {
    const term = search.trim().toLowerCase();
    return term === ''
      ? accounts
      : accounts.filter(
          (a) => a.code.toLowerCase().startsWith(term) || a.name.toLowerCase().includes(term),
        );
  }, [accounts, search]);

  const refresh = () => queryClient.invalidateQueries({ queryKey: ['accounts'] });
  const save = useMutation({
    mutationFn: (body: GlAccountRequest) =>
      editing ? glApi.updateAccount(editing.id, body) : glApi.createAccount(body),
    onSuccess: async (a) => {
      await refresh();
      setForm(null);
      toast.success(`Account ${a.code} saved – pending authorization`);
    },
  });
  const act = useMutation({
    mutationFn: (run: () => Promise<GlAccount>) => run(),
    onSuccess: async (a) => {
      await refresh();
      toast.success(`Account ${a.code} updated`);
    },
  });

  const open = (a: GlAccount | null) => {
    setEditing(a);
    setForm(a ? toRequest(a) : blankAccount(companyId));
  };

  return (
    <div className="stack">
      <PageHeader
        section="General Ledger"
        title="Chart of Accounts"
        description="Main, Sub and Micro GL heads. Changes require authorization by a second user; accounts are never deleted."
        actions={
          can('MASTER_MAINTAIN') && (
            <Button variant="accent" icon={<Plus size={16} />} onClick={() => open(null)}>
              New Account
            </Button>
          )
        }
      />
      <ErrorAlert error={act.error} />
      <Card
        flush
        title={`${rows.length} accounts`}
        actions={
          <input
            className="input"
            style={{ width: 260 }}
            aria-label="Search accounts"
            placeholder="Search code or name"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        }
      >
        <DataTable<GlAccount>
          rows={rows}
          rowKey={(a) => a.id}
          onRowClick={can('MASTER_MAINTAIN') ? open : undefined}
          columns={[
            {
              key: 'code',
              header: 'Account',
              render: (a) => (
                <span
                  style={{
                    paddingLeft: (INDENT[a.level] ?? 0) * 18,
                    fontWeight: a.postable ? 400 : 700,
                  }}
                >
                  {a.code} – {a.name}
                </span>
              ),
            },
            { key: 'class', header: 'Class', render: (a) => a.accountClass },
            { key: 'tier', header: 'Tier', render: (a) => a.level },
            { key: 'post', header: 'Postable', render: (a) => (a.postable ? 'Yes' : 'Heading') },
            {
              key: 'sl',
              header: 'Sub-ledger',
              render: (a) => (a.controlAccount ? a.subLedgerType : ''),
            },
            { key: 'grp', header: 'Statement Line', render: (a) => a.reportGroup ?? '' },
            {
              key: 'st',
              header: 'Status',
              render: (a) => <StatusBadge status={a.frozen ? 'FROZEN' : a.recordStatus} />,
            },
            {
              key: 'act',
              header: 'Actions',
              render: (a) => (
                <div
                  className="row"
                  role="presentation"
                  onClick={(e) => e.stopPropagation()}
                  onKeyDown={(e) => e.stopPropagation()}
                >
                  {awaitsOtherChecker(a, user?.username) && can('MASTER_AUTHORIZE') && (
                    <Button
                      size="sm"
                      variant="secondary"
                      icon={<ShieldCheck size={14} />}
                      onClick={() => act.mutate(() => glApi.authorizeAccount(a.id))}
                    >
                      Authorize
                    </Button>
                  )}
                  {a.recordStatus === 'ACTIVE' && can('MASTER_AUTHORIZE') && (
                    <Button
                      size="sm"
                      variant="ghost"
                      icon={<Snowflake size={14} />}
                      onClick={() =>
                        act.mutate(() =>
                          a.frozen
                            ? glApi.unfreezeAccount(a.id)
                            : glApi.freezeAccount(a.id, 'Frozen by finance'),
                        )
                      }
                    >
                      {a.frozen ? 'Unfreeze' : 'Freeze'}
                    </Button>
                  )}
                </div>
              ),
            },
          ]}
        />
      </Card>
      <Modal
        title={editing ? `Edit account ${editing.code}` : 'New GL account'}
        open={form !== null}
        onClose={() => setForm(null)}
        footer={
          <Button variant="accent" busy={save.isPending} onClick={() => form && save.mutate(form)}>
            Save for Authorization
          </Button>
        }
      >
        <ErrorAlert error={save.error} />
        {form !== null && (
          <AccountForm value={form} editing={editing !== null} onChange={setForm} />
        )}
      </Modal>
    </div>
  );
}
