import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { CalendarX, Plus } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { underwritingApi } from '@/api/underwriting';
import type { Quotation, QuotationInput } from '@/api/underwriting';
import { useAuth } from '@/auth/authContext';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { useDefaultBranchId } from '@/context/workspaceContext';
import { formatDate, today } from '@/utils/format';
import { SelectField } from './FormFields';
import { newQuotation } from './policyForm';
import { QuotationForm } from './QuotationForm';
import { useUwLookups } from './useUwLookups';

const STATUSES = ['DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'REJECTED', 'CONVERTED', 'EXPIRED'].map(
  (s) => ({ value: s, label: s.replace('_', ' ') }),
);

function latest(q: Quotation) {
  return q.iterations[q.iterations.length - 1];
}

/** Quotations: list by status, create with a first iteration, open one to negotiate or convert. */
export default function QuotationsPage() {
  const lookups = useUwLookups();
  const defaultBranch = useDefaultBranchId();
  const { can } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [status, setStatus] = useState('');
  const [form, setForm] = useState<QuotationInput | null>(null);
  const companyId = lookups.companyId;

  const query = useQuery({
    queryKey: ['quotations', companyId, status],
    queryFn: () => underwritingApi.quotations(companyId, status === '' ? undefined : status),
    enabled: companyId > 0,
  });
  const create = useMutation({
    mutationFn: (body: QuotationInput) => underwritingApi.createQuotation(body),
    onSuccess: async (q) => {
      await queryClient.invalidateQueries({ queryKey: ['quotations'] });
      setForm(null);
      toast.success(`Quotation ${q.quotationNo} created`);
      await navigate(`/underwriting/quotations/${String(q.id)}`);
    },
  });
  const expire = useMutation({
    mutationFn: () => underwritingApi.expireQuotations(companyId),
    onSuccess: async ({ expired }) => {
      await queryClient.invalidateQueries({ queryKey: ['quotations'] });
      toast.success(
        expired === 0
          ? 'No lapsed quotations to expire'
          : `${String(expired)} lapsed quotation(s) expired`,
      );
    },
  });

  return (
    <div className="stack">
      <PageHeader
        section="Underwriting"
        title="Quotations"
        description="Offers to prospective clients with negotiation iterations, approval and conversion into a policy."
        actions={
          can('POLICY_MAINTAIN') && (
            <>
              <Button
                variant="secondary"
                icon={<CalendarX size={16} />}
                busy={expire.isPending}
                disabled={companyId <= 0}
                onClick={() => expire.mutate()}
              >
                Expire lapsed quotations
              </Button>
              <Button
                variant="accent"
                icon={<Plus size={16} />}
                onClick={() => setForm(newQuotation(companyId, defaultBranch, today()))}
              >
                New quotation
              </Button>
            </>
          )
        }
      />
      <Card>
        <div className="form-grid">
          <SelectField
            label="Status"
            value={status}
            emptyLabel="All"
            options={STATUSES}
            onChange={setStatus}
          />
        </div>
      </Card>
      <ErrorAlert error={query.error ?? expire.error} />
      <Card flush>
        <DataTable<Quotation>
          loading={query.isLoading}
          rows={query.data ?? []}
          rowKey={(q) => q.id}
          onRowClick={(q) => void navigate(`/underwriting/quotations/${String(q.id)}`)}
          caption="Quotations"
          columns={[
            { key: 'no', header: 'Quotation', render: (q) => <strong>{q.quotationNo}</strong> },
            { key: 'it', header: 'Iteration', numeric: true, render: (q) => q.currentIteration },
            { key: 'prod', header: 'Product', render: (q) => q.productCode },
            { key: 'ins', header: 'Insured', render: (q) => q.insuredName },
            { key: 'iss', header: 'Issued', render: (q) => formatDate(q.issueDate) },
            { key: 'exp', header: 'Valid until', render: (q) => formatDate(q.expiryDate) },
            {
              key: 'si',
              header: 'Sum insured',
              numeric: true,
              render: (q) => <Amount value={latest(q)?.sumInsured} />,
            },
            {
              key: 'net',
              header: 'Our net premium',
              numeric: true,
              render: (q) => <Amount value={latest(q)?.ourNetPremium} />,
            },
            { key: 'st', header: 'Status', render: (q) => <StatusBadge status={q.status} /> },
          ]}
        />
      </Card>
      <Modal
        title="New quotation"
        open={form !== null}
        onClose={() => setForm(null)}
        footer={
          <Button
            variant="accent"
            busy={create.isPending}
            onClick={() => form && create.mutate(form)}
          >
            Create quotation
          </Button>
        }
      >
        <ErrorAlert error={create.error} />
        {form !== null && <QuotationForm form={form} onChange={setForm} />}
      </Modal>
    </div>
  );
}
