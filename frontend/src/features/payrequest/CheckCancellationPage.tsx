import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { LovSelect } from '@/components/broking/LovSelect';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { payRequestApi } from './api';
import type { FieldErrors } from './requestForm';

/**
 * Cancellation of a disbursed check (MKT 1.19.0): the refund or cash advance paid by check, the
 * check number and the reason; it is reviewed and approved, then handed to Disbursement, which
 * cancels the approved DV (DIS 2.20.0).
 */
export default function CheckCancellationPage() {
  const companyId = useCompanyId();
  const navigate = useNavigate();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [params] = useSearchParams();
  const [targetRequestNo, setTarget] = useState(params.get('request') ?? '');
  const [checkNo, setCheckNo] = useState('');
  const [reasonCode, setReason] = useState('');
  const [remarks, setRemarks] = useState('');
  const [errors, setErrors] = useState<FieldErrors>({});
  const save = useMutation({
    mutationFn: () =>
      payRequestApi.createCheckCancellation(companyId, {
        targetRequestNo: targetRequestNo.trim(),
        checkNo: checkNo.trim() || undefined,
        reasonCode,
        remarks: remarks.trim() || undefined,
      }),
    onSuccess: async (r) => {
      await queryClient.invalidateQueries({ queryKey: ['payrequest'] });
      toast.success(`${r.requestNo} raised for ${r.target.requestNo ?? ''}`);
      void navigate(`/payment-requests/requests/${String(r.id)}`);
    },
  });
  const submit = () => {
    const found: FieldErrors = {};
    if (targetRequestNo.trim() === '') {
      found.targetRequestNo = 'Enter the request that was paid by check';
    }
    if (reasonCode === '') {
      found.reasonCode = 'Select the reason';
    }
    setErrors(found);
    if (Object.keys(found).length === 0) {
      save.mutate();
    }
  };
  return (
    <div className="stack">
      <PageHeader
        backTo="/payment-requests"
        section="Finance · Refund & Cash Advance Requests"
        title="Cancel a Disbursed Check"
        description="Ask Disbursement to cancel the check of a paid refund or cash advance, after review and approval."
        actions={
          <>
            <Button variant="secondary" onClick={() => void navigate('/payment-requests')}>
              Cancel
            </Button>
            <Button variant="accent" busy={save.isPending} onClick={submit}>
              Raise Cancellation
            </Button>
          </>
        }
      />
      <ErrorAlert error={save.error} />
      <Card title="Check to Cancel">
        <div className="form-grid">
          <Field
            label="Paid Request No."
            required
            error={errors.targetRequestNo}
            hint="RRF- or RFP- number whose DV was paid by check"
          >
            {(id) => (
              <input
                id={id}
                className="input"
                maxLength={30}
                aria-invalid={errors.targetRequestNo !== undefined}
                value={targetRequestNo}
                onChange={(e) => setTarget(e.target.value)}
              />
            )}
          </Field>
          <Field label="Check No.">
            {(id) => (
              <input
                id={id}
                className="input"
                maxLength={40}
                value={checkNo}
                onChange={(e) => setCheckNo(e.target.value)}
              />
            )}
          </Field>
          <Field label="Reason" required error={errors.reasonCode}>
            {(id) => (
              <LovSelect
                id={id}
                type="DISB_CANCEL_REASON"
                value={reasonCode}
                onChange={setReason}
              />
            )}
          </Field>
          <Field label="Remarks">
            {(id) => (
              <input
                id={id}
                className="input"
                maxLength={500}
                value={remarks}
                onChange={(e) => setRemarks(e.target.value)}
              />
            )}
          </Field>
        </div>
      </Card>
    </div>
  );
}
