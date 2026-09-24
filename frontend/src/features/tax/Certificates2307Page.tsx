import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { FileDown, FileStack } from 'lucide-react';
import { useState } from 'react';
import { saveFile } from '@/api/client';
import { taxApi } from '@/api/tax';
import type { Certificate, CertificateBatch } from '@/api/tax';
import { useAuth } from '@/auth/authContext';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDateTime, today } from '@/utils/format';
import { defaultChoice, quarterOf } from './taxPeriods';

/** BIR Form 2307: batch generation per quarter, the certificate register and PDF downloads. */
export default function Certificates2307Page() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [target, setTarget] = useState(() => defaultChoice(today(), 'QUARTER'));
  const certificates = useQuery({
    queryKey: ['tax-2307', companyId, target.year],
    queryFn: () => taxApi.certificates(companyId, target.year),
    enabled: companyId > 0,
  });
  const batches = useQuery({
    queryKey: ['tax-2307-batches', companyId],
    queryFn: () => taxApi.batches(companyId),
    enabled: companyId > 0,
  });
  const refresh = async () => {
    await queryClient.invalidateQueries({ queryKey: ['tax-2307'] });
    await queryClient.invalidateQueries({ queryKey: ['tax-2307-batches'] });
  };
  const generate = useMutation({
    mutationFn: () => taxApi.generateBatch(companyId, target.year, target.index),
    onSuccess: async (b) => {
      await refresh();
      const skipped =
        b.skippedPayees.length > 0 ? `; unmapped ATC: ${b.skippedPayees.join(', ')}` : '';
      toast.success(`${b.batchNo}: ${b.certificateCount} certificates issued${skipped}`);
    },
  });
  const download = useMutation({
    mutationFn: (req: { kind: 'certificate' | 'batch'; id: number }) =>
      req.kind === 'batch' ? taxApi.batchPdf(req.id) : taxApi.certificatePdf(req.id),
    onSuccess: ({ blob, fileName }) => saveFile(blob, fileName),
  });
  const cancel = useMutation({
    mutationFn: (id: number) => taxApi.cancelCertificate(id, 'Cancelled for re-issue'),
    onSuccess: async (c) => {
      await refresh();
      toast.success(`Certificate ${c.certificateNo} cancelled`);
    },
  });

  return (
    <div className="stack">
      <PageHeader
        section="Tax & Statutory"
        title="BIR Form 2307"
        description="Certificates of creditable tax withheld at source per payee and quarter, from the EWT worksheet."
      />
      <Card title="Generate certificates">
        <div className="row">
          <Field label="Year">
            {(id) => (
              <input
                id={id}
                className="input"
                type="number"
                value={target.year}
                onChange={(e) => setTarget({ ...target, year: Number(e.target.value) })}
              />
            )}
          </Field>
          <Field label="Quarter">
            {(id) => (
              <select
                id={id}
                className="select"
                value={target.index}
                onChange={(e) => setTarget({ ...target, index: Number(e.target.value) })}
              >
                {[1, 2, 3, 4].map((q) => (
                  <option key={q} value={q}>
                    Q{q}
                  </option>
                ))}
              </select>
            )}
          </Field>
          {can('TAX_MANAGE') && (
            <Button
              variant="accent"
              icon={<FileStack size={16} />}
              busy={generate.isPending}
              onClick={() => generate.mutate()}
            >
              Generate Batch
            </Button>
          )}
        </div>
      </Card>
      <ErrorAlert error={generate.error ?? download.error ?? cancel.error} />
      <Card title="Batches" flush>
        <DataTable<CertificateBatch>
          rows={batches.data ?? []}
          loading={batches.isLoading}
          rowKey={(b) => b.id}
          caption="Certificate batches"
          columns={[
            { key: 'n', header: 'Batch', render: (b) => <strong>{b.batchNo}</strong> },
            {
              key: 'q',
              header: 'Quarter',
              render: (b) => `${b.periodStart.slice(0, 4)}-Q${quarterOf(b.periodStart)}`,
            },
            { key: 'c', header: 'Certificates', numeric: true, render: (b) => b.certificateCount },
            {
              key: 't',
              header: 'Tax Withheld',
              numeric: true,
              render: (b) => <Amount value={b.totalTax} />,
            },
            {
              key: 'u',
              header: 'Issued',
              render: (b) => `${b.createdBy} ${formatDateTime(b.createdAt)}`,
            },
            {
              key: 'x',
              header: 'PDF',
              render: (b) => (
                <Button
                  size="sm"
                  variant="ghost"
                  icon={<FileDown size={14} />}
                  onClick={() => download.mutate({ kind: 'batch', id: b.id })}
                >
                  All
                </Button>
              ),
            },
          ]}
        />
      </Card>
      <Card title={`Certificate register ${target.year}`} flush>
        <DataTable<Certificate>
          rows={certificates.data ?? []}
          loading={certificates.isLoading}
          rowKey={(c) => c.id}
          caption="Certificate register"
          columns={[
            { key: 'n', header: 'Certificate', render: (c) => c.certificateNo },
            { key: 'q', header: 'Quarter', render: (c) => `Q${quarterOf(c.periodStart)}` },
            { key: 'p', header: 'Payee', render: (c) => c.payeeName },
            { key: 'tin', header: 'TIN', render: (c) => c.payeeTin },
            {
              key: 'i',
              header: 'Income',
              numeric: true,
              render: (c) => <Amount value={c.totalIncome} />,
            },
            {
              key: 't',
              header: 'Tax Withheld',
              numeric: true,
              render: (c) => <Amount value={c.totalTax} />,
            },
            { key: 's', header: 'Status', render: (c) => <StatusBadge status={c.status} /> },
            {
              key: 'x',
              header: 'Actions',
              render: (c) => (
                <div className="row">
                  <Button
                    size="sm"
                    variant="ghost"
                    icon={<FileDown size={14} />}
                    onClick={() => download.mutate({ kind: 'certificate', id: c.id })}
                  >
                    PDF
                  </Button>
                  {c.status === 'ISSUED' && can('TAX_MANAGE') && (
                    <Button size="sm" variant="ghost" onClick={() => cancel.mutate(c.id)}>
                      Cancel
                    </Button>
                  )}
                </div>
              ),
            },
          ]}
        />
      </Card>
    </div>
  );
}
