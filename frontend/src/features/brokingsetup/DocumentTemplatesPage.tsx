import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { FilePlus2, FileType2 } from 'lucide-react';
import { useMemo, useState } from 'react';
import { docTemplatesApi } from '@/api/docTemplates';
import type { DocTemplateVersion, TemplateDraft } from '@/api/docTemplates';
import { useAuth } from '@/auth/authContext';
import { useFileDownload } from '@/components/broking/useFileDownload';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { formatDate, formatDateTime, today } from '@/utils/format';

/**
 * Document templates (BRNB.004): the texts merged into quotations, slips, insurance advice and
 * e-mails. A change is a new version effective from a date; generated documents keep the version
 * they used. A version downloads as Word, and an edited Word file loads into a new version (client
 * requirement 16).
 */
export default function DocumentTemplatesPage() {
  const { can } = useAuth();
  const templates = useQuery({ queryKey: ['doc-templates'], queryFn: docTemplatesApi.all });
  const [code, setCode] = useState<string>('');
  const [editing, setEditing] = useState<DocTemplateVersion | null>(null);
  const codes = useMemo(
    () => [...new Set((templates.data ?? []).map((t) => t.code))],
    [templates.data],
  );
  const current = code === '' ? (codes[0] ?? '') : code;
  const versions = (templates.data ?? []).filter((t) => t.code === current);
  const latest = versions[0];
  const canEdit = can('MASTER_MAINTAIN') || can('LOV_MANAGE');
  const download = useFileDownload();
  const downloadWord = (v: DocTemplateVersion) =>
    download.mutate(() => docTemplatesApi.word(v.code, v.versionNo));
  return (
    <div className="stack">
      <PageHeader
        section="Broking Setup"
        title="Document Templates"
        description="Texts of generated documents with {{placeholders}} filled from the record. Changes take effect from the chosen date."
        actions={
          canEdit &&
          latest && (
            <Button
              variant="accent"
              icon={<FilePlus2 size={16} />}
              onClick={() => setEditing(latest)}
            >
              New Version
            </Button>
          )
        }
      />
      <ErrorAlert error={templates.error ?? download.error} />
      <div className="split">
        <Card title="Templates" flush>
          <ul className="nav-list">
            {codes.map((c) => (
              <li key={c}>
                <button
                  type="button"
                  className={c === current ? 'nav-list-item active' : 'nav-list-item'}
                  onClick={() => setCode(c)}
                >
                  {(templates.data ?? []).find((t) => t.code === c)?.title ?? c}
                  <span className="muted mono">{c}</span>
                </button>
              </li>
            ))}
          </ul>
        </Card>
        <div className="stack">
          {latest && (
            <Card
              title={`${latest.title} — version ${latest.versionNo}`}
              actions={
                <div className="row">
                  <span className="muted">effective {formatDate(latest.effectiveFrom)}</span>
                  <Button
                    size="sm"
                    variant="secondary"
                    icon={<FileType2 size={14} />}
                    busy={download.isPending}
                    onClick={() => downloadWord(latest)}
                  >
                    Download Word
                  </Button>
                </div>
              }
            >
              <pre className="message-body">{latest.body}</pre>
            </Card>
          )}
          <Card title="Version history" flush>
            <DataTable<DocTemplateVersion>
              rows={versions}
              rowKey={(v) => v.id}
              columns={[
                { key: 'v', header: 'Version', numeric: true, render: (v) => v.versionNo },
                { key: 'title', header: 'Title', render: (v) => v.title },
                {
                  key: 'from',
                  header: 'Effective from',
                  render: (v) => formatDate(v.effectiveFrom),
                },
                {
                  key: 'status',
                  header: 'Status',
                  render: (v) => <StatusBadge status={v.active ? 'ACTIVE' : 'INACTIVE'} />,
                },
                {
                  key: 'by',
                  header: 'Created',
                  render: (v) => `${v.createdBy} · ${formatDateTime(v.createdAt)}`,
                },
                {
                  key: 'word',
                  header: '',
                  render: (v) => (
                    <Button
                      size="sm"
                      variant="ghost"
                      icon={<FileType2 size={14} />}
                      aria-label={`Download version ${v.versionNo} as Word`}
                      onClick={() => downloadWord(v)}
                    >
                      Word
                    </Button>
                  ),
                },
              ]}
            />
          </Card>
        </div>
      </div>
      {editing && <NewVersionDialog base={editing} onClose={() => setEditing(null)} />}
    </div>
  );
}

function NewVersionDialog({
  base,
  onClose,
}: Readonly<{ base: DocTemplateVersion; onClose: () => void }>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [title, setTitle] = useState(base.title);
  const [body, setBody] = useState(base.body);
  const [effectiveFrom, setEffectiveFrom] = useState(today());
  const [draft, setDraft] = useState<TemplateDraft | null>(null);
  const load = useMutation({
    mutationFn: (file: File) => docTemplatesApi.readWord(base.code, file),
    onSuccess: (d) => {
      setTitle(d.title);
      setBody(d.body);
      setDraft(d);
    },
  });
  const save = useMutation({
    mutationFn: () => docTemplatesApi.newVersion(base.code, { title, body, effectiveFrom }),
    onSuccess: async (v) => {
      await queryClient.invalidateQueries({ queryKey: ['doc-templates'] });
      toast.success(`${v.code} version ${v.versionNo} saved`);
      onClose();
    },
  });
  return (
    <Modal
      open
      title={`New version of ${base.code}`}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            variant="accent"
            busy={save.isPending}
            disabled={!title.trim() || !body.trim()}
            onClick={() => save.mutate()}
          >
            Save Version
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={save.error ?? load.error} />
        <Field
          label="Load from Word"
          hint="A .docx file: the first paragraph is the title, the others the text. Review it below before saving."
        >
          {(id) => (
            <input
              id={id}
              type="file"
              className="input"
              accept=".docx,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
              disabled={load.isPending}
              onChange={(e) => {
                const file = e.target.files?.[0];
                if (file !== undefined) {
                  load.mutate(file);
                }
              }}
            />
          )}
        </Field>
        {draft !== null && <PlaceholderCheck draft={draft} />}
        <Field label="Title" required>
          {(id) => (
            <input
              id={id}
              className="input"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
            />
          )}
        </Field>
        <Field label="Effective from" required>
          {(id) => (
            <input
              id={id}
              type="date"
              className="input"
              value={effectiveFrom}
              onChange={(e) => setEffectiveFrom(e.target.value)}
            />
          )}
        </Field>
        <Field
          label="Text"
          required
          hint="Keep the {{placeholders}} of the current version; they are filled from the record."
        >
          {(id) => (
            <textarea
              id={id}
              className="textarea mono"
              rows={12}
              value={body}
              onChange={(e) => setBody(e.target.value)}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}

/** Placeholders an uploaded Word draft dropped or added, compared with the latest version. */
function PlaceholderCheck({ draft }: Readonly<{ draft: TemplateDraft }>) {
  if (draft.missingPlaceholders.length === 0 && draft.addedPlaceholders.length === 0) {
    return <div className="alert success">Loaded from Word: the placeholders are unchanged.</div>;
  }
  return (
    <div className="alert warning">
      {draft.missingPlaceholders.length > 0 && (
        <div>
          No longer in the text: {draft.missingPlaceholders.map((p) => `{{${p}}}`).join(', ')}
        </div>
      )}
      {draft.addedPlaceholders.length > 0 && (
        <div>
          New, filled only if the record supplies them:{' '}
          {draft.addedPlaceholders.map((p) => `{{${p}}}`).join(', ')}
        </div>
      )}
    </div>
  );
}
