import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { modulesApi } from '@/api/modules';
import type { ModuleProfile, ModuleSwitch } from '@/api/modules';
import { displayNameOf } from '@/api/users';
import { useAuth } from '@/auth/authContext';
import { Card } from '@/components/ui/Card';
import { CellStack } from '@/components/ui/CellStack';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Notice } from '@/components/ui/Notice';
import { PageHeader } from '@/components/ui/PageHeader';
import { RowActionMenu } from '@/components/ui/RowActionMenu';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { UserName } from '@/components/ui/UserName';
import { useToast } from '@/components/ui/toastContext';
import { formatDateTime } from '@/utils/format';
import { isPending, moduleActions, moduleStatus } from './moduleRows';
import type { Pending } from './moduleRows';

function PendingDialog({ pending, onClose }: Readonly<{ pending: Pending; onClose: () => void }>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const act = useMutation({
    mutationFn: async (reason: string): Promise<string> => {
      switch (pending.kind) {
        case 'switch':
          await modulesApi.request(pending.module.code, !pending.module.enabled, reason);
          return `${pending.module.name}: change sent for approval`;
        case 'approve':
          await modulesApi.approve(pending.module.code);
          return `${pending.module.name}: change approved`;
        case 'reject':
          await modulesApi.reject(pending.module.code, reason);
          return `${pending.module.name}: change rejected`;
        case 'withdraw':
          await modulesApi.reject(pending.module.code);
          return `${pending.module.name}: request withdrawn`;
        default: {
          const changed = await modulesApi.applyProfile(pending.profile.code, reason);
          return `${pending.profile.name}: ${changed.length} module change(s) sent for approval`;
        }
      }
    },
    onSuccess: async (message) => {
      onClose();
      await queryClient.invalidateQueries({ queryKey: ['product-modules'] });
      toast.success(message);
    },
  });
  const common = { busy: act.isPending, error: act.error, onClose, onConfirm: act.mutate };
  if (pending.kind === 'profile') {
    const p = pending.profile;
    return (
      <ConfirmDialog
        {...common}
        title="Apply Module Profile"
        record={p.name}
        effect={
          <>
            <p>{p.description}.</p>
            <p>
              A change is requested for every module switched differently from the profile; each
              change applies once another user approves it.
            </p>
          </>
        }
        confirmLabel="Apply Profile"
        reason="required"
      />
    );
  }
  const m = pending.module;
  switch (pending.kind) {
    case 'switch':
      return (
        <ConfirmDialog
          {...common}
          title={m.enabled ? 'Switch Module Off' : 'Switch Module On'}
          record={m.name}
          effect={
            m.enabled
              ? 'Once approved, the menus, screens, reports, dashboard widgets and scheduled jobs of the module are no longer available, and its permissions grant nothing. Its records are kept.'
              : 'Once approved, the menus, screens, reports, dashboard widgets and scheduled jobs of the module are available again to the users whose roles hold its permissions.'
          }
          confirmLabel={m.enabled ? 'Request Switch Off' : 'Request Switch On'}
          destructive={m.enabled}
          reason="required"
        />
      );
    case 'approve':
      return (
        <ConfirmDialog
          {...common}
          title="Approve Module Change"
          record={`${m.name}: switch ${m.pendingEnabled === true ? 'on' : 'off'}`}
          effect={`Requested by ${displayNameOf(m.pendingBy)} on ${formatDateTime(m.pendingAt)}. Reason: ${m.pendingReason ?? ''}`}
          confirmLabel="Approve"
          destructive={m.pendingEnabled !== true}
        />
      );
    case 'reject':
      return (
        <ConfirmDialog
          {...common}
          title="Reject Module Change"
          record={m.name}
          effect="The module stays as it is; the requester sees the reason."
          confirmLabel="Reject"
          destructive
          reason="required"
        />
      );
    default:
      return (
        <ConfirmDialog
          {...common}
          title="Withdraw Module Change"
          record={m.name}
          effect="The request is withdrawn; the module stays as it is."
          confirmLabel="Withdraw Request"
          destructive
        />
      );
  }
}

/**
 * Product Modules (Administration): the modules of the product this deployment uses. The system
 * administrator switches a module on or off, or applies a module profile, with a reason; another
 * user approves the change. Menus, screens, permissions, reports, dashboard widgets and scheduled
 * jobs of a switched-off module are not available.
 */
export default function ProductModulesPage() {
  const { can, user } = useAuth();
  const [pending, setPending] = useState<Pending | null>(null);
  const modules = useQuery({ queryKey: ['product-modules'], queryFn: modulesApi.list });
  const profiles = useQuery({
    queryKey: ['product-modules', 'profiles'],
    queryFn: modulesApi.profiles,
  });
  const who = {
    username: user?.username ?? '',
    mayManage: can('MODULE_SWITCH_MANAGE'),
    mayApprove: can('MODULE_SWITCH_APPROVE'),
  };
  const waiting = (modules.data ?? []).filter(isPending).length;

  return (
    <div className="stack">
      <PageHeader
        section="Administration"
        title="Product Modules"
        description="The modules of the product in use in this deployment. A change applies once another user approves it."
      />
      {waiting > 0 && (
        <Notice tone="info" title="Changes waiting for approval">
          {`${waiting} module change(s) wait for approval by a user other than the requester.`}
        </Notice>
      )}
      <Card title="Modules" flush>
        <ErrorAlert error={modules.error} />
        <DataTable<ModuleSwitch>
          callout="product-modules"
          loading={modules.isLoading}
          rows={modules.data ?? []}
          rowKey={(m) => m.code}
          columns={[
            { key: 'name', header: 'Module', render: (m) => <strong>{m.name}</strong> },
            {
              key: 'status',
              header: 'Status',
              kind: 'status',
              width: '160px',
              render: (m) => {
                const s = moduleStatus(m);
                return <StatusBadge status={s.status} label={s.label} />;
              },
            },
            {
              key: 'needs',
              header: 'Needs',
              render: (m) => (m.needs.length === 0 ? '—' : m.needs.join(', ')),
            },
            {
              key: 'request',
              header: 'Requested By',
              render: (m) =>
                isPending(m) ? (
                  <CellStack
                    main={<UserName login={m.pendingBy} />}
                    sub={formatDateTime(m.pendingAt)}
                  />
                ) : (
                  '—'
                ),
            },
            {
              key: 'reason',
              header: 'Reason',
              render: (m) => (isPending(m) ? m.pendingReason : (m.changedReason ?? '—')),
            },
            {
              key: 'changed',
              header: 'Last Changed',
              render: (m) =>
                m.changedAt === undefined || m.changedAt === null ? (
                  'As delivered'
                ) : (
                  <CellStack main={formatDateTime(m.changedAt)} sub={displayNameOf(m.changedBy)} />
                ),
            },
            {
              key: 'approved',
              header: 'Approved By',
              render: (m) =>
                m.approvedBy === undefined || m.approvedBy === null ? (
                  '—'
                ) : (
                  <UserName login={m.approvedBy} />
                ),
            },
            {
              key: 'actions',
              header: <span className="visually-hidden">Actions</span>,
              width: '64px',
              render: (m) => (
                <RowActionMenu label={m.name} actions={moduleActions(m, who, setPending)} />
              ),
            },
          ]}
        />
      </Card>
      <Card title="Module Profiles" flush>
        <ErrorAlert error={profiles.error} />
        <DataTable<ModuleProfile>
          callout="module-profiles"
          loading={profiles.isLoading}
          rows={profiles.data ?? []}
          rowKey={(p) => p.code}
          columns={[
            { key: 'name', header: 'Profile', render: (p) => <strong>{p.name}</strong> },
            { key: 'description', header: 'Description', render: (p) => p.description },
            {
              key: 'off',
              header: 'Modules Off',
              render: (p) => (p.modulesOff.length === 0 ? 'None' : p.modulesOff.join(', ')),
            },
            {
              key: 'actions',
              header: <span className="visually-hidden">Actions</span>,
              width: '64px',
              render: (p) => (
                <RowActionMenu
                  label={p.name}
                  actions={
                    who.mayManage
                      ? [
                          {
                            label: 'Apply Profile',
                            onSelect: () => setPending({ kind: 'profile', profile: p }),
                          },
                        ]
                      : []
                  }
                />
              ),
            },
          ]}
        />
      </Card>
      {pending !== null && <PendingDialog pending={pending} onClose={() => setPending(null)} />}
    </div>
  );
}
