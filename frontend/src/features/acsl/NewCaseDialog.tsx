import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { CASE_TYPE_LABELS } from './acsl';
import { acslApi } from './api';
import type { CaseType } from './api';

/** Opens an ACSL case on an invoice (ACSL 2.5.0 investigation, 2.5.5 analysis, 2.6.x). */
export function NewCaseDialog({ onClose }: Readonly<{ onClose: () => void }>) {
  const companyId = useCompanyId();
  const navigate = useNavigate();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [type, setType] = useState<CaseType>('INVESTIGATION');
  const [invoiceNo, setInvoiceNo] = useState('');
  const [arNo, setArNo] = useState('');
  const [subject, setSubject] = useState('');
  const [details, setDetails] = useState('');
  const [error, setError] = useState<string>();
  const open = useMutation({
    mutationFn: () =>
      acslApi.openCase(companyId, {
        type,
        invoiceNo: invoiceNo.trim() || undefined,
        arNo: arNo.trim() || undefined,
        subject: subject.trim(),
        details: details.trim() || undefined,
      }),
    onSuccess: async (c) => {
      await queryClient.invalidateQueries({ queryKey: ['acsl'] });
      toast.success(`${c.caseNo} opened`);
      void navigate(`/acsl/cases/${String(c.id)}`);
    },
  });
  const submit = () => {
    if (subject.trim() === '') {
      setError('Enter the subject of the case');
      return;
    }
    setError(undefined);
    open.mutate();
  };
  return (
    <Modal
      open
      title="Open a Case"
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button variant="accent" busy={open.isPending} onClick={submit}>
            Open Case
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={open.error} />
        <Field label="Case Type" required>
          {(id) => (
            <select
              id={id}
              className="select"
              value={type}
              onChange={(e) => setType(e.target.value as CaseType)}
            >
              {(Object.keys(CASE_TYPE_LABELS) as CaseType[]).map((t) => (
                <option key={t} value={t}>
                  {CASE_TYPE_LABELS[t]}
                </option>
              ))}
            </select>
          )}
        </Field>
        <Field label="Invoice No." hint="The account investigated; its whole family is linked">
          {(id) => (
            <input
              id={id}
              className="input"
              value={invoiceNo}
              onChange={(e) => setInvoiceNo(e.target.value)}
            />
          )}
        </Field>
        <Field label="AR / OR No.">
          {(id) => (
            <input
              id={id}
              className="input"
              value={arNo}
              onChange={(e) => setArNo(e.target.value)}
            />
          )}
        </Field>
        <Field label="Subject" required error={error}>
          {(id) => (
            <input
              id={id}
              className="input"
              maxLength={250}
              aria-invalid={error !== undefined}
              value={subject}
              onChange={(e) => setSubject(e.target.value)}
            />
          )}
        </Field>
        <Field label="Details">
          {(id) => (
            <textarea
              id={id}
              className="textarea"
              rows={3}
              value={details}
              onChange={(e) => setDetails(e.target.value)}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}
