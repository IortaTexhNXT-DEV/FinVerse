import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
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
import { RefundLinesEditor } from './RefundLinesEditor';
import { emptyLine, refundErrors, refundInput } from './requestForm';
import type { FieldErrors, LineDraft, PayoutDraft } from './requestForm';

interface Header {
  segment: string;
  referenceText: string;
  requestingUnit: string;
  purpose: string;
}

const EMPTY_HEADER: Header = { segment: '', referenceText: '', requestingUnit: '', purpose: '' };
const EMPTY_PAYOUT: PayoutDraft = { paymentMode: '', accountNo: '', accountName: '' };

function fromRequest(r: PayRequest): { header: Header; payout: PayoutDraft; lines: LineDraft[] } {
  return {
    header: {
      segment: r.content.segment ?? '',
      referenceText: r.content.referenceText ?? '',
      requestingUnit: r.content.requestingUnit ?? '',
      purpose: r.content.purpose,
    },
    payout: {
      paymentMode: r.payee.mode,
      accountNo: r.payee.accountNo ?? '',
      accountName: r.payee.accountName ?? '',
    },
    lines: r.lines.map((l) => ({
      arNo: l.arNo,
      clientCode: l.clientCode,
      assuredName: l.assuredName,
      invoiceNo: l.invoiceNo ?? '',
      amount: String(l.amount),
      reasonCode: l.reasonCode,
      branchUnit: l.branchUnit ?? '',
      categoryA: l.categoryA ?? '',
      categoryB: l.categoryB ?? '',
    })),
  };
}

function HeaderFields({
  value,
  onChange,
}: Readonly<{ value: Header; onChange: (next: Header) => void }>) {
  const text = (key: keyof Header, label: string, max: number, hint?: string) => (
    <Field label={label} hint={hint}>
      {(id) => (
        <input
          id={id}
          className="input"
          maxLength={max}
          value={value[key]}
          onChange={(e) => onChange({ ...value, [key]: e.target.value })}
        />
      )}
    </Field>
  );
  return (
    <div className="form-grid">
      {text('segment', 'Segment', 40)}
      {text('referenceText', 'Reference', 80, 'e.g. 2025_331 Refund')}
      {text('requestingUnit', 'Requesting Unit', 60)}
      {text('purpose', 'Purpose / Remarks', 500)}
    </div>
  );
}

/**
 * Refund Request Form (RRF; MKT 1.10.0, Appendix D): segment and reference, the mode of payment
 * with the CA / SA information (prefilled from the client's known accounts, MKT 2.25.0), and the
 * accounts to refund. One live refund per AR number (MKT 2.23.0); a cancelled-policy refund is
 * validated by ACSL and Cashiering after submission (MKT 1.11.0).
 */
export default function RefundFormPage() {
  const companyId = useCompanyId();
  const navigate = useNavigate();
  const toast = useToast();
  const queryClient = useQueryClient();
  const idParam = useParams().id;
  const editId = idParam === undefined ? undefined : Number(idParam);
  const [header, setHeader] = useState<Header>(EMPTY_HEADER);
  const [payout, setPayout] = useState<PayoutDraft>(EMPTY_PAYOUT);
  const [lines, setLines] = useState<LineDraft[]>([emptyLine()]);
  const [errors, setErrors] = useState<FieldErrors>({});
  const existing = useQuery({
    queryKey: ['payrequest', 'request', editId],
    queryFn: () => payRequestApi.get(editId ?? 0),
    enabled: editId !== undefined,
  });
  const [loadedFrom, setLoadedFrom] = useState<unknown>();
  if (existing.data && existing.data !== loadedFrom) {
    setLoadedFrom(existing.data);
    const loaded = fromRequest(existing.data);
    setHeader(loaded.header);
    setPayout(loaded.payout);
    setLines(loaded.lines);
  }
  const client = lines[0]?.clientCode.trim() ?? '';
  const known = useQuery({
    queryKey: ['payrequest', 'payout', companyId, client],
    queryFn: () => payRequestApi.payoutAccounts(companyId, client),
    enabled: companyId > 0 && client !== '',
    retry: false,
  });
  const save = useMutation({
    mutationFn: () => {
      const input = refundInput(header, payout, lines);
      return editId === undefined
        ? payRequestApi.createRefund(companyId, input)
        : payRequestApi.updateRefund(editId, input);
    },
    onSuccess: async (r) => {
      await queryClient.invalidateQueries({ queryKey: ['payrequest'] });
      toast.success(`${r.requestNo} saved`);
      void navigate(`/payment-requests/requests/${String(r.id)}`);
    },
  });
  const submit = () => {
    const found = refundErrors(payout, lines);
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
        title={
          editId === undefined ? 'New Refund Request' : `Change ${existing.data?.requestNo ?? ''}`
        }
        description="Refund Request Form: the client's accounts to refund, the reason and how the refund is paid."
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
      <Card title="Request">
        <HeaderFields value={header} onChange={setHeader} />
      </Card>
      <Card title="Mode of Payment">
        <PayoutFields value={payout} errors={errors} onChange={setPayout} known={known.data} />
      </Card>
      <Card title="Accounts to Refund">
        <RefundLinesEditor lines={lines} errors={errors} onChange={setLines} />
      </Card>
    </div>
  );
}
