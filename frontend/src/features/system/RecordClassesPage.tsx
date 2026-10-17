import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { supportAdminApi } from '@/api/supportAdmin';
import type { RecordClass, RecordClassSettings } from '@/api/supportAdmin';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { RowActionMenu } from '@/components/ui/RowActionMenu';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { humanize } from '@/utils/format';
import { durationText } from './supportText';

const PERIOD = /^P\d+[YMD]$/;

function EditDialog({ rc, onClose }: Readonly<{ rc: RecordClass; onClose: () => void }>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [form, setForm] = useState<RecordClassSettings>({
    retentionRecordType: rc.retentionRecordType ?? '',
    retentionPeriod: rc.retentionPeriod,
    legalHold: rc.legalHold,
    archiveToEcm: rc.archiveToEcm,
    active: rc.active,
  });
  const [tried, setTried] = useState(false);
  const periodError = PERIOD.test(form.retentionPeriod)
    ? undefined
    : 'Enter a period such as P10Y (10 years), P6M (6 months) or P90D (90 days)';
  const save = useMutation({
    mutationFn: () => {
      const recordType = form.retentionRecordType?.trim();
      return supportAdminApi.changeRecordClass(rc.code, {
        ...form,
        retentionRecordType: recordType === undefined || recordType === '' ? null : recordType,
      });
    },
    onSuccess: async () => {
      onClose();
      await queryClient.invalidateQueries({ queryKey: ['record-classes'] });
      toast.success(`${rc.name}: settings changed`);
    },
  });
  const check = (key: 'legalHold' | 'archiveToEcm' | 'active', label: string) => (
    <label className="checkbox">
      <input
        type="checkbox"
        checked={form[key]}
        onChange={(e) => setForm({ ...form, [key]: e.target.checked })}
      />{' '}
      {label}
    </label>
  );
  return (
    <Modal
      open
      title={`Change Record Class ${rc.name}`}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={save.isPending}>
            Cancel
          </Button>
          <Button
            variant="accent"
            busy={save.isPending}
            onClick={() => {
              setTried(true);
              if (periodError === undefined) {
                save.mutate();
              }
            }}
          >
            Save
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={save.error} title="Cannot change the record class" />
        <p className="muted">{rc.description}</p>
        <Field
          label="Retention rule record type"
          hint="Record type of the retention rules; blank uses the fallback retention"
        >
          {(id) => (
            <input
              id={id}
              className="input"
              maxLength={40}
              value={form.retentionRecordType ?? ''}
              onChange={(e) => setForm({ ...form, retentionRecordType: e.target.value })}
            />
          )}
        </Field>
        <Field
          label="Fallback retention"
          required
          hint={PERIOD.test(form.retentionPeriod) ? durationText(form.retentionPeriod) : undefined}
          error={tried ? periodError : undefined}
        >
          {(id) => (
            <input
              id={id}
              className="input"
              value={form.retentionPeriod}
              onChange={(e) =>
                setForm({ ...form, retentionPeriod: e.target.value.trim().toUpperCase() })
              }
            />
          )}
        </Field>
        {check('legalHold', 'New files are under legal hold from the start')}
        {check('archiveToEcm', 'Final files are archived to the document management system')}
        {check('active', 'Accepts new files')}
      </div>
    </Modal>
  );
}

/**
 * Record Classes (Administration): how long the files of each class are kept, whether they start
 * under legal hold and whether final files are archived. The records hold approver changes the
 * settings; every change is audited.
 */
export default function RecordClassesPage() {
  const { can } = useAuth();
  const [editing, setEditing] = useState<RecordClass | null>(null);
  const classes = useQuery({
    queryKey: ['record-classes'],
    queryFn: supportAdminApi.recordClasses,
  });
  const mayChange = can('FILE_LEGAL_HOLD_APPROVE');
  return (
    <div className="stack">
      <PageHeader
        section="Administration"
        title="Record Classes"
        description="Retention, legal hold and archiving of the stored files, by record class."
      />
      <Card flush>
        <ErrorAlert error={classes.error} />
        <DataTable<RecordClass>
          callout="record-classes"
          loading={classes.isLoading}
          rows={classes.data ?? []}
          rowKey={(c) => c.code}
          columns={[
            { key: 'n', header: 'Record Class', render: (c) => <strong>{c.name}</strong> },
            { key: 'd', header: 'Description', render: (c) => c.description ?? '—' },
            { key: 'b', header: 'Storage', render: (c) => humanize(c.bucketClass) },
            {
              key: 'r',
              header: 'Retention',
              render: (c) =>
                c.retentionRecordType
                  ? `Retention rules of ${humanize(c.retentionRecordType)}; otherwise ${durationText(c.retentionPeriod)}`
                  : durationText(c.retentionPeriod),
            },
            {
              key: 'h',
              header: 'Legal Hold From Start',
              render: (c) => (c.legalHold ? 'Yes' : 'No'),
            },
            { key: 'e', header: 'Archived', render: (c) => (c.archiveToEcm ? 'Yes' : 'No') },
            {
              key: 's',
              header: 'Status',
              kind: 'status',
              render: (c) => <StatusBadge status={c.active ? 'ACTIVE' : 'INACTIVE'} />,
            },
            {
              key: 'a',
              header: <span className="visually-hidden">Actions</span>,
              width: '64px',
              render: (c) => (
                <RowActionMenu
                  label={c.name}
                  actions={
                    mayChange ? [{ label: 'Change Settings', onSelect: () => setEditing(c) }] : []
                  }
                />
              ),
            },
          ]}
        />
      </Card>
      {editing !== null && <EditDialog rc={editing} onClose={() => setEditing(null)} />}
    </div>
  );
}
