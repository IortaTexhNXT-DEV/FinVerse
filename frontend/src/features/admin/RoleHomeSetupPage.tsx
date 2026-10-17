import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ArrowDown, ArrowUp } from 'lucide-react';
import { useState } from 'react';
import { roleHomesApi } from '@/api/roleHomes';
import type { HomeWidget, RoleHome } from '@/api/roleHomes';
import { displayNameOf } from '@/api/users';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { CellStack } from '@/components/ui/CellStack';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { Notice } from '@/components/ui/Notice';
import { PageHeader } from '@/components/ui/PageHeader';
import { RowActionMenu } from '@/components/ui/RowActionMenu';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { UserName } from '@/components/ui/UserName';
import { useToast } from '@/components/ui/toastContext';
import { countOf, formatDateTime } from '@/utils/format';
import {
  isPendingHome,
  moveWidget,
  roleHomeActions,
  roleHomeStatus,
  toggleWidget,
  widgetNames,
} from './roleHomeRows';
import type { RoleHomeAction } from './roleHomeRows';

const QUERY_KEY = ['role-homes'];

/** Choose and order the widgets of a role, with the reason of the change. */
function WidgetEditor({
  home,
  catalogue,
  onClose,
}: Readonly<{ home: RoleHome; catalogue: readonly HomeWidget[]; onClose: () => void }>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [chosen, setChosen] = useState<string[]>(home.widgets);
  const [reason, setReason] = useState('');
  const [tried, setTried] = useState(false);
  const save = useMutation({
    mutationFn: () => roleHomesApi.request(home.roleCode, chosen, reason.trim()),
    onSuccess: async () => {
      onClose();
      await queryClient.invalidateQueries({ queryKey: QUERY_KEY });
      toast.success(`${home.roleName}: home page change sent for approval`);
    },
  });
  const noWidget = chosen.length === 0;
  const noReason = reason.trim() === '';
  const others = catalogue.filter((w) => !chosen.includes(w.code));
  const nameOf = (code: string) => catalogue.find((w) => w.code === code)?.name ?? code;
  return (
    <Modal
      open
      title={`Change Home Page Widgets ${home.roleName}`}
      onClose={onClose}
      footer={
        <Button
          variant="accent"
          busy={save.isPending}
          onClick={() => {
            setTried(true);
            if (!noWidget && !noReason) {
              save.mutate();
            }
          }}
        >
          Send for Approval
        </Button>
      }
    >
      <ErrorAlert error={save.error} title="Cannot request the change" />
      <div className="stack">
        <p className="muted">
          The home page of the role shows the ticked widgets in this order. A user with several
          roles sees each widget once. The change applies once another user approves it.
        </p>
        {tried && noWidget && (
          <Notice tone="warning" title="No widget chosen">
            Tick at least one widget.
          </Notice>
        )}
        <ul className="stack" aria-label="Widgets shown">
          {chosen.map((code, i) => (
            <li key={code} className="row">
              <label>
                <input
                  type="checkbox"
                  checked
                  onChange={() => setChosen(toggleWidget(chosen, code))}
                />{' '}
                {nameOf(code)}
              </label>
              <span className="spacer" />
              <Button
                variant="ghost"
                size="sm"
                icon={<ArrowUp size={14} />}
                aria-label={`Move ${nameOf(code)} up`}
                disabled={i === 0}
                onClick={() => setChosen(moveWidget(chosen, i, -1))}
              />
              <Button
                variant="ghost"
                size="sm"
                icon={<ArrowDown size={14} />}
                aria-label={`Move ${nameOf(code)} down`}
                disabled={i === chosen.length - 1}
                onClick={() => setChosen(moveWidget(chosen, i, 1))}
              />
            </li>
          ))}
          {others.map((w) => (
            <li key={w.code}>
              <label>
                <input
                  type="checkbox"
                  checked={false}
                  onChange={() => setChosen(toggleWidget(chosen, w.code))}
                />{' '}
                {w.name}
              </label>
            </li>
          ))}
        </ul>
        <Field
          label="Reason"
          required
          error={tried && noReason ? 'Enter the reason for the change' : undefined}
        >
          {(id) => (
            <textarea
              id={id}
              className="textarea"
              rows={2}
              maxLength={400}
              value={reason}
              onChange={(e) => setReason(e.target.value)}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}

/** Approve, reject or withdraw the requested widgets of a role. */
function DecisionDialog({
  action,
  catalogue,
  onClose,
}: Readonly<{
  action: Exclude<RoleHomeAction, { kind: 'edit' }>;
  catalogue: readonly HomeWidget[];
  onClose: () => void;
}>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const h = action.home;
  const act = useMutation({
    mutationFn: async (reason: string): Promise<string> => {
      switch (action.kind) {
        case 'approve':
          await roleHomesApi.approve(h.roleCode);
          return `${h.roleName}: home page change approved`;
        case 'reject':
          await roleHomesApi.reject(h.roleCode, reason);
          return `${h.roleName}: home page change rejected`;
        default:
          await roleHomesApi.reject(h.roleCode);
          return `${h.roleName}: request withdrawn`;
      }
    },
    onSuccess: async (message) => {
      onClose();
      await queryClient.invalidateQueries({ queryKey: QUERY_KEY });
      toast.success(message);
    },
  });
  const common = { busy: act.isPending, error: act.error, onClose, onConfirm: act.mutate };
  const requested = widgetNames(h.pendingWidgets ?? [], catalogue);
  switch (action.kind) {
    case 'approve':
      return (
        <ConfirmDialog
          {...common}
          title="Approve Home Page Change"
          record={h.roleName}
          effect={`Widgets: ${requested}. Requested by ${displayNameOf(h.pendingBy)} on ${formatDateTime(h.pendingAt)}. Reason: ${h.pendingReason ?? ''}`}
          confirmLabel="Approve"
        />
      );
    case 'reject':
      return (
        <ConfirmDialog
          {...common}
          title="Reject Home Page Change"
          record={h.roleName}
          effect="The role's home page stays as it is; the requester sees the reason."
          confirmLabel="Reject"
          destructive
          reason="required"
        />
      );
    default:
      return (
        <ConfirmDialog
          {...common}
          title="Withdraw Home Page Change"
          record={h.roleName}
          effect="The request is withdrawn; the role's home page stays as it is."
          confirmLabel="Withdraw Request"
          destructive
        />
      );
  }
}

/**
 * Role Home Set-up (Setup & Administration, FR-CR-010): the System Administrator chooses and
 * orders the home-page widgets of each role with a reason; another user approves the change. A
 * role not set up shows the delivered widgets.
 */
export default function RoleHomeSetupPage() {
  const { can, user } = useAuth();
  const [action, setAction] = useState<RoleHomeAction | null>(null);
  const homes = useQuery({ queryKey: QUERY_KEY, queryFn: roleHomesApi.list });
  const widgets = useQuery({ queryKey: [...QUERY_KEY, 'widgets'], queryFn: roleHomesApi.widgets });
  const catalogue = widgets.data ?? [];
  const who = {
    username: user?.username ?? '',
    mayManage: can('ROLE_HOME_MANAGE'),
    mayApprove: can('ROLE_HOME_APPROVE'),
  };
  const waiting = (homes.data ?? []).filter(isPendingHome).length;

  return (
    <div className="stack">
      <PageHeader
        section="Administration"
        title="Role Home Set-up"
        description="The widgets of the home page of each role, in display order. A change applies once another user approves it."
      />
      {waiting > 0 && (
        <Notice tone="info" title="Changes waiting for approval">
          {`${countOf(waiting, 'home page change')} wait for approval by a user other than the requester.`}
        </Notice>
      )}
      <Card title="Roles" flush>
        <ErrorAlert error={homes.error ?? widgets.error} />
        <DataTable<RoleHome>
          callout="role-homes"
          loading={homes.isLoading}
          rows={homes.data ?? []}
          rowKey={(h) => h.roleCode}
          columns={[
            {
              key: 'role',
              header: 'Role',
              render: (h) => <CellStack main={<strong>{h.roleName}</strong>} sub={h.roleCode} />,
            },
            {
              key: 'status',
              header: 'Status',
              kind: 'status',
              width: '150px',
              render: (h) => {
                const s = roleHomeStatus(h);
                return <StatusBadge status={s.status} label={s.label} />;
              },
            },
            {
              key: 'widgets',
              header: 'Widgets',
              render: (h) => widgetNames(h.widgets, catalogue),
            },
            {
              key: 'requested',
              header: 'Requested',
              render: (h) =>
                isPendingHome(h) ? (
                  <CellStack
                    main={widgetNames(h.pendingWidgets ?? [], catalogue)}
                    sub={
                      <>
                        <UserName login={h.pendingBy} /> · {formatDateTime(h.pendingAt)}
                      </>
                    }
                  />
                ) : (
                  '—'
                ),
            },
            {
              key: 'changed',
              header: 'Last Changed',
              render: (h) =>
                h.changedAt === undefined || h.changedAt === null ? (
                  'As delivered'
                ) : (
                  <CellStack
                    main={formatDateTime(h.changedAt)}
                    sub={`Approved by ${displayNameOf(h.approvedBy)}`}
                  />
                ),
            },
            {
              key: 'actions',
              header: <span className="visually-hidden">Actions</span>,
              width: '64px',
              render: (h) => (
                <RowActionMenu label={h.roleName} actions={roleHomeActions(h, who, setAction)} />
              ),
            },
          ]}
        />
      </Card>
      {action !== null && action.kind === 'edit' && (
        <WidgetEditor home={action.home} catalogue={catalogue} onClose={() => setAction(null)} />
      )}
      {action !== null && action.kind !== 'edit' && (
        <DecisionDialog action={action} catalogue={catalogue} onClose={() => setAction(null)} />
      )}
    </div>
  );
}
