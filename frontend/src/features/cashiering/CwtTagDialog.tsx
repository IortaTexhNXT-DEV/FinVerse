import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Modal } from '@/components/ui/Modal';
import { useToast } from '@/components/ui/toastContext';
import { humanize } from '@/utils/format';
import { CodeSelect, TextField } from './CashFields';
import { cashieringApi } from './cashieringApi';
import type { CwtTagBody } from './cashieringApi';
import { positive } from './cashieringLogic';
import { useDebounced } from './useDebounced';

type TagForm = Omit<CwtTagBody, 'companyId' | 'amount'> & { amount: string };

function tagErrors(f: TagForm): Record<string, string> {
  const e: Record<string, string> = {};
  if (f.invoiceNo.trim() === '') {
    e.invoiceNo = 'Invoice number is required';
  }
  if (f.path === 'CERTIFICATE' && (f.certificateNo ?? '').trim() === '') {
    e.certificateNo = 'Certificate number is required for a certificate';
  }
  if (f.amount !== '' && !positive(f.amount)) {
    e.amount = 'Enter an amount above zero, or leave it empty for the expected 2%';
  }
  return e;
}

/**
 * Marketing tags the BIR 2307 of a client (MKTID.010/013, CSHID.026): the certificate it holds
 * or the cash the client pays instead, for the 2% the client withheld on the invoice.
 */
export function CwtTagDialog({
  companyId,
  onClose,
}: Readonly<{ companyId: number; onClose: () => void }>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [f, setF] = useState<TagForm>({ invoiceNo: '', amount: '', path: 'CERTIFICATE' });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const invoiceNo = useDebounced(f.invoiceNo.trim());
  const expected = useQuery({
    queryKey: ['cashiering', 'cwt-expected', invoiceNo],
    queryFn: () => cashieringApi.cwtExpected(invoiceNo),
    enabled: invoiceNo.length > 3,
    retry: false,
  });
  const tag = useMutation({
    mutationFn: () =>
      cashieringApi.tagCwt({
        ...f,
        companyId,
        amount: f.amount === '' ? undefined : Number(f.amount),
      }),
    onSuccess: async (t) => {
      toast.success(`${t.reference} tagged`);
      await queryClient.invalidateQueries({ queryKey: ['cashiering', 'cwt'] });
      onClose();
    },
  });
  const set = (k: keyof TagForm) => (v: string) => setF((x) => ({ ...x, [k]: v }));
  const save = () => {
    const found = tagErrors(f);
    setErrors(found);
    if (Object.keys(found).length === 0) {
      tag.mutate();
    }
  };
  const e = expected.data;
  return (
    <Modal
      open
      title="Tag BIR 2307"
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button busy={tag.isPending} onClick={save}>
            Tag 2307
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={tag.error} />
        <div className="form-grid">
          <TextField
            label="Invoice No."
            required
            value={f.invoiceNo}
            onChange={set('invoiceNo')}
            error={errors.invoiceNo}
            maxLength={40}
          />
          <CodeSelect
            label="Path"
            required
            value={f.path}
            options={['CERTIFICATE', 'CASH']}
            onChange={set('path')}
          />
          <TextField
            label="Amount"
            type="number"
            value={f.amount}
            onChange={set('amount')}
            error={errors.amount}
            hint="Empty for the expected 2%"
          />
          <TextField
            label="Certificate No."
            required={f.path === 'CERTIFICATE'}
            value={f.certificateNo ?? ''}
            onChange={set('certificateNo')}
            error={errors.certificateNo}
            maxLength={60}
          />
          <TextField
            label="Period From"
            type="date"
            value={f.periodFrom ?? ''}
            onChange={set('periodFrom')}
          />
          <TextField
            label="Period To"
            type="date"
            value={f.periodTo ?? ''}
            onChange={set('periodTo')}
          />
          <TextField
            label="Remarks"
            value={f.remarks ?? ''}
            onChange={set('remarks')}
            maxLength={250}
          />
        </div>
        {e && (
          <p className="muted">
            {e.assuredName ?? e.arn} · insurer {e.insurerCode ?? ''} · expected 2307{' '}
            <Amount value={e.expected} /> ·{' '}
            {e.cwt ? '2% CWT client' : 'not flagged as a CWT client'} ·{' '}
            {humanize(e.remittanceStatus)}
          </p>
        )}
      </div>
    </Modal>
  );
}
