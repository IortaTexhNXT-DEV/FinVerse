import { Check, Save, Send, Trash2, X } from 'lucide-react';
import type { ReactNode } from 'react';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { Field } from '@/components/ui/Field';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatDate, formatDateTime, today } from '@/utils/format';
import type { ConfigChange, ConfigContent, ConfigVersion } from './api';

/** Building blocks of the configuration version workbench (FR-SS-010, 019). */

export interface Draft {
  id: number;
  effectiveFrom: string;
  changeNote: string;
  content: ConfigContent;
}

export function VersionList({
  versions,
  selected,
  onSelect,
}: Readonly<{ versions: ConfigVersion[]; selected?: number; onSelect: (id: number) => void }>) {
  return (
    <Card title="Versions" flush>
      <DataTable<ConfigVersion>
        caption="Versions"
        rows={versions}
        rowKey={(v) => v.id}
        onRowClick={(v) => onSelect(v.id)}
        emptyMessage="No version yet: click New Draft to create the first one"
        columns={[
          {
            key: 'no',
            header: 'Version',
            render: (v) =>
              v.id === selected ? <strong>v{v.versionNo}</strong> : `v${v.versionNo}`,
          },
          { key: 'status', header: 'Status', render: (v) => <StatusBadge status={v.status} /> },
          { key: 'from', header: 'Effective From', render: (v) => formatDate(v.effectiveFrom) },
          { key: 'maker', header: 'Maker', render: (v) => v.submittedBy ?? v.createdBy },
          { key: 'checker', header: 'Decided By', render: (v) => v.decidedBy ?? '—' },
          {
            key: 'note',
            header: 'Change Note',
            render: (v) => v.decisionReason ?? v.changeNote ?? '',
          },
        ]}
      />
    </Card>
  );
}

export function ChangesCard({ changes }: Readonly<{ changes: ConfigChange[] }>) {
  return (
    <Card title="Changes Against the Version in Force" flush>
      <DataTable<ConfigChange>
        caption="Changes"
        rows={changes}
        rowKey={(c) => `${c.item}|${c.attribute}`}
        emptyMessage="No difference with the version in force"
        columns={[
          { key: 'item', header: 'Rule', render: (c) => c.item },
          { key: 'attr', header: 'Attribute', render: (c) => c.attribute },
          { key: 'before', header: 'Before', render: (c) => c.before ?? '—' },
          { key: 'after', header: 'After', render: (c) => c.after ?? '—' },
        ]}
      />
    </Card>
  );
}

export interface ActionsProps {
  version: ConfigVersion;
  draft?: Draft;
  isMaker: boolean;
  mayDecide: boolean;
  busy: boolean;
  saving: boolean;
  submitting: boolean;
  deciding: boolean;
  onSave: (draft: Draft) => void;
  onSubmit: (draft: Draft) => void;
  onDiscard: () => void;
  onApprove: () => void;
  onReject: () => void;
}

export function VersionActions(p: Readonly<ActionsProps>) {
  const { draft } = p;
  return (
    <div className="row">
      <StatusBadge status={p.version.status} />
      {draft !== undefined && (
        <>
          <Button
            variant="secondary"
            icon={<Save size={16} />}
            busy={p.saving}
            onClick={() => p.onSave(draft)}
          >
            Save
          </Button>
          <Button
            variant="accent"
            icon={<Send size={16} />}
            busy={p.submitting}
            disabled={p.busy}
            onClick={() => p.onSubmit(draft)}
          >
            Submit for Approval
          </Button>
          {p.isMaker && (
            <Button
              variant="ghost"
              icon={<Trash2 size={16} />}
              disabled={p.busy}
              onClick={p.onDiscard}
            >
              Discard
            </Button>
          )}
        </>
      )}
      {p.mayDecide && (
        <>
          <Button variant="secondary" icon={<X size={16} />} disabled={p.busy} onClick={p.onReject}>
            Reject
          </Button>
          <Button
            variant="accent"
            icon={<Check size={16} />}
            busy={p.deciding}
            onClick={p.onApprove}
          >
            Approve
          </Button>
        </>
      )}
    </div>
  );
}

function decisionText(v: ConfigVersion): string {
  return v.decidedAt === undefined
    ? '—'
    : `${v.decidedBy ?? ''} ${formatDateTime(v.decidedAt)} ${v.decisionReason ?? ''}`.trim();
}

function submittedText(v: ConfigVersion): string {
  return v.submittedAt === undefined
    ? '—'
    : `${v.submittedBy ?? ''} ${formatDateTime(v.submittedAt)}`;
}

interface HeaderProps {
  version: ConfigVersion;
  draft?: Draft;
  onDraft: (draft: Draft) => void;
  actions: ReactNode;
}

export function VersionHeaderCard({ version, draft, onDraft, actions }: Readonly<HeaderProps>) {
  return (
    <Card title={version.label} actions={actions}>
      <div className="form-grid">
        <Field label="Effective From" required hint="Today or later">
          {(id) => (
            <input
              id={id}
              type="date"
              className="input"
              disabled={draft === undefined}
              min={today()}
              value={draft?.effectiveFrom ?? version.effectiveFrom}
              onChange={(e) => draft && onDraft({ ...draft, effectiveFrom: e.target.value })}
            />
          )}
        </Field>
        <Field label="Change Note">
          {(id) => (
            <input
              id={id}
              className="input"
              maxLength={1000}
              disabled={draft === undefined}
              value={draft?.changeNote ?? version.changeNote ?? ''}
              onChange={(e) => draft && onDraft({ ...draft, changeNote: e.target.value })}
            />
          )}
        </Field>
        <Field label="Submitted">
          {(id) => <input id={id} className="input" disabled value={submittedText(version)} />}
        </Field>
        <Field label="Decision">
          {(id) => <input id={id} className="input" disabled value={decisionText(version)} />}
        </Field>
      </div>
    </Card>
  );
}
