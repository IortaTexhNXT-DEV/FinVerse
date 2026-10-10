import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { attachmentsApi } from '@/api/attachments';
import { TSU_STATUS, renewalProposalApi } from '@/api/renewalProposal';
import type { TsuView } from '@/api/renewalProposal';
import { useAuth } from '@/auth/authContext';
import { InsurerSelect } from '@/components/broking/InsurerSelect';
import { useInsurerName } from '@/components/broking/useLabels';
import { useFileDownload } from '@/components/broking/useFileDownload';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';

type Run = (call: () => Promise<unknown>, done: string) => void;

function TlActions({ r, run }: Readonly<{ r: TsuView; run: Run }>) {
  const companyId = useCompanyId();
  const [remarks, setRemarks] = useState('');
  const [officer, setOfficer] = useState('');
  const officers = useQuery({
    queryKey: ['renewal', 'proposal-users', 'TSU_PROCESS'],
    queryFn: () => renewalProposalApi.users('TSU_PROCESS'),
  });
  if (r.status === 'APPROVED') {
    return (
      <div className="rnw-actions">
        <Field label="TSU Officer" required>
          {(id) => (
            <select
              id={id}
              className="select"
              value={officer}
              onChange={(e) => setOfficer(e.target.value)}
            >
              <option value="">Select…</option>
              {(officers.data ?? []).map((u) => (
                <option key={u.username} value={u.username}>
                  {u.displayName}
                </option>
              ))}
            </select>
          )}
        </Field>
        <Button
          disabled={officer === ''}
          onClick={() =>
            run(
              () => renewalProposalApi.assignTsu(companyId, r.requestNo, officer),
              'TSU Officer assigned',
            )
          }
        >
          Assign TSU Officer
        </Button>
      </div>
    );
  }
  if (r.status !== 'PENDING_TL_APPROVAL') {
    return null;
  }
  const decide = (decision: string, done: string) =>
    run(() => renewalProposalApi.decideTsu(companyId, r.requestNo, decision, remarks), done);
  return (
    <div className="rnw-actions">
      <Field label="Remarks">
        {(id) => (
          <input
            id={id}
            className="input"
            value={remarks}
            onChange={(e) => setRemarks(e.target.value)}
          />
        )}
      </Field>
      <Button onClick={() => decide('APPROVED', 'Request approved')}>Approve</Button>
      <Button
        variant="secondary"
        disabled={remarks.trim() === ''}
        onClick={() => decide('RETURNED_FOR_REVISION', 'Returned for revision')}
      >
        Return for Revision
      </Button>
      <Button
        variant="ghost"
        disabled={remarks.trim() === ''}
        onClick={() => decide('REJECTED', 'Request rejected')}
      >
        Reject
      </Button>
    </div>
  );
}

function TsuActions({ r, run }: Readonly<{ r: TsuView; run: Run }>) {
  const companyId = useCompanyId();
  const [insurer, setInsurer] = useState('');
  const [premium, setPremium] = useState('');
  const [terms, setTerms] = useState('');
  const [file, setFile] = useState<File | null>(null);
  if (r.status !== 'FOR_TSU_PROCESSING') {
    return null;
  }
  return (
    <div className="stack">
      <div className="rnw-actions">
        <Field label="Insurer">
          {(id) => <InsurerSelect id={id} value={insurer} onChange={setInsurer} />}
        </Field>
        <Field label="Premium">
          {(id) => (
            <input
              id={id}
              className="input"
              inputMode="decimal"
              value={premium}
              onChange={(e) => setPremium(e.target.value)}
            />
          )}
        </Field>
        <Field label="Terms and conditions">
          {(id) => (
            <input
              id={id}
              className="input"
              value={terms}
              onChange={(e) => setTerms(e.target.value)}
            />
          )}
        </Field>
        <Button
          variant="secondary"
          disabled={insurer === ''}
          onClick={() =>
            run(
              () =>
                renewalProposalApi.quoteTsu(companyId, r.requestNo, {
                  insurerCode: insurer,
                  premium: premium === '' ? null : Number(premium),
                  terms,
                }),
              'Quotation recorded',
            )
          }
        >
          Add to Comparative Table
        </Button>
      </div>
      <div className="rnw-actions">
        <Field label="Proposal">
          {(id) => (
            <input id={id} type="file" onChange={(e) => setFile(e.target.files?.[0] ?? null)} />
          )}
        </Field>
        <Button
          disabled={file === null}
          onClick={() => {
            if (file !== null) {
              run(
                () => renewalProposalApi.completeTsu(companyId, r.requestNo, file),
                'Proposal completed',
              );
            }
          }}
        >
          Complete with Proposal
        </Button>
      </div>
    </div>
  );
}

function QuotesTable({ r, run }: Readonly<{ r: TsuView; run: Run }>) {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const insurerName = useInsurerName();
  if (r.quotes.length === 0) {
    return null;
  }
  const choose = (code: string, checked: boolean) => {
    const chosen = r.quotes
      .filter((x) => (x.insurerCode === code ? checked : x.selected))
      .map((x) => x.insurerCode);
    run(() => renewalProposalApi.selectTsu(companyId, r.requestNo, chosen), 'Insurers selected');
  };
  return (
    <table className="table">
      <thead>
        <tr>
          <th>Insurer</th>
          <th>Premium</th>
          <th>Terms</th>
          <th>Selected</th>
        </tr>
      </thead>
      <tbody>
        {r.quotes.map((q) => (
          <tr key={q.insurerCode}>
            <td>{insurerName(q.insurerCode)}</td>
            <td>
              <Amount value={q.premium} />
            </td>
            <td>{q.terms ?? ''}</td>
            <td>
              <input
                type="checkbox"
                aria-label={`Select ${q.insurerCode}`}
                checked={q.selected}
                disabled={!can('RNW_DISPOSE')}
                onChange={(e) => choose(q.insurerCode, e.target.checked)}
              />
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

function OpenRequest({ r, run }: Readonly<{ r: TsuView; run: Run }>) {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const download = useFileDownload();
  const open = r;
  return (
    <div className="stack">
      <p>
        {open.requestNo}{' '}
        <StatusBadge status={open.status} label={TSU_STATUS[open.status] ?? open.status} full />{' '}
        {open.decisionRemarks ?? ''}
      </p>
      {open.status === 'RETURNED_FOR_REVISION' && can('RNW_DISPOSE') && (
        <Button
          onClick={() =>
            run(
              () => renewalProposalApi.resubmitTsu(companyId, open.requestNo),
              'Request resubmitted',
            )
          }
        >
          Resubmit
        </Button>
      )}
      {can('RNW_REVIEW') && <TlActions r={open} run={run} />}
      {(can('TSU_PROCESS') || can('RNW_TSU_QUEUE')) && <TsuActions r={open} run={run} />}
      <QuotesTable r={open} run={run} />
      {open.proposalAttachmentId !== null && (
        <Button
          variant="secondary"
          onClick={() =>
            download.mutate(() => attachmentsApi.download(open.proposalAttachmentId ?? 0))
          }
        >
          Download TSU Proposal
        </Button>
      )}
    </div>
  );
}

/**
 * The TSU request of an account For Quotation (FRRN.018.01): creation and submission for the Team
 * Lead's approval, the decision, the TSU Officer, the comparative table with the insurers chosen by
 * Marketing, and the proposal completed by TSU.
 */
export function TsuCard({
  renewalRef,
  forQuotation,
}: Readonly<{ renewalRef: string; forQuotation: boolean }>) {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [remarks, setRemarks] = useState('');
  const requests = useQuery({
    queryKey: ['renewal', 'tsu', companyId, renewalRef],
    queryFn: () => renewalProposalApi.tsuRequests(companyId, renewalRef),
  });
  const act = useMutation({
    mutationFn: (input: { call: () => Promise<unknown>; done: string }) => input.call(),
    onSuccess: async (_r, input) => {
      toast.success(input.done);
      await queryClient.invalidateQueries({ queryKey: ['renewal'] });
    },
  });
  const run: Run = (call, done) => act.mutate({ call, done });
  const open = (requests.data ?? []).find((r) => r.status !== 'REJECTED');
  return (
    <Card title="TSU Request">
      <ErrorAlert error={requests.error ?? act.error} />
      {open === undefined && can('RNW_DISPOSE') && (
        <div className="rnw-actions">
          <Field label="Remarks">
            {(id) => (
              <input
                id={id}
                className="input"
                value={remarks}
                onChange={(e) => setRemarks(e.target.value)}
              />
            )}
          </Field>
          <Button
            disabled={!forQuotation}
            onClick={() =>
              run(
                () => renewalProposalApi.createTsu(companyId, renewalRef, remarks),
                'Request submitted for the Team Lead approval',
              )
            }
          >
            Create TSU Request
          </Button>
        </div>
      )}
      {open !== undefined && <OpenRequest r={open} run={run} />}
    </Card>
  );
}
