import { useMutation, useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ApiError } from '@/api/client';
import { useToast } from '@/components/ui/toastContext';
import { today } from '@/utils/format';
import { coverApi } from '../cover/api';
import type { LineDraft } from '../insurer/InsurerLinesEditor';
import type { LocationPick } from '../location/api';
import { claimApi } from './api';
import { linesFromShares, toRecordInput } from './recordInput';
import type { ClaimForm, ClaimFormErrors } from './recordLogic';
import { claimFormErrors, emptyClaimForm } from './recordLogic';

/** Error codes the user may confirm to go on (FR-CL-011, FR-CL-021). */
const CONFIRMABLE = new Set(['BCL_LOSS_OUTSIDE_COVER', 'BCL_INSURER_CLAIM_NO_REUSED']);

const DATE = /^\d{4}-\d{2}-\d{2}$/;

/**
 * State and actions of Record Claim (FR-CL-011): the form, locations and insurer lines, the cover
 * card of the chosen policy year and loss date, the save with the confirmations BIBS may ask for
 * (loss date outside the cover period, insurer claim number already on another claim).
 */
export function useRecordClaim(companyId: number, arn: string) {
  const navigate = useNavigate();
  const toast = useToast();
  const [policyYear, setPolicyYear] = useState(1);
  const [form, setForm] = useState<ClaimForm>(() => emptyClaimForm(today()));
  const [errors, setErrors] = useState<ClaimFormErrors>({});
  const [picks, setPicks] = useState<LocationPick[]>([]);
  const [edited, setLines] = useState<LineDraft[]>();
  const [confirm, setConfirm] = useState({ outsidePeriod: false, reuse: false });
  const [question, setQuestion] = useState<ApiError>();
  const lossDate = DATE.test(form.lossDate) ? form.lossDate : undefined;
  const draft = useQuery({
    queryKey: ['broker-claims', 'draft', companyId, arn, policyYear, lossDate],
    queryFn: () => coverApi.draft(companyId, arn, policyYear, lossDate),
    enabled: arn !== '',
  });
  const lines = edited ?? linesFromShares(draft.data?.insurers ?? []);
  const save = useMutation({
    mutationFn: (flags: { outsidePeriod: boolean; reuse: boolean }) =>
      claimApi.record(toRecordInput(companyId, policyYear, { ...form, arn }, picks, lines, flags)),
    onSuccess: (claim) => {
      toast.success(`Claim ${claim.claimNo} recorded`);
      void navigate(`/claims-handling/${claim.id}`);
    },
    onError: (error) => {
      if (error instanceof ApiError && CONFIRMABLE.has(error.code)) {
        setQuestion(error);
      }
    },
  });
  const submit = () => {
    const found = claimFormErrors(
      { ...form, arn, insurerClaimNos: lines.map((l) => l.insurerClaimNo) },
      today(),
    );
    setErrors(found);
    if (Object.keys(found).length === 0) {
      save.mutate(confirm);
    }
  };
  const confirmQuestion = () => {
    const next = {
      outsidePeriod: confirm.outsidePeriod || question?.code === 'BCL_LOSS_OUTSIDE_COVER',
      reuse: confirm.reuse || question?.code === 'BCL_INSURER_CLAIM_NO_REUSED',
    };
    setConfirm(next);
    setQuestion(undefined);
    save.mutate(next);
  };
  return {
    form,
    setForm,
    errors,
    picks,
    setPicks,
    lines,
    setLines,
    policyYear,
    setPolicyYear,
    draft,
    save,
    submit,
    question,
    setQuestion,
    confirmQuestion,
  };
}

/** What {@link useRecordClaim} gives the page. */
export type RecordState = ReturnType<typeof useRecordClaim>;
