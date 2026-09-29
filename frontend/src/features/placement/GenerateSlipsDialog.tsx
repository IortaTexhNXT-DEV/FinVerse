import { useMutation, useQuery } from '@tanstack/react-query';
import { placementApi } from '@/api/placement';
import type { Readiness, Slip } from '@/api/placement';
import { Button } from '@/components/ui/Button';
import { DataTable } from '@/components/ui/DataTable';
import { countOf, humanize } from '@/utils/format';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Modal } from '@/components/ui/Modal';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { InsurerName } from '@/components/broking/LovLabel';

/**
 * "For Placement" (BRNB.069): shows every prerequisite of the selected accounts (payment
 * confirmed, documents complete, TSU cleared, insurer reachable) and generates one placement slip
 * per insurer branch when all are met.
 */
export function GenerateSlipsDialog({
  companyId,
  arns,
  onDone,
  onClose,
}: Readonly<{
  companyId: number;
  arns: string[];
  onDone: (slips: Slip[]) => void;
  onClose: () => void;
}>) {
  const readiness = useQuery({
    queryKey: ['placement', 'readiness', companyId, arns],
    queryFn: () => placementApi.readiness(companyId, arns),
  });
  const generate = useMutation({
    mutationFn: () => placementApi.generateSlips(companyId, arns),
    onSuccess: onDone,
  });
  const rows = readiness.data ?? [];
  const allReady = rows.length > 0 && rows.every((r) => r.ready);
  const missing = rows.flatMap((r) =>
    r.unmet.map((u) => ({ key: `${r.arn}:${u.code}`, arn: r.arn, ...u })),
  );
  return (
    <Modal
      open
      title="Generate Placement Slips"
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            variant="primary"
            busy={generate.isPending}
            disabled={!allReady}
            onClick={() => generate.mutate()}
          >
            Generate Slips
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={readiness.error ?? generate.error} />
        <p className="muted">
          One slip is generated per insurer branch, as PDF and Excel, from the placement slip
          template.
        </p>
        <DataTable<Readiness>
          caption="Placement prerequisites"
          loading={readiness.isLoading}
          rows={rows}
          rowKey={(r) => r.arn}
          columns={[
            {
              key: 'arn',
              header: 'ARN',
              kind: 'code',
              render: (r) => (
                <span>
                  <code>{r.arn}</code>
                  <span className="cell-sub">{r.clientName}</span>
                </span>
              ),
            },
            {
              key: 'insurer',
              header: 'Insurer',
              render: (r) => (
                <>
                  <InsurerName code={r.insurerCode} />
                  {r.insurerBranch ? ` / ${r.insurerBranch}` : ''}
                </>
              ),
            },
            {
              key: 'ready',
              header: 'Prerequisites',
              kind: 'status',
              render: (r) =>
                r.ready ? (
                  <StatusBadge status="VALID" label="Ready" />
                ) : (
                  <StatusBadge status="INVALID" label={countOf(r.unmet.length, 'item')} />
                ),
            },
          ]}
        />
        {missing.length > 0 && (
          <DataTable<(typeof missing)[number]>
            caption="What is still missing"
            rows={missing}
            rowKey={(m) => m.key}
            columns={[
              { key: 'arn', header: 'ARN', kind: 'code', render: (m) => <code>{m.arn}</code> },
              { key: 'check', header: 'Check', render: (m) => humanize(m.code) },
              { key: 'what', header: 'What Is Missing', render: (m) => m.message },
            ]}
          />
        )}
      </div>
    </Modal>
  );
}
