import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Ship } from 'lucide-react';
import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { underwritingApi } from '@/api/underwriting';
import type { CertificateInput, OpenCover, Policy } from '@/api/underwriting';
import { useAuth } from '@/auth/authContext';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Kpi } from '@/components/ui/Kpi';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { formatAmount, formatDate, today } from '@/utils/format';
import { DateField, NumberField, TextField } from './FormFields';
import { round2 } from './premiumMath';
import { RiskEditor } from './RiskEditor';
import { emptyRisk } from './policyForm';
import { sourceForIntermediary } from './workflow';
import { WorkflowActions } from './WorkflowActions';

function declared(certificates: Policy[]): number {
  return round2(
    certificates
      .filter((c) => c.document.status !== 'CANCELLED')
      .reduce((acc, c) => acc + c.premium.sumInsured, 0),
  );
}

function Summary({ cover, certificates }: Readonly<{ cover: OpenCover; certificates: Policy[] }>) {
  const used = declared(certificates);
  return (
    <div className="grid-4">
      <Kpi
        label="Client"
        value={cover.customerName}
        hint={`${formatDate(cover.periodFrom)} – ${formatDate(cover.periodTo)}`}
      />
      <Kpi
        label="Limit per shipment"
        value={`${cover.currency} ${formatAmount(cover.limitPerShipment)}`}
        hint={`Rate ${String(cover.rate)}%`}
      />
      <Kpi
        label="Declared"
        value={`${cover.currency} ${formatAmount(used)}`}
        hint={`${String(certificates.length)} certificates`}
      />
      <Kpi
        accent
        label="Remaining annual limit"
        value={`${cover.currency} ${formatAmount(cover.annualLimit - used)}`}
      />
    </div>
  );
}

/** Open cover view: limits used, certificates with their workflow, shipment declaration. */
export default function OpenCoverDetailPage() {
  const id = Number(useParams().id);
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [declaration, setDeclaration] = useState<CertificateInput | null>(null);
  const cover = useQuery({
    queryKey: ['open-cover', id],
    queryFn: () => underwritingApi.openCover(id),
  });
  const certificates = useQuery({
    queryKey: ['certificates', id],
    queryFn: () => underwritingApi.certificates(id),
  });
  const refresh = async () => {
    await queryClient.invalidateQueries({ queryKey: ['certificates', id] });
    await queryClient.invalidateQueries({ queryKey: ['policies'] });
  };
  const declare = useMutation({
    mutationFn: (body: CertificateInput) => underwritingApi.declareShipment(id, body),
    onSuccess: async (cert) => {
      setDeclaration(null);
      toast.success(`Certificate ${cert.policyNo} created as draft`);
      await refresh();
    },
  });

  if (cover.data === undefined) {
    return cover.error ? (
      <ErrorAlert error={cover.error} />
    ) : (
      <span className="spinner" aria-label="Loading" />
    );
  }
  const c = cover.data;
  const certs = certificates.data ?? [];
  const set = (patch: Partial<CertificateInput>) =>
    declaration && setDeclaration({ ...declaration, ...patch });

  return (
    <div className="stack">
      <PageHeader
        section="Underwriting · Open cover"
        title={c.openCoverNo}
        description={c.cargoDescription ?? c.insuredName}
        actions={
          <>
            <StatusBadge status={c.recordStatus} />
            {c.recordStatus === 'ACTIVE' && can('POLICY_MAINTAIN') && (
              <Button
                variant="accent"
                icon={<Ship size={16} />}
                onClick={() =>
                  setDeclaration({ issueDate: today(), transitDays: 60, shipment: emptyRisk() })
                }
              >
                Declare shipment
              </Button>
            )}
          </>
        }
      />
      <Summary cover={c} certificates={certs} />
      <ErrorAlert error={certificates.error} />
      <Card title="Certificates" flush>
        <DataTable<Policy>
          loading={certificates.isLoading}
          rows={certs}
          rowKey={(p) => p.id}
          emptyMessage="No shipments declared yet."
          caption="Certificates"
          columns={[
            {
              key: 'no',
              header: 'Certificate',
              render: (p) => (
                <Link to={`/underwriting/policies/${String(p.id)}`}>{p.policyNo}</Link>
              ),
            },
            { key: 'ves', header: 'Vessel', render: (p) => p.risks[0]?.marine?.vesselName ?? '' },
            {
              key: 'voy',
              header: 'Voyage',
              render: (p) =>
                `${p.risks[0]?.marine?.voyageFrom ?? ''} → ${p.risks[0]?.marine?.voyageTo ?? ''}`,
            },
            { key: 'sail', header: 'Sailing', render: (p) => formatDate(p.periodFrom) },
            {
              key: 'si',
              header: 'Sum insured',
              numeric: true,
              render: (p) => <Amount value={p.premium.sumInsured} />,
            },
            {
              key: 'net',
              header: 'Net premium',
              numeric: true,
              render: (p) => <Amount value={p.premium.ourNetPremium} />,
            },
            {
              key: 'st',
              header: 'Status',
              render: (p) => <StatusBadge status={p.document.status} />,
            },
            {
              key: 'act',
              header: 'Actions',
              render: (p) => (
                <div className="row">
                  <WorkflowActions
                    compact
                    label={p.policyNo}
                    facts={p.document}
                    handlers={{
                      submit: () => underwritingApi.submitPolicy(p.id),
                      discard: () => underwritingApi.discardPolicy(p.id),
                      approve: (date) => underwritingApi.approvePolicy(p.id, date),
                      reject: (reason) => underwritingApi.rejectPolicy(p.id, reason),
                      done: refresh,
                    }}
                  />
                </div>
              ),
            },
          ]}
        />
      </Card>
      <Modal
        title={`Declare shipment under ${c.openCoverNo}`}
        open={declaration !== null}
        onClose={() => setDeclaration(null)}
        footer={
          <Button
            variant="accent"
            busy={declare.isPending}
            onClick={() => declaration && declare.mutate(declaration)}
          >
            Create certificate
          </Button>
        }
      >
        <ErrorAlert error={declare.error} />
        {declaration !== null && (
          <div className="stack">
            <div className="form-grid">
              <DateField
                label="Issue date"
                required
                value={declaration.issueDate}
                onChange={(v) => set({ issueDate: v })}
              />
              <NumberField
                label="Transit days"
                value={declaration.transitDays}
                onChange={(v) => set({ transitDays: v })}
              />
              <TextField
                label="Broker / agent code"
                hint="Blank = direct"
                value={declaration.intermediaryCode}
                onChange={(v) =>
                  set({
                    intermediaryCode: v === '' ? undefined : v,
                    sourceType: sourceForIntermediary(v),
                  })
                }
              />
            </div>
            <RiskEditor
              risks={[declaration.shipment]}
              marine
              single
              onChange={(risks) => set({ shipment: risks[0] ?? emptyRisk() })}
            />
          </div>
        )}
      </Modal>
    </div>
  );
}
