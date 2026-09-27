import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { CheckCircle2, CircleAlert, Pencil, Send } from 'lucide-react';
import { useState } from 'react';
import { productMaintApi } from '@/api/productmaint';
import type { Advisory, PackageRequest } from '@/api/productmaint';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { EmptyState } from '@/components/ui/EmptyState';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { TextInput } from '@/features/assets/FormControls';
import { formatDateTime, humanize } from '@/utils/format';

const GROUPS = ['MARKETING', 'TSU', 'MBS', 'PROCESSING', 'OPERATIONS'];

function EditDialog({
  advisory,
  onClose,
  onSaved,
}: Readonly<{ advisory: Advisory; onClose: () => void; onSaved: () => Promise<void> }>) {
  const [groups, setGroups] = useState(advisory.groups);
  const [emailTo, setEmailTo] = useState(advisory.emailTo.join(', '));
  const [subject, setSubject] = useState(advisory.subject);
  const [body, setBody] = useState(advisory.body);
  const save = useMutation({
    mutationFn: () =>
      productMaintApi.updateAdvisory(advisory.id, {
        groups,
        emailTo: emailTo
          .split(',')
          .map((e) => e.trim())
          .filter((e) => e !== ''),
        subject,
        body,
      }),
    onSuccess: onSaved,
  });
  return (
    <Modal
      open
      title="Edit Advisory"
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            variant="accent"
            busy={save.isPending}
            disabled={groups.length === 0 || subject.trim() === '' || body.trim() === ''}
            onClick={() => save.mutate()}
          >
            Save
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={save.error} />
        <fieldset className="field">
          <legend>Recipient groups</legend>
          <div className="insurer-choices">
            {GROUPS.map((g) => (
              <label key={g} className="checkbox">
                <input
                  type="checkbox"
                  checked={groups.includes(g)}
                  onChange={() =>
                    setGroups(groups.includes(g) ? groups.filter((x) => x !== g) : [...groups, g])
                  }
                />
                {humanize(g)}
              </label>
            ))}
          </div>
        </fieldset>
        <TextInput
          label="E-mail to (optional)"
          hint="Comma-separated addresses; the advisory PDF is sent password protected."
          value={emailTo}
          onChange={setEmailTo}
        />
        <TextInput label="Subject" required value={subject} onChange={setSubject} />
        <Field label="Text" required>
          {(id) => (
            <textarea
              id={id}
              className="textarea"
              rows={6}
              maxLength={8000}
              value={body}
              onChange={(e) => setBody(e.target.value)}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}

function AdvisoryCard({
  advisory,
  onChanged,
}: Readonly<{ advisory: Advisory; onChanged: () => Promise<void> }>) {
  const { can } = useAuth();
  const toast = useToast();
  const [editing, setEditing] = useState(false);
  const draft = advisory.status === 'DRAFT' && can('PKG_ADVISORY');
  const missing = advisory.documents.filter((d) => !d.attached);
  const send = useMutation({
    mutationFn: () => productMaintApi.sendAdvisory(advisory.id),
    onSuccess: async () => {
      await onChanged();
      toast.success('Advisory sent');
    },
  });
  return (
    <Card
      title={advisory.subject}
      actions={
        <span className="row">
          <StatusBadge status={advisory.status} />
          {draft && (
            <>
              <Button
                size="sm"
                variant="secondary"
                icon={<Pencil size={14} />}
                onClick={() => setEditing(true)}
              >
                Edit
              </Button>
              <Button
                size="sm"
                variant="accent"
                icon={<Send size={14} />}
                busy={send.isPending}
                disabled={missing.length > 0}
                title={missing.length > 0 ? 'Attach the supporting documents first' : undefined}
                onClick={() => send.mutate()}
              >
                Send Advisory
              </Button>
            </>
          )}
        </span>
      }
    >
      <div className="stack">
        <ErrorAlert error={send.error} />
        <p className="muted">
          {humanize(advisory.type)} · to {advisory.groups.map(humanize).join(', ')}
          {advisory.sentAt &&
            ` · sent ${formatDateTime(advisory.sentAt)} by ${advisory.sentBy ?? ''}`}
        </p>
        <p className="message-body">{advisory.body}</p>
        <ul className="checklist">
          {advisory.documents.map((d) => (
            <li key={d.documentType} className="checklist-item">
              {d.attached ? (
                <CheckCircle2 size={16} className="text-success" aria-label="Attached" />
              ) : (
                <CircleAlert size={16} className="text-danger" aria-label="Missing" />
              )}{' '}
              {humanize(d.documentType)}
            </li>
          ))}
        </ul>
        {missing.length > 0 && advisory.status === 'DRAFT' && (
          <p className="text-danger" role="status">
            The advisory cannot be sent until the supporting documents are attached (Documents tab).
          </p>
        )}
      </div>
      {editing && (
        <EditDialog
          advisory={advisory}
          onClose={() => setEditing(false)}
          onSaved={async () => {
            setEditing(false);
            await onChanged();
            toast.success('Advisory saved');
          }}
        />
      )}
    </Card>
  );
}

/**
 * Advisories of a package request (BRPM.016): drafted on release or retirement, edited and sent to
 * the recipient groups; sending is blocked while a supporting document is missing.
 */
export function AdvisoriesTab({ request }: Readonly<{ request: PackageRequest }>) {
  const queryClient = useQueryClient();
  const key = ['package-request-tab', request.id, 'advisories'];
  const list = useQuery({ queryKey: key, queryFn: () => productMaintApi.advisories(request.id) });
  const onChanged = () => queryClient.invalidateQueries({ queryKey: key });
  const advisories = list.data ?? [];
  return (
    <div className="stack">
      <ErrorAlert error={list.error} />
      {advisories.length === 0 && !list.isLoading ? (
        <Card>
          <EmptyState message="An advisory is drafted when the package is released or retired" />
        </Card>
      ) : (
        advisories.map((a) => <AdvisoryCard key={a.id} advisory={a} onChanged={onChanged} />)
      )}
    </div>
  );
}
