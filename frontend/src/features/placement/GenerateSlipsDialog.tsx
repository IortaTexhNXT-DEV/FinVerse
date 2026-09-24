import { useMutation, useQuery } from '@tanstack/react-query';
import { placementApi } from '@/api/placement';
import type { Readiness, Slip } from '@/api/placement';
import { Button } from '@/components/ui/Button';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Modal } from '@/components/ui/Modal';
import { StatusBadge } from '@/components/ui/StatusBadge';

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
              header: 'Proposal No.',
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
              render: (r) => [r.insurerCode, r.insurerBranch].filter(Boolean).join(' / ') || '—',
            },
            {
              key: 'ready',
              header: 'Prerequisites',
              render: (r) =>
                r.ready ? (
                  <StatusBadge status="VALID" />
                ) : (
                  <ul className="unmet-list">
                    {r.unmet.map((u) => (
                      <li key={u.code}>{u.message}</li>
                    ))}
                  </ul>
                ),
            },
          ]}
        />
      </div>
    </Modal>
  );
}
