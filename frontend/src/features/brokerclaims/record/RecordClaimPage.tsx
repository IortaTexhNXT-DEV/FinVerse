import { useSearchParams } from 'react-router-dom';
import type { ApiError } from '@/api/client';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { useCompanyId } from '@/context/workspaceContext';
import { CLAIMS_SECTION } from '../ClaimsPlaceholder';
import type { ClaimDraft } from '../cover/api';
import { CoverSearch } from '../cover/CoverSearch';
import { InsurerLinesEditor } from '../insurer/InsurerLinesEditor';
import { LocationPicker } from '../location/LocationPicker';
import type { ClaimSource } from './api';
import { CoverCard } from './CoverCard';
import { DialogFooter } from './FormParts';
import { LossFields } from './LossFields';
import { policyYearLabel } from './recordLogic';
import type { RecordState } from './useRecordClaim';
import { useRecordClaim } from './useRecordClaim';

function ClaimSetup({
  draft,
  policyYear,
  source,
  onYear,
  onSource,
}: Readonly<{
  draft: ClaimDraft;
  policyYear: number;
  source: ClaimSource;
  onYear: (year: number) => void;
  onSource: (source: ClaimSource) => void;
}>) {
  return (
    <div className="form-grid">
      <Field label="Policy Year" required>
        {(id) => (
          <select
            id={id}
            className="select"
            value={policyYear}
            onChange={(e) => onYear(Number(e.target.value))}
          >
            {draft.years.map((y) => (
              <option key={y.year} value={y.year}>
                {policyYearLabel(y.year, y.from, y.to)}
              </option>
            ))}
          </select>
        )}
      </Field>
      <Field label="Source" required>
        {(id) => (
          <select
            id={id}
            className="select"
            value={source}
            onChange={(e) => onSource(e.target.value as ClaimSource)}
          >
            <option value="BDOI_NOTICE">BDOI notice</option>
            <option value="INSURER_REPORTED">Insurer-reported</option>
          </select>
        )}
      </Field>
    </div>
  );
}

function ClaimEditor({ draft, state }: Readonly<{ draft: ClaimDraft; state: RecordState }>) {
  const { form, setForm, errors, picks, setPicks, lines, setLines, policyYear, setPolicyYear } =
    state;
  return (
    <>
      <CoverCard draft={draft} />
      <Card title="Claim">
        <ClaimSetup
          draft={draft}
          policyYear={policyYear}
          source={form.source}
          onYear={(y) => {
            setPolicyYear(y);
            setLines(undefined);
          }}
          onSource={(s) => setForm({ ...form, source: s })}
        />
      </Card>
      {draft.locations.length > 0 && (
        <Card title="Insured Locations">
          <LocationPicker locations={draft.locations} picks={picks} onChange={setPicks} />
        </Card>
      )}
      <Card title="Loss Details">
        <LossFields
          form={form}
          errors={errors}
          onChange={(key, value) => setForm({ ...form, [key]: value })}
          currency={draft.currency}
        />
      </Card>
      <Card title="Insurers">
        <div className="stack">
          {errors.insurerClaimNos && <div className="field-error">{errors.insurerClaimNos}</div>}
          <InsurerLinesEditor lines={lines} onChange={setLines} />
        </div>
      </Card>
    </>
  );
}

function ConfirmModal({
  question,
  busy,
  onClose,
  onConfirm,
}: Readonly<{ question: ApiError; busy: boolean; onClose: () => void; onConfirm: () => void }>) {
  return (
    <Modal
      title="Confirm to Continue"
      open
      onClose={onClose}
      footer={
        <DialogFooter
          label="Confirm and Save"
          busy={busy}
          onClose={onClose}
          onConfirm={onConfirm}
        />
      }
    >
      <p>{question.message}</p>
    </Modal>
  );
}

/**
 * Record Claim (BRCLM.001/003/004/006/009/016/037/039/041/043; FR-CL-011): find the cover, check
 * the cover card and premium, pick the locations, enter the loss and confirm the insurers. The claim
 * gets its BCL number, phase NEW and the handler's queue; an unpaid cover is recorded and flagged.
 */
export default function RecordClaimPage() {
  const companyId = useCompanyId();
  const [params, setParams] = useSearchParams();
  const arn = params.get('arn') ?? '';
  const state = useRecordClaim(companyId, arn);
  const { draft, save, question, setQuestion } = state;
  const d = draft.data;
  const saveError = question === undefined ? save.error : undefined;
  return (
    <div className="stack">
      <PageHeader
        section={CLAIMS_SECTION}
        title="Record Claim"
        description="Find the cover by ARN, policy number or assured, check the premium, then record the loss, the locations and the insurers."
        backTo={arn === '' ? '/claims-handling' : '/claims-handling/new'}
        actions={
          d ? (
            <Button variant="accent" busy={save.isPending} onClick={state.submit}>
              Save Claim
            </Button>
          ) : undefined
        }
      />
      {arn === '' && <CoverSearch onSelect={(c) => setParams({ arn: c.arn })} />}
      <ErrorAlert error={draft.error ?? saveError} />
      {draft.isLoading && <span className="spinner" aria-label="Loading" />}
      {d && <ClaimEditor draft={d} state={state} />}
      {question !== undefined && (
        <ConfirmModal
          question={question}
          busy={save.isPending}
          onClose={() => setQuestion(undefined)}
          onConfirm={state.confirmQuestion}
        />
      )}
    </div>
  );
}
