import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { FilePlus2 } from 'lucide-react';
import { useMemo, useState } from 'react';
import { docTemplatesApi } from '@/api/docTemplates';
import type { DocTemplateVersion } from '@/api/docTemplates';
import { useAuth } from '@/auth/authContext';
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
 * they used.
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
              New version
            </Button>
          )
        }
      />
      <ErrorAlert error={templates.error} />
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
              actions={<span className="muted">effective {formatDate(latest.effectiveFrom)}</span>}
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
            Save version
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={save.error} />
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
