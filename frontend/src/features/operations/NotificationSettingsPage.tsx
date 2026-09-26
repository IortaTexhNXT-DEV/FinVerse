import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { opsApi } from '@/api/operations';
import type { NotificationChannels } from '@/api/operations';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { useToast } from '@/components/ui/toastContext';
import { humanize } from '@/utils/format';
import './operations.css';

/**
 * Notification settings (RMTID.034): for each Operations event, whether you receive the in-app
 * notification and the e-mail. Events you never changed follow their default.
 */
export default function NotificationSettingsPage() {
  const toast = useToast();
  const queryClient = useQueryClient();
  const prefs = useQuery({ queryKey: ['ops', 'preferences'], queryFn: opsApi.preferences });
  const save = useMutation({
    mutationFn: (p: { code: string; inApp: boolean; email: boolean }) =>
      opsApi.updatePreference(p.code, p.inApp, p.email),
    onSuccess: async (p) => {
      await queryClient.invalidateQueries({ queryKey: ['ops', 'preferences'] });
      toast.success(`${p.name} saved`);
    },
  });
  const toggle = (e: NotificationChannels, channel: 'inApp' | 'email', value: boolean) =>
    save.mutate({ code: e.code, inApp: e.inApp, email: e.email, [channel]: value });
  const columns: Column<NotificationChannels>[] = [
    {
      key: 'event',
      header: 'Event',
      render: (e) => (
        <>
          <strong>{e.name}</strong>
          <div className="ops-muted">{e.description}</div>
        </>
      ),
    },
    { key: 'module', header: 'Module', render: (e) => humanize(e.module) },
    {
      key: 'inApp',
      header: 'In-App',
      render: (e) => (
        <label className="ops-pref-row">
          <input
            type="checkbox"
            checked={e.inApp}
            disabled={save.isPending}
            onChange={(ev) => toggle(e, 'inApp', ev.target.checked)}
          />
          <span className="visually-hidden">In-app notification of {e.name}</span>
        </label>
      ),
    },
    {
      key: 'email',
      header: 'E-mail',
      render: (e) => (
        <label className="ops-pref-row">
          <input
            type="checkbox"
            checked={e.email}
            disabled={save.isPending}
            onChange={(ev) => toggle(e, 'email', ev.target.checked)}
          />
          <span className="visually-hidden">E-mail of {e.name}</span>
        </label>
      ),
    },
    { key: 'custom', header: 'Setting', render: (e) => (e.custom ? 'Yours' : 'Default') },
  ];
  return (
    <div className="stack">
      <PageHeader
        section="Operations"
        title="Notification Settings"
        description="Choose how you hear about Operations events: in the notification bell, by e-mail, or both."
      />
      <ErrorAlert error={prefs.error ?? save.error} />
      <Card flush>
        <DataTable
          caption="Notification settings"
          columns={columns}
          rows={prefs.data ?? []}
          rowKey={(e) => e.code}
          loading={prefs.isLoading}
        />
      </Card>
    </div>
  );
}
