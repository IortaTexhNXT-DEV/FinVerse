import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { CLIENT_STATUS, STATUS_LABELS, renewalApprovalApi } from '@/api/renewalApproval';
import type { ApprovalState } from '@/api/renewalApproval';
import type { CandidateDetail } from '@/api/renewal';
import { useAuth } from '@/auth/authContext';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDateTime } from '@/utils/format';

type Run = (call: () => Promise<unknown>, done: string) => void;

function label(code: string | null | undefined): string {
  return code === null || code === undefined ? '' : (STATUS_LABELS[code] ?? code);
}

function ClientCard({
  state,
  run,
}: Readonly<{ state: ApprovalState; run: (status: string, remarks: string) => void }>) {
  const { can } = useAuth();
  const [status, setStatus] = useState(state.client.status ?? 'PENDING');
  const [remarks, setRemarks] = useState('');
  const needs = status === 'REJECTED' || status === 'REVISION';
  return (
    <Card title="Client Acceptance Status">
      <p>
        <StatusBadge
          status={CLIENT_STATUS[state.client.status ?? ''] ?? 'Not recorded'}
          label={CLIENT_STATUS[state.client.status ?? ''] ?? 'Not recorded'}
          full
        />{' '}
        {state.client.remarks ?? ''}
        {state.client.at !== null && (
          <span className="muted"> · {formatDateTime(state.client.at)}</span>
        )}
      </p>
      {(can('RNW_DISPOSE') || can('RNW_ACCEPT')) && (
        <div className="rnw-actions">
          <Field label="Status" required>
            {(id) => (
              <select
                id={id}
                className="select"
                value={status}
                onChange={(e) => setStatus(e.target.value)}
              >
                {Object.entries(CLIENT_STATUS).map(([code, text]) => (
                  <option key={code} value={code}>
                    {text}
                  </option>
                ))}
              </select>
            )}
          </Field>
          <Field label="Remarks" required={needs}>
            {(id) => (
              <input
                id={id}
                className="input"
                value={remarks}
                onChange={(e) => setRemarks(e.target.value)}
              />
            )}
          </Field>
          <Button disabled={needs && remarks.trim() === ''} onClick={() => run(status, remarks)}>
            Save Status
          </Button>
        </div>
      )}
    </Card>
  );
}

function BookingOnlyCard({
  state,
  renewalRef,
  run,
}: Readonly<{ state: ApprovalState; renewalRef: string; run: Run }>) {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const b = state.bookingOnly;
  const [policy, setPolicy] = useState(b.policyNo ?? '');
  const [or, setOr] = useState(b.orNo ?? '');
  const [reason, setReason] = useState('');
  return (
    <Card title="For Booking Only">
      <label className="checkbox">
        <input
          type="checkbox"
          checked={b.tagged}
          disabled={!can('RNW_DISPOSE')}
          onChange={(e) =>
            run(
              () =>
                renewalApprovalApi.bookingOnlyDetails(companyId, renewalRef, {
                  tagged: e.target.checked,
                  policyNo: policy,
                  orNo: or,
                }),
              e.target.checked ? 'Tagged For Booking Only' : 'For Booking Only tag removed',
            )
          }
        />{' '}
        For Booking Only
      </label>
      {b.tagged && (
        <div className="stack">
          <div className="rnw-actions">
            <Field label="Policy Number">
              {(id) => (
                <input
                  id={id}
                  className="input"
                  value={policy}
                  onChange={(e) => setPolicy(e.target.value)}
                />
              )}
            </Field>
            <Field label="OR Number">
              {(id) => (
                <input
                  id={id}
                  className="input"
                  value={or}
                  onChange={(e) => setOr(e.target.value)}
                />
              )}
            </Field>
            <Button
              variant="secondary"
              onClick={() =>
                run(
                  () =>
                    renewalApprovalApi.bookingOnlyDetails(companyId, renewalRef, {
                      tagged: true,
                      policyNo: policy,
                      orNo: or,
                    }),
                  'Booking details saved',
                )
              }
            >
              Save Details
            </Button>
          </div>
          {b.missing.length > 0 && (
            <p className="rnw-note">Missing before the submission: {b.missing.join(', ')}</p>
          )}
          <p>
            Override:{' '}
            <StatusBadge
              status={label(b.override) || 'None'}
              label={label(b.override) || 'None'}
              full
            />{' '}
            {b.overrideReason ?? ''}
          </p>
          <div className="rnw-actions">
            <Field label="Override reason or justification">
              {(id) => (
                <input
                  id={id}
                  className="input"
                  value={reason}
                  onChange={(e) => setReason(e.target.value)}
                />
              )}
            </Field>
            {b.missing.length > 0 && b.override !== 'REQUESTED' && (
              <Button
                variant="secondary"
                disabled={reason.trim() === ''}
                onClick={() =>
                  run(
                    () => renewalApprovalApi.requestOverride(companyId, renewalRef, reason),
                    'Override requested',
                  )
                }
              >
                Request Override
              </Button>
            )}
            {b.override === 'REQUESTED' && can('RNW_OVERRIDE') && (
              <>
                <Button
                  disabled={reason.trim() === ''}
                  onClick={() =>
                    run(
                      () => renewalApprovalApi.decideOverride(companyId, renewalRef, true, reason),
                      'Override approved',
                    )
                  }
                >
                  Approve Override
                </Button>
                <Button
                  variant="secondary"
                  disabled={reason.trim() === ''}
                  onClick={() =>
                    run(
                      () => renewalApprovalApi.decideOverride(companyId, renewalRef, false, reason),
                      'Override rejected',
                    )
                  }
                >
                  Reject Override
                </Button>
              </>
            )}
          </div>
        </div>
      )}
    </Card>
  );
}

function DirectToInsurerCard({
  state,
  renewalRef,
  run,
}: Readonly<{ state: ApprovalState; renewalRef: string; run: Run }>) {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const d = state.directToInsurer;
  const [reason, setReason] = useState('');
  return (
    <Card title="Direct-to-Insurer Payment">
      <div className="rnw-actions">
        <Field label="Option">
          {(id) => (
            <select
              id={id}
              className="select"
              value={d.option ?? ''}
              disabled={!can('RNW_DISPOSE')}
              onChange={(e) =>
                run(
                  () =>
                    renewalApprovalApi.tagDirectToInsurer(
                      companyId,
                      renewalRef,
                      e.target.value === '' ? null : e.target.value,
                    ),
                  'Direct-to-Insurer Payment updated',
                )
              }
            >
              <option value="">Not tagged</option>
              <option value="BLANKET">With Blanket Approval</option>
              <option value="UNIT_HEAD">Route for UH Approval</option>
            </select>
          )}
        </Field>
        {d.status !== null && <StatusBadge status={label(d.status)} label={label(d.status)} full />}
        {d.reason !== null && <span>{d.reason}</span>}
      </div>
      {d.status === 'PENDING' && can('RNW_REVIEW') && (
        <div className="rnw-actions">
          <Field label="Rejection reason">
            {(id) => (
              <input
                id={id}
                className="input"
                value={reason}
                onChange={(e) => setReason(e.target.value)}
              />
            )}
          </Field>
          <Button
            onClick={() =>
              run(
                () => renewalApprovalApi.decideDirectToInsurer(companyId, renewalRef, true, reason),
                'Approved',
              )
            }
          >
            Approve
          </Button>
          <Button
            variant="secondary"
            disabled={reason.trim() === ''}
            onClick={() =>
              run(
                () =>
                  renewalApprovalApi.decideDirectToInsurer(companyId, renewalRef, false, reason),
                'Rejected',
              )
            }
          >
            Reject
          </Button>
        </div>
      )}
    </Card>
  );
}

/**
 * The Acceptance & Approval tab of a renewal account (FRRN.014.04, FRRN.014.05, FRRN.025.01,
 * FRRN.26.01, FRRN.028.01): the client acceptance status, the second approval, the payment status,
 * the For Booking Only tag with its override and the Direct-to-Insurer Payment tag.
 */
export function ApprovalTab({ detail }: Readonly<{ detail: CandidateDetail }>) {
  const ref = detail.row.renewalRef;
  const companyId = useCompanyId();
  const toast = useToast();
  const { can } = useAuth();
  const queryClient = useQueryClient();
  const state = useQuery({
    queryKey: ['renewal', 'approval', companyId, ref],
    queryFn: () => renewalApprovalApi.state(companyId, ref),
  });
  const act = useMutation({
    mutationFn: (input: { call: () => Promise<unknown>; done: string }) => input.call(),
    onSuccess: async (_r, input) => {
      toast.success(input.done);
      await queryClient.invalidateQueries({ queryKey: ['renewal'] });
    },
  });
  const run: Run = (call, done) => act.mutate({ call, done });
  const s = state.data;
  if (s === undefined) {
    return <ErrorAlert error={state.error} />;
  }
  const notSubmitted = s.approval.required ? 'Not submitted' : 'Not required';
  const approvalText = label(s.approval.status) || notSubmitted;
  return (
    <div className="stack">
      <ErrorAlert error={act.error} />
      <ClientCard
        key={s.client.status ?? ''}
        state={s}
        run={(status, remarks) =>
          run(
            () => renewalApprovalApi.clientStatus(companyId, ref, status, remarks),
            'Client acceptance status saved',
          )
        }
      />
      <Card title="Approval and Payment">
        <p>
          Approval: <StatusBadge status={approvalText} label={approvalText} full />{' '}
          {s.approval.remarks ?? ''}
        </p>
        <p>
          Payment status:{' '}
          <StatusBadge status={label(s.payment.status)} label={label(s.payment.status)} full />{' '}
          Outstanding <Amount value={s.payment.outstanding} /> of{' '}
          <Amount value={s.payment.premium} />
        </p>
        {s.approval.status === 'RETURNED' && can('RNW_DISPOSE') && (
          <Button
            onClick={() =>
              run(
                () => renewalApprovalApi.resubmit(companyId, ref),
                'Renewal account submitted for review and approval.',
              )
            }
          >
            Resubmit for Approval
          </Button>
        )}
      </Card>
      <BookingOnlyCard
        key={`b-${String(s.bookingOnly.tagged)}`}
        state={s}
        renewalRef={ref}
        run={run}
      />
      <DirectToInsurerCard state={s} renewalRef={ref} run={run} />
    </div>
  );
}
