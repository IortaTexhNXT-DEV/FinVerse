import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useAuth } from '@/auth/authContext';
import { LovSelect } from '@/components/broking/LovSelect';
import { Button } from '@/components/ui/Button';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { formatDate, formatDateTime, humanize } from '@/utils/format';
import { screeningSetupApi } from './api';
import type { EntryDetail, EntryRequest, ListSource, WatchlistChange, WatchlistEntry } from './api';
import { RemarksDialog } from './RemarksDialog';
import { aliasesText, entryErrors, parseAliases, requestOf } from './watchlistLogic';

type TextKey = 'primaryName' | 'firstName' | 'lastName' | 'nationality' | 'idNumbers';
type DateKey = 'birthDate' | 'listedOn' | 'delistedOn';

interface FormProps {
  entry?: WatchlistEntry;
  aliases?: EntryRequest['aliases'];
  sources: ListSource[];
  onClose: () => void;
}

/**
 * Add Entry / Change (FR-SS-022): the entry data and the reason; saved as a change waiting for a
 * Compliance Checker, not used by screening until approved.
 */
export function EntryFormDialog({ entry, aliases, sources, onClose }: Readonly<FormProps>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [request, setRequest] = useState<EntryRequest>(() => requestOf(entry, aliases));
  const [aliasText, setAliasText] = useState(aliasesText(aliases));
  const [errors, setErrors] = useState<Record<string, string>>({});
  const save = useMutation({
    mutationFn: (r: EntryRequest) =>
      entry === undefined
        ? screeningSetupApi.addEntry(r)
        : screeningSetupApi.changeEntry(entry.id, r),
    onSuccess: async (change) => {
      await queryClient.invalidateQueries({ queryKey: ['screening-setup', 'watchlist'] });
      toast.success(`${humanize(change.changeType)} of ${change.externalRef} sent for approval`);
      onClose();
    },
  });
  const set = (patch: Partial<EntryRequest>) => setRequest({ ...request, ...patch });
  const text = (key: TextKey, label: string, required = false) => (
    <Field label={label} required={required} error={errors[key]}>
      {(id) => (
        <input
          id={id}
          className="input"
          value={request[key] ?? ''}
          onChange={(e) => set({ [key]: e.target.value })}
        />
      )}
    </Field>
  );
  const date = (key: DateKey, label: string) => (
    <Field label={label}>
      {(id) => (
        <input
          id={id}
          type="date"
          className="input"
          value={request[key] ?? ''}
          onChange={(e) => set({ [key]: e.target.value || undefined })}
        />
      )}
    </Field>
  );
  const submit = () => {
    const full = { ...request, aliases: parseAliases(aliasText) };
    const found = entryErrors(full);
    setErrors(found);
    if (Object.keys(found).length === 0) {
      save.mutate(full);
    }
  };
  return (
    <Modal
      open
      title={entry === undefined ? 'Add Entry' : `Change ${entry.externalRef}`}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button variant="accent" busy={save.isPending} onClick={submit}>
            Submit for Approval
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={save.error} />
        <div className="form-grid">
          {entry === undefined && (
            <Field label="Source" required>
              {(id) => (
                <select
                  id={id}
                  className="select"
                  value={request.sourceCode ?? ''}
                  onChange={(e) => set({ sourceCode: e.target.value })}
                >
                  {sources.map((s) => (
                    <option key={s.code} value={s.code}>
                      {s.name}
                    </option>
                  ))}
                </select>
              )}
            </Field>
          )}
          <Field label="List Type" required error={errors.listType}>
            {(id) => (
              <LovSelect
                id={id}
                type="SCR_LIST_TYPE"
                value={request.listType ?? ''}
                onChange={(code) => set({ listType: code })}
              />
            )}
          </Field>
          <Field label="Entity Type" required>
            {(id) => (
              <select
                id={id}
                className="select"
                value={request.entityType}
                onChange={(e) => set({ entityType: e.target.value as EntryRequest['entityType'] })}
              >
                <option value="INDIVIDUAL">Individual</option>
                <option value="ENTITY">Entity</option>
              </select>
            )}
          </Field>
          {text('primaryName', 'Primary Name', true)}
          {text('firstName', 'First Name')}
          {text('lastName', 'Last Name')}
          {date('birthDate', 'Birth Date')}
          {text('nationality', 'Nationality')}
          {text('idNumbers', 'ID Numbers')}
          {date('listedOn', 'Listed On')}
          {date('delistedOn', 'Delisted On')}
          <Field label="Aliases" hint="Separate aliases with semicolons">
            {(id) => (
              <input
                id={id}
                className="input"
                value={aliasText}
                onChange={(e) => setAliasText(e.target.value)}
              />
            )}
          </Field>
        </div>
        <Field
          label="Remarks"
          required
          error={errors.remarks}
          hint="Reason for the change (regulatory update, internal finding ...)"
        >
          {(id) => (
            <textarea
              id={id}
              className="textarea"
              rows={3}
              maxLength={2000}
              value={request.remarks}
              onChange={(e) => set({ remarks: e.target.value })}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}

interface DetailProps {
  entryId: number;
  sources: ListSource[];
  onOpenChange: (change: WatchlistChange) => void;
  onClose: () => void;
}

function EntryFacts({ detail }: Readonly<{ detail: EntryDetail }>) {
  const e = detail.entry;
  const ids = [e.nationality, e.idNumbers].filter((v) => v !== undefined && v !== '').join(' / ');
  const aliases = aliasesText(detail.aliases);
  return (
    <dl className="detail-list">
      <dt>Status</dt>
      <dd>
        <StatusBadge status={e.status} />
      </dd>
      <dt>Source / List</dt>
      <dd>{`${e.sourceCode} / ${humanize(e.listType)}`}</dd>
      <dt>Entity Type</dt>
      <dd>{humanize(e.entityType)}</dd>
      <dt>Birth Date</dt>
      <dd>{formatDate(e.birthDate)}</dd>
      <dt>Nationality / IDs</dt>
      <dd>{ids === '' ? '—' : ids}</dd>
      <dt>Aliases</dt>
      <dd>{aliases === '' ? '—' : aliases}</dd>
      <dt>Listed / Delisted</dt>
      <dd>{`${formatDate(e.listedOn)} / ${formatDate(e.delistedOn)}`}</dd>
      <dt>Effective From / Version</dt>
      <dd>{`${formatDate(e.effectiveFrom)} / ${e.entryVersion}`}</dd>
    </dl>
  );
}

function checkerText(c: WatchlistChange): string {
  return c.decidedBy === undefined ? '—' : `${c.decidedBy} ${formatDateTime(c.decidedAt)}`;
}

function HistoryTable({
  rows,
  loading,
  onOpen,
}: Readonly<{ rows: WatchlistChange[]; loading: boolean; onOpen: (c: WatchlistChange) => void }>) {
  return (
    <DataTable<WatchlistChange>
      caption="Change history"
      rows={rows}
      rowKey={(c) => c.id}
      onRowClick={onOpen}
      loading={loading}
      columns={[
        { key: 'type', header: 'Change', render: (c) => humanize(c.changeType) },
        { key: 'status', header: 'Status', render: (c) => <StatusBadge status={c.status} /> },
        {
          key: 'maker',
          header: 'Maker',
          render: (c) => `${c.createdBy} ${formatDateTime(c.createdAt)}`,
        },
        { key: 'checker', header: 'Checker', render: checkerText },
        { key: 'remarks', header: 'Remarks', render: (c) => c.decisionRemarks ?? c.makerRemarks },
      ]}
    />
  );
}

interface ViewProps {
  detail: EntryDetail;
  sources: ListSource[];
  onOpenChange: (change: WatchlistChange) => void;
  onClose: () => void;
}

function EntryView({ detail, sources, onOpenChange, onClose }: Readonly<ViewProps>) {
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState(false);
  const [deactivating, setDeactivating] = useState(false);
  const entry = detail.entry;
  const deactivate = useMutation({
    mutationFn: (remarks: string) => screeningSetupApi.deactivateEntry(entry.id, remarks),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['screening-setup', 'watchlist'] });
      toast.success('Deactivation sent for approval');
      setDeactivating(false);
    },
  });
  const pending = detail.history.some((c) => c.status === 'PENDING');
  const maintain = can('SCR_LIST_MAINTAIN') && !pending;
  return (
    <Modal
      open
      title={`${entry.externalRef} ${entry.primaryName}`}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Close
          </Button>
          {maintain && entry.status === 'ACTIVE' && (
            <Button variant="secondary" onClick={() => setDeactivating(true)}>
              Deactivate
            </Button>
          )}
          {maintain && (
            <Button variant="accent" onClick={() => setEditing(true)}>
              Change
            </Button>
          )}
        </>
      }
    >
      <div className="stack">
        {pending && <div className="alert warning">A change of this entry waits for approval.</div>}
        <EntryFacts detail={detail} />
        <HistoryTable rows={detail.history} loading={false} onOpen={onOpenChange} />
      </div>
      {editing && (
        <EntryFormDialog
          entry={entry}
          aliases={detail.aliases}
          sources={sources}
          onClose={() => setEditing(false)}
        />
      )}
      {deactivating && (
        <RemarksDialog
          title="Deactivate Entry"
          label="Reason for the change"
          confirmLabel="Submit for Approval"
          busy={deactivate.isPending}
          error={deactivate.error}
          onConfirm={(remarks) => deactivate.mutate(remarks)}
          onClose={() => setDeactivating(false)}
        />
      )}
    </Modal>
  );
}

/** An entry with its aliases and change history, and the maker's Change / Deactivate actions. */
export function EntryDetailDialog({
  entryId,
  sources,
  onOpenChange,
  onClose,
}: Readonly<DetailProps>) {
  const detail = useQuery({
    queryKey: ['screening-setup', 'watchlist', 'entry', entryId],
    queryFn: () => screeningSetupApi.entry(entryId),
  });
  if (detail.data === undefined) {
    return (
      <Modal open title="Watchlist Entry" onClose={onClose}>
        <ErrorAlert error={detail.error} />
      </Modal>
    );
  }
  return (
    <EntryView
      detail={detail.data}
      sources={sources}
      onOpenChange={onOpenChange}
      onClose={onClose}
    />
  );
}
