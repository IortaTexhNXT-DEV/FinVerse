import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { placementApi } from '@/api/placement';
import type { MatchStatus, PaymentReport, ReportLine } from '@/api/placement';
import { WorkTiles } from '@/components/broking/WorkTiles';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { formatAmount, formatDate } from '@/utils/format';
import { candidatesOf, linesOf, placementLink, REPORT_TABS } from './placementLogic';

function MatchDialog({
  report,
  line,
  onClose,
}: Readonly<{ report: PaymentReport; line: ReportLine; onClose: () => void }>) {
  const queryClient = useQueryClient();
  const candidates = candidatesOf(line);
  const [arn, setArn] = useState(candidates[0] ?? '');
  const match = useMutation({
    mutationFn: () => placementApi.matchLine(report.id, line.id, arn.trim()),
    onSuccess: (updated) => {
      queryClient.setQueryData(['placement', 'report', report.id], updated);
      onClose();
    },
  });
  return (
    <Modal
      open
      title={`Match Row ${line.rowNo} (${line.reference})`}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            variant="primary"
            busy={match.isPending}
            disabled={arn.trim() === ''}
            onClick={() => match.mutate()}
          >
            Match
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={match.error} />
        <Field label="Account (ARN)" required hint="An account of the company awaiting payment.">
          {(id) =>
            candidates.length > 0 ? (
              <select
                id={id}
                className="select"
                value={arn}
                onChange={(e) => setArn(e.target.value)}
              >
                {candidates.map((c) => (
                  <option key={c} value={c}>
                    {c}
                  </option>
                ))}
              </select>
            ) : (
              <input
                id={id}
                className="input"
                value={arn}
                onChange={(e) => setArn(e.target.value)}
              />
            )
          }
        </Field>
      </div>
    </Modal>
  );
}

function ReportActions({ report }: Readonly<{ report: PaymentReport }>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const done = (updated: PaymentReport, text: string) => {
    queryClient.setQueryData(['placement', 'report', report.id], updated);
    void queryClient.invalidateQueries({ queryKey: ['placement'] });
    toast.success(text);
  };
  const confirm = useMutation({
    mutationFn: () => placementApi.confirmReport(report.id),
    onSuccess: (r) =>
      done(
        r,
        `${r.reportNo} confirmed: ${r.lines.filter((l) => l.applied).length} payment gate(s) opened`,
      ),
  });
  const discard = useMutation({
    mutationFn: () => placementApi.discardReport(report.id),
    onSuccess: (r) => done(r, `${r.reportNo} discarded`),
  });
  if (report.status !== 'REVIEW') {
    return null;
  }
  return (
    <>
      <ErrorAlert error={confirm.error ?? discard.error} />
      <Button variant="secondary" busy={discard.isPending} onClick={() => discard.mutate()}>
        Discard
      </Button>
      <Button variant="primary" busy={confirm.isPending} onClick={() => confirm.mutate()}>
        Confirm Matches
      </Button>
    </>
  );
}

/**
 * Match review of a payment report (BRNB.067/068): lines matched, unpaid, unmatched and
 * ambiguous; an unmatched or ambiguous line can be matched to an account by hand. Confirming
 * opens the payment gate of every matched, paid account (each outcome shown on its line).
 */
export default function PaymentReportPage() {
  const id = Number(useParams().id);
  const [tab, setTab] = useState<MatchStatus>('MATCHED');
  const [matching, setMatching] = useState<ReportLine | null>(null);
  const report = useQuery({
    queryKey: ['placement', 'report', id],
    queryFn: () => placementApi.report(id),
  });
  if (report.data === undefined) {
    return report.error ? (
      <ErrorAlert error={report.error} />
    ) : (
      <span className="spinner" aria-label="Loading" />
    );
  }
  const r = report.data;
  const counts: Record<MatchStatus, number> = {
    MATCHED: r.matched,
    UNPAID: r.unpaid,
    UNMATCHED: r.unmatched,
    AMBIGUOUS: r.ambiguous,
  };
  const reviewable = r.status === 'REVIEW' && (tab === 'UNMATCHED' || tab === 'AMBIGUOUS');
  return (
    <div className="stack">
      <PageHeader
        backTo="/placement/billing"
        section="Placement & Booking · CLPC Billing"
        title={`Payment Report ${r.reportNo}`}
        description={`${r.kind === 'CLPC' ? 'CLPC report matched by PN / loan application no.' : 'Payment report matched by ARN'} · ${r.fileName}`}
        actions={
          <>
            <StatusBadge status={r.status} />
            <ReportActions report={r} />
          </>
        }
      />
      <WorkTiles
        label="Match outcome"
        tiles={REPORT_TABS.map((t) => ({
          key: t.id,
          label: t.label,
          value: counts[t.id],
          active: t.id === tab,
          alert: t.id === 'UNMATCHED' || t.id === 'AMBIGUOUS',
          onClick: () => setTab(t.id),
        }))}
      />
      <Card title={REPORT_TABS.find((t) => t.id === tab)?.label} flush>
        <DataTable<ReportLine>
          caption="Report lines"
          rows={linesOf(r.lines, tab)}
          rowKey={(l) => l.id}
          emptyMessage="No items to display"
          columns={[
            { key: 'row', header: 'Row', numeric: true, render: (l) => l.rowNo },
            { key: 'ref', header: 'Reported Reference', render: (l) => <code>{l.reference}</code> },
            { key: 'paid', header: 'Paid', render: (l) => (l.paid ? 'Yes' : 'No') },
            {
              key: 'amount',
              header: 'Amount',
              numeric: true,
              render: (l) => formatAmount(l.amount),
            },
            { key: 'date', header: 'Payment Date', render: (l) => formatDate(l.paidOn) },
            {
              key: 'arn',
              header: 'Account',
              render: (l) =>
                l.arn ? <Link to={placementLink(l.arn)}>{l.arn}</Link> : (l.candidates ?? '—'),
            },
            { key: 'message', header: 'Detail', render: (l) => l.applyMessage ?? l.message ?? '' },
            {
              key: 'act',
              header: '',
              render: (l) =>
                reviewable ? (
                  <Button size="sm" variant="secondary" onClick={() => setMatching(l)}>
                    Match
                  </Button>
                ) : null,
            },
          ]}
        />
      </Card>
      {matching && <MatchDialog report={r} line={matching} onClose={() => setMatching(null)} />}
    </div>
  );
}
