import { useMutation, useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { attachmentsApi } from '@/api/attachments';
import { useAuth } from '@/auth/authContext';
import { Attachments } from '@/components/attachments/Attachments';
import { LovSelect } from '@/components/broking/LovSelect';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { useToast } from '@/components/ui/toastContext';
import { screeningMatchesApi } from './api';
import type { ScreeningMatch } from './api';
import { MAX_JUSTIFICATION, falsePositiveErrors, falsePositiveRequest } from './matchLogic';
import type { FalsePositiveForm } from './matchLogic';

/** Attachment entity type of match evidence (backend MatchDecisionService.MATCH_ENTITY). */
export const MATCH_ENTITY = 'ScreeningMatch';

/**
 * Mark False Positive (SNSRP-304; FR-SS-032, 035): the justification, at least one evidence
 * document attached to the match and, with SCR_RISK_TAG, a corrected risk rating and the removal
 * of the WATCHLIST_REVIEW tag. The client is not matched again against this entry version.
 */
export function FalsePositiveDialog({
  match,
  onDone,
  onClose,
}: Readonly<{ match: ScreeningMatch; onDone: () => void; onClose: () => void }>) {
  const { can } = useAuth();
  const toast = useToast();
  const [form, setForm] = useState<FalsePositiveForm>({
    justification: '',
    riskRating: '',
    removeWatchlistTag: false,
  });
  const [touched, setTouched] = useState(false);
  const entityId = String(match.id);
  const evidence = useQuery({
    queryKey: ['attachments', MATCH_ENTITY, entityId],
    queryFn: () => attachmentsApi.list(MATCH_ENTITY, entityId),
  });
  const errors = falsePositiveErrors(form, evidence.data?.length ?? 0);
  const save = useMutation({
    mutationFn: () => screeningMatchesApi.falsePositive(match.id, falsePositiveRequest(form)),
    onSuccess: () => {
      toast.success(`Match of ${match.clientName} cleared as a false positive`);
      onDone();
    },
  });
  const shown = (key: keyof typeof errors) => (touched ? errors[key] : undefined);
  return (
    <Modal
      open
      title="Mark False Positive"
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            busy={save.isPending}
            onClick={() => {
              setTouched(true);
              if (Object.keys(errors).length === 0) {
                save.mutate();
              }
            }}
          >
            Mark False Positive
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={save.error} />
        <p className="muted">
          {match.clientName} ({match.clientCode}) against {match.entryName}, version{' '}
          {match.entryVersion}. The client is not matched again against this version of the entry.
        </p>
        <Field label="Justification" required error={shown('justification')}>
          {(id) => (
            <textarea
              id={id}
              className="textarea"
              rows={4}
              maxLength={MAX_JUSTIFICATION}
              value={form.justification}
              onChange={(e) => setForm({ ...form, justification: e.target.value })}
            />
          )}
        </Field>
        {can('SCR_RISK_TAG') && (
          <div className="form-grid">
            <Field label="New Risk Rating" hint="Leave blank to keep the client's rating">
              {(id) => (
                <LovSelect
                  id={id}
                  type="KYC_RISK_RATING"
                  value={form.riskRating}
                  placeholder="Keep the current rating"
                  onChange={(riskRating) => setForm({ ...form, riskRating })}
                />
              )}
            </Field>
            <label className="checkbox">
              <input
                type="checkbox"
                checked={form.removeWatchlistTag}
                onChange={(e) => setForm({ ...form, removeWatchlistTag: e.target.checked })}
              />{' '}
              End the Watchlist Review tag
            </label>
          </div>
        )}
        {shown('evidence') !== undefined && (
          <p className="field-error" role="alert">
            {shown('evidence')}
          </p>
        )}
        <Attachments
          entityType={MATCH_ENTITY}
          entityId={entityId}
          title="Evidence"
          reference={match.clientCode}
          documentTypes={false}
        />
      </div>
    </Modal>
  );
}
