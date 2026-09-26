import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Save, Send } from 'lucide-react';
import { useState } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { nbadminApi } from '@/api/nbadmin';
import type { AccessRequest, AccessRequestType, UserAccess } from '@/api/nbadmin';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { useToast } from '@/components/ui/toastContext';
import { today } from '@/utils/format';
import {
  allowsExternal,
  EMPTY_ACCESS_REQUEST,
  fromAccessRequest,
  GROUP_TYPES,
  initialForm,
  isGroupProfile,
  REQUEST_TYPE_LABELS,
  toAccessRequest,
  USER_TYPES,
  validateAccessRequest,
} from './accessRequest';
import type { AccessRequestErrors, AccessRequestForm } from './accessRequest';
import { ApproverPicker } from './ApproverPicker';
import { GroupProfileFields } from './GroupProfileFields';
import { UserRequestFields } from './UserRequestFields';

type Mode = 'draft' | 'submit';

function TypeFields({
  form,
  set,
  editing,
}: Readonly<{
  form: AccessRequestForm;
  set: (p: Partial<AccessRequestForm>) => void;
  editing: boolean;
}>) {
  const types = isGroupProfile(form.type) ? GROUP_TYPES : USER_TYPES;
  return (
    <div className="form-grid">
      <Field
        label="Request Type"
        required
        hint={editing ? 'Fixed after the first save' : undefined}
      >
        {(id) => (
          <select
            id={id}
            className="select"
            disabled={editing}
            value={form.type}
            onChange={(e) =>
              set({
                ...EMPTY_ACCESS_REQUEST,
                type: e.target.value as AccessRequestType,
                justification: form.justification,
              })
            }
          >
            {types.map((t) => (
              <option key={t} value={t}>
                {REQUEST_TYPE_LABELS[t]}
              </option>
            ))}
          </select>
        )}
      </Field>
      {allowsExternal(form.type) && (
        <Field label="User Type" hint="External: portal users of insurers and clients">
          {(id) => (
            <select
              id={id}
              className="select"
              disabled={editing}
              value={form.userType}
              onChange={(e) =>
                set({ userType: e.target.value as AccessRequestForm['userType'], username: '' })
              }
            >
              <option value="INTERNAL">Internal (BIBS user)</option>
              <option value="EXTERNAL">External (portal user)</option>
            </select>
          )}
        </Field>
      )}
      {!isGroupProfile(form.type) && (
        <Field label="Effective Date" hint="Blank: applies on approval">
          {(id) => (
            <input
              id={id}
              className="input"
              type="date"
              min={today()}
              value={form.effectiveFrom}
              onChange={(e) => set({ effectiveFrom: e.target.value })}
            />
          )}
        </Field>
      )}
    </div>
  );
}

interface EditorProps {
  id?: number;
  initial: AccessRequestForm;
  saved?: AccessRequest;
  users: UserAccess[];
  userIdPattern?: string;
}

function RemarksCard({
  form,
  set,
  errors,
  returned,
  remarks,
  onRemarks,
}: Readonly<{
  form: AccessRequestForm;
  set: (p: Partial<AccessRequestForm>) => void;
  errors: AccessRequestErrors;
  returned: boolean;
  remarks: string;
  onRemarks: (text: string) => void;
}>) {
  const groupProfile = isGroupProfile(form.type);
  return (
    <Card>
      <div className="stack">
        <ApproverPicker
          userType={form.userType}
          subject={groupProfile ? undefined : form.username}
          value={form.approvers}
          onChange={(approvers) => set({ approvers })}
          multiple={groupProfile}
          error={errors.approvers}
        />
        <Field label="Remarks (Justification)" required error={errors.justification}>
          {(fieldId) => (
            <textarea
              id={fieldId}
              className="textarea"
              rows={3}
              maxLength={1000}
              value={form.justification}
              onChange={(e) => set({ justification: e.target.value })}
            />
          )}
        </Field>
        {returned && (
          <Field label="Correction Remarks" required hint="Mandatory to resubmit">
            {(fieldId) => (
              <textarea
                id={fieldId}
                className="textarea"
                rows={2}
                maxLength={1000}
                value={remarks}
                onChange={(e) => onRemarks(e.target.value)}
              />
            )}
          </Field>
        )}
      </div>
    </Card>
  );
}

function EditorActions({
  busy,
  returned,
  onRun,
}: Readonly<{ busy: Mode | undefined; returned: boolean; onRun: (mode: Mode) => void }>) {
  const navigate = useNavigate();
  return (
    <>
      <Button variant="secondary" onClick={() => void navigate(-1)}>
        Cancel
      </Button>
      <Button
        variant="secondary"
        icon={<Save size={16} />}
        busy={busy === 'draft'}
        onClick={() => onRun('draft')}
      >
        Save Draft
      </Button>
      <Button
        variant="accent"
        icon={<Send size={16} />}
        busy={busy === 'submit'}
        onClick={() => onRun('submit')}
      >
        {returned ? 'Resubmit' : 'Submit'}
      </Button>
    </>
  );
}

function RequestEditor({ id, initial, saved, users, userIdPattern }: Readonly<EditorProps>) {
  const navigate = useNavigate();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [form, setForm] = useState<AccessRequestForm>(initial);
  const [remarks, setRemarks] = useState('');
  const [attempt, setAttempt] = useState<Mode>();
  const set = (patch: Partial<AccessRequestForm>) => setForm((f) => ({ ...f, ...patch }));
  const returned = saved?.status === 'RETURNED';
  const context = (mode: Mode | undefined) => ({
    users,
    submit: mode === 'submit',
    userIdPattern,
    today: today(),
  });
  const errors = attempt === undefined ? {} : validateAccessRequest(form, context(attempt));
  const save = useMutation({
    mutationFn: async (mode: Mode): Promise<AccessRequest> => {
      const body = toAccessRequest(form);
      if (id === undefined) {
        return nbadminApi.create(body, mode === 'draft');
      }
      const edited = await nbadminApi.edit(id, body);
      return mode === 'draft' ? edited : nbadminApi.submitSaved(id, form.approvers, remarks);
    },
    onSuccess: async (r) => {
      await queryClient.invalidateQueries({ queryKey: ['nbadmin'] });
      toast.success(`${r.requestNo} ${r.status === 'DRAFT' ? 'saved as a draft' : 'submitted'}`);
      void navigate(`/user-access/requests/${String(r.id)}`);
    },
  });
  const run = (mode: Mode) => {
    setAttempt(mode);
    if (Object.keys(validateAccessRequest(form, context(mode))).length === 0) {
      save.mutate(mode);
    }
  };
  const groupProfile = isGroupProfile(form.type);
  return (
    <div className="stack">
      <PageHeader
        section={`User Access · ${groupProfile ? 'Group Profile Requests' : 'Access Requests'}`}
        backTo={groupProfile ? '/user-access/group-profiles' : '/user-access/requests'}
        title={saved === undefined ? 'New Request' : `Edit ${saved.requestNo}`}
        description="Save a draft to finish later, or submit it to the approver. Nothing changes until the request is approved."
        actions={
          <EditorActions
            busy={save.isPending ? save.variables : undefined}
            returned={returned}
            onRun={run}
          />
        }
      />
      <ErrorAlert error={save.error} />
      {returned && saved.decisionComment && (
        <div className="alert warning" role="status">
          Returned by {saved.decidedBy}: {saved.decisionComment}
        </div>
      )}
      <Card>
        <div className="stack">
          <TypeFields form={form} set={set} editing={id !== undefined} />
          {groupProfile ? (
            <GroupProfileFields form={form} set={set} errors={errors} />
          ) : (
            <UserRequestFields form={form} set={set} errors={errors} users={users} />
          )}
        </div>
      </Card>
      <RemarksCard
        form={form}
        set={set}
        errors={errors}
        returned={returned}
        remarks={remarks}
        onRemarks={setRemarks}
      />
    </div>
  );
}

/**
 * New / Edit Request (BRD 1.002-1.007, 3.002; FR-UA-010 to FR-UA-016, FR-UA-040 to FR-UA-044):
 * one form for the user and group-profile requests, saved as a draft (formats only) or submitted
 * to the chosen approver(s) after the full checks; a returned request is corrected and resubmitted
 * with a correction remark.
 */
export default function AccessRequestFormPage() {
  const params = useParams();
  const [search] = useSearchParams();
  const id = params.id === undefined ? undefined : Number(params.id);
  const request = useQuery({
    queryKey: ['nbadmin', 'request', id],
    queryFn: () => nbadminApi.request(id ?? 0),
    enabled: id !== undefined,
  });
  const roles = useQuery({ queryKey: ['nbadmin', 'roles'], queryFn: nbadminApi.roles });
  const users = useQuery({ queryKey: ['nbadmin', 'users'], queryFn: nbadminApi.users });
  const settings = useQuery({ queryKey: ['nbadmin', 'settings'], queryFn: nbadminApi.settings });
  const error = request.error ?? roles.error ?? users.error;
  const ready =
    roles.data !== undefined &&
    users.data !== undefined &&
    (id === undefined || request.data !== undefined);
  if (!ready) {
    return error ? <ErrorAlert error={error} /> : <span className="spinner" aria-label="Loading" />;
  }
  const initial =
    request.data === undefined
      ? initialForm(search, users.data)
      : fromAccessRequest(request.data, roles.data);
  return (
    <RequestEditor
      key={id ?? 'new'}
      id={id}
      initial={initial}
      saved={request.data}
      users={users.data}
      userIdPattern={settings.data?.userIdPattern}
    />
  );
}
