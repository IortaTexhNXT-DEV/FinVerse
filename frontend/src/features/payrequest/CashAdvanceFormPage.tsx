import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '@/auth/authContext';
import { LovSelect } from '@/components/broking/LovSelect';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { payRequestApi } from './api';
import type { PayRequest } from './api';
import { PayoutFields } from './PayoutFields';
import { cashAdvanceErrors, cashAdvanceInput } from './requestForm';
import type { CashAdvanceDraft, FieldErrors } from './requestForm';

function emptyDraft(username: string, fullName: string): CashAdvanceDraft {
  return {
    paymentMode: '',
    accountNo: '',
    accountName: fullName,
    employeeNo: username.toUpperCase(),
    employeeName: fullName,
    rfpType: 'CASH_ADVANCE',
    purpose: '',
    amount: '',
    segment: '',
    requestingUnit: '',
  };
}

function fromRequest(r: PayRequest): CashAdvanceDraft {
  return {
    paymentMode: r.payee.mode,
    accountNo: r.payee.accountNo ?? '',
    accountName: r.payee.accountName ?? '',
    employeeNo: r.payee.code,
    employeeName: r.payee.name,
    rfpType: r.content.rfpType ?? 'CASH_ADVANCE',
    purpose: r.content.purpose,
    amount: String(r.amount),
    segment: r.content.segment ?? '',
    requestingUnit: r.content.requestingUnit ?? '',
  };
}

type TextKey = 'employeeNo' | 'employeeName' | 'purpose' | 'amount' | 'segment' | 'requestingUnit';

/**
 * Request for Payment of an employee cash advance (RFP; MKT 1.10.0, Appendix D): employee, type,
 * purpose, amount and mode of payment. After the Marketing approval it goes to HR, then to
 * Disbursement (MKT 1.16.2, 2.24.0); once paid it is liquidated (AQ18).
 */
export default function CashAdvanceFormPage() {
  const companyId = useCompanyId();
  const navigate = useNavigate();
  const toast = useToast();
  const queryClient = useQueryClient();
  const { user } = useAuth();
  const idParam = useParams().id;
  const editId = idParam === undefined ? undefined : Number(idParam);
  const [draft, setDraft] = useState<CashAdvanceDraft>(() =>
    emptyDraft(user?.username ?? '', user?.fullName ?? ''),
  );
  const [errors, setErrors] = useState<FieldErrors>({});
  const existing = useQuery({
    queryKey: ['payrequest', 'request', editId],
    queryFn: () => payRequestApi.get(editId ?? 0),
    enabled: editId !== undefined,
  });
  const [loadedFrom, setLoadedFrom] = useState<unknown>();
  if (existing.data && existing.data !== loadedFrom) {
    setLoadedFrom(existing.data);
    setDraft(fromRequest(existing.data));
  }
  const save = useMutation({
    mutationFn: () =>
      editId === undefined
        ? payRequestApi.createCashAdvance(companyId, cashAdvanceInput(draft))
        : payRequestApi.updateCashAdvance(editId, cashAdvanceInput(draft)),
    onSuccess: async (r) => {
      await queryClient.invalidateQueries({ queryKey: ['payrequest'] });
      toast.success(`${r.requestNo} saved`);
      void navigate(`/payment-requests/requests/${String(r.id)}`);
    },
  });
  const submit = () => {
    const found = cashAdvanceErrors(draft);
    setErrors(found);
    if (Object.keys(found).length === 0) {
      save.mutate();
    }
  };
  const text = (key: TextKey, label: string, required: boolean, max: number) => (
    <Field label={label} required={required} error={errors[key]}>
      {(id) => (
        <input
          id={id}
          className="input"
          maxLength={max}
          inputMode={key === 'amount' ? 'decimal' : undefined}
          aria-invalid={errors[key] !== undefined}
          value={draft[key]}
          onChange={(e) => setDraft({ ...draft, [key]: e.target.value })}
        />
      )}
    </Field>
  );
  return (
    <div className="stack">
      <PageHeader
        backTo="/payment-requests"
        section="Finance · Refund & Cash Advance Requests"
        title={
          editId === undefined ? 'New Cash Advance' : `Change ${existing.data?.requestNo ?? ''}`
        }
        description="Request for Payment of an employee cash advance, approved by Marketing and HR before Disbursement pays it."
        actions={
          <>
            <Button variant="secondary" onClick={() => void navigate('/payment-requests')}>
              Cancel
            </Button>
            <Button variant="accent" busy={save.isPending} onClick={submit}>
              Save Request
            </Button>
          </>
        }
      />
      <ErrorAlert error={existing.error ?? save.error} />
      <Card title="Request for Payment">
        <div className="form-grid">
          {text('employeeNo', 'Employee No.', true, 30)}
          {text('employeeName', 'Employee Name', true, 250)}
          <Field label="Type">
            {(id) => (
              <LovSelect
                id={id}
                type="PRQ_RFP_TYPE"
                value={draft.rfpType}
                onChange={(code) => setDraft({ ...draft, rfpType: code })}
              />
            )}
          </Field>
          {text('amount', 'Amount (PHP)', true, 18)}
          {text('purpose', 'Purpose', true, 500)}
          {text('segment', 'Segment', false, 40)}
          {text('requestingUnit', 'Requesting Unit', false, 60)}
        </div>
      </Card>
      <Card title="Mode of Payment">
        <PayoutFields
          value={draft}
          errors={errors}
          onChange={(p) => setDraft({ ...draft, ...p })}
        />
      </Card>
    </div>
  );
}
