import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Save } from 'lucide-react';
import { useState } from 'react';
import { ApiError } from '@/api/client';
import { LovSelect } from '@/components/broking/LovSelect';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { EmptyState } from '@/components/ui/EmptyState';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { formatDateTime } from '@/utils/format';
import { casesApi } from './api';
import type { CaseDetail, CaseDocument, TemplateField } from './api';
import { fieldError, missingFields, sections } from './caseLogic';

interface InputProps {
  field: TemplateField;
  id: string;
  value: string;
  disabled: boolean;
  documents: CaseDocument[];
  onChange: (value: string) => void;
}

function FieldInput({ field, id, value, disabled, documents, onChange }: Readonly<InputProps>) {
  switch (field.dataType) {
    case 'LONG_TEXT':
      return (
        <textarea
          id={id}
          className="textarea"
          rows={3}
          maxLength={4000}
          disabled={disabled}
          value={value}
          onChange={(e) => onChange(e.target.value)}
        />
      );
    case 'LOV':
      return (
        <LovSelect
          id={id}
          type={field.lovType ?? ''}
          value={value}
          disabled={disabled}
          onChange={onChange}
        />
      );
    case 'CHECKBOX':
      return (
        <input
          id={id}
          type="checkbox"
          disabled={disabled}
          checked={value.toUpperCase() === 'TRUE'}
          onChange={(e) => onChange(e.target.checked ? 'TRUE' : 'FALSE')}
        />
      );
    case 'ATTACHMENT':
      return (
        <select
          id={id}
          className="select"
          disabled={disabled}
          value={value}
          onChange={(e) => onChange(e.target.value)}
        >
          <option value="">Select a case document…</option>
          {documents.map((d) => (
            <option key={d.attachmentId} value={String(d.attachmentId)}>
              {d.nominatedName}
            </option>
          ))}
        </select>
      );
    default:
      return (
        <input
          id={id}
          className="input"
          type={field.dataType === 'DATE' ? 'date' : 'text'}
          inputMode={
            field.dataType === 'NUMBER' || field.dataType === 'AMOUNT' ? 'decimal' : undefined
          }
          disabled={disabled}
          value={value}
          onChange={(e) => onChange(e.target.value)}
        />
      );
  }
}

/**
 * The guided review (SNSRP-501; FR-SS-050): the template of the case type in the version the
 * review started with, mandatory fields marked, Save Draft at any time; wrong values are flagged
 * next to their field and missing mandatory fields are listed before submitting.
 */
export function ReviewTab({ detail }: Readonly<{ detail: CaseDetail }>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const caseId = detail.row.id;
  const review = useQuery({
    queryKey: ['screening', 'case', caseId, 'review'],
    queryFn: () => casesApi.review(caseId),
  });
  const documents = useQuery({
    queryKey: ['screening', 'case', caseId, 'documents'],
    queryFn: () => casesApi.documents(caseId),
  });
  const [edits, setEdits] = useState<{ at: number; values: Record<string, string> }>();
  const values = edits?.at === review.dataUpdatedAt ? edits.values : (review.data?.values ?? {});
  const setValues = (next: Record<string, string>) =>
    setEdits({ at: review.dataUpdatedAt, values: next });
  const save = useMutation({
    mutationFn: () => casesApi.saveReview(caseId, values),
    onSuccess: async () => {
      toast.success('Review saved as a draft');
      await queryClient.invalidateQueries({ queryKey: ['screening', 'case', caseId] });
    },
  });
  if (review.isLoading) {
    return <span className="spinner" aria-label="Loading" />;
  }
  const form = review.data;
  if (form === undefined) {
    return (
      <Card>
        <ErrorAlert error={review.error} />
        <EmptyState message="No review template of this case type is in force; ask Compliance to activate one." />
      </Card>
    );
  }
  const editable = detail.actions.includes('REVIEW_EDIT');
  const serverErrors = save.error instanceof ApiError ? save.error.fieldErrors : {};
  const missing = editable ? missingFields(form.fields, values) : {};
  return (
    <Card
      title={`${form.name} (version ${form.templateVersionId})`}
      actions={
        editable ? (
          <Button icon={<Save size={16} />} busy={save.isPending} onClick={() => save.mutate()}>
            Save Draft
          </Button>
        ) : (
          <StatusBadge status={form.status} />
        )
      }
    >
      <div className="stack">
        <ErrorAlert error={save.error} />
        {form.submittedAt && (
          <p className="muted">
            Submitted by {form.submittedBy} on {formatDateTime(form.submittedAt)}.
          </p>
        )}
        {Object.keys(missing).length > 0 && (
          <p className="muted">
            Complete the {Object.keys(missing).length} mandatory field(s) before submitting.
          </p>
        )}
        {sections(form.fields).map((group) => (
          <fieldset key={group.section} className="form-grid">
            <legend>{group.section}</legend>
            {group.fields.map((field) => (
              <Field
                key={field.code}
                label={field.label}
                required={field.mandatory}
                hint={field.help ?? undefined}
                error={serverErrors[field.code] ?? fieldError(field, values[field.code])}
              >
                {(id) => (
                  <FieldInput
                    field={field}
                    id={id}
                    value={values[field.code] ?? ''}
                    disabled={!editable}
                    documents={documents.data ?? []}
                    onChange={(value) => setValues({ ...values, [field.code]: value })}
                  />
                )}
              </Field>
            ))}
          </fieldset>
        ))}
      </div>
    </Card>
  );
}
