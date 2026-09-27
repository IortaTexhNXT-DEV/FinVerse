import { useQuery } from '@tanstack/react-query';
import { FileDown, FileSpreadsheet } from 'lucide-react';
import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { ebMarketApi } from '@/api/ebMarket';
import type { ComparativeView } from '@/api/ebMarket';
import { useAuth } from '@/auth/authContext';
import { RecordHeader } from '@/components/broking/RecordHeader';
import { ReferenceChip } from '@/components/broking/ReferenceChip';
import { useFileDownload } from '@/components/broking/useFileDownload';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { UserName } from '@/components/ui/UserName';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate, statusPhrase } from '@/utils/format';
import { EB_SECTION } from '../EbPlaceholder';
import '../eb.css';
import { orNone } from '../common/formValues';
import { useEbMutation } from '../common/useEbMutation';
import { PremiumMatrix, TermsMatrix } from './ComparativeMatrix';
import { CommentsCard, DecisionsCard } from './ComparativeSide';
import { savedRecommendation, STEPS, stepsFor } from './comparativeSteps';
import type { Step } from './comparativeSteps';

function Body({ view }: Readonly<{ view: ComparativeView }>) {
  const { can } = useAuth();
  const companyId = useCompanyId();
  const download = useFileDownload();
  const [step, setStep] = useState<Step>();
  const draft = view.comparative.status === 'DRAFT' && can('EB_MARKET');
  const [recommendation, setRecommendation] = useState(() => savedRecommendation(view));
  const [summary, setSummary] = useState(view.summary ?? '');
  const id = view.comparative.id;
  const act = useEbMutation(
    (c, v: { step: Step; remarks: string }) =>
      ebMarketApi.comparativeAction(c, id, v.step, orNone(v.remarks)),
    (r: ComparativeView) =>
      `Comparative ${r.comparative.comparativeNo} is now ${statusPhrase(r.comparative.status)}`,
    () => setStep(undefined),
  );
  const save = useEbMutation<undefined, ComparativeView>(
    (c) => ebMarketApi.recommend(c, id, { recommendation, summary: orNone(summary) }),
    'Recommendation saved',
    () => undefined,
  );
  const exportAs = (format: 'pdf' | 'xlsx') =>
    download.mutate(() => ebMarketApi.exportComparative(companyId, id, format));
  const steps = stepsFor(view, can);
  return (
    <>
      <PageHeader
        section={EB_SECTION}
        title={`Comparative ${view.comparative.comparativeNo}`}
        backTo={`/eb/programmes/${String(view.programmeId)}?tab=comparative`}
        actions={
          <>
            <Button
              variant="secondary"
              icon={<FileDown size={16} />}
              onClick={() => exportAs('pdf')}
            >
              PDF
            </Button>
            <Button
              variant="secondary"
              icon={<FileSpreadsheet size={16} />}
              onClick={() => exportAs('xlsx')}
            >
              Excel
            </Button>
            {steps.map((s) => (
              <Button
                key={s}
                variant={s === 'return' ? 'secondary' : 'accent'}
                onClick={() => setStep(s)}
              >
                {STEPS[s].label}
              </Button>
            ))}
          </>
        }
      />
      <RecordHeader
        chips={
          <>
            <ReferenceChip label="Programme" value={view.programmeNo} />
            <ReferenceChip label="Cycle" value={view.cycleNo} />
          </>
        }
        status={view.comparative.status}
        statuses={[{ label: 'Cycle', status: view.cycleStage }]}
        facts={[
          { label: 'Client', value: view.clientName },
          { label: 'Programme', value: view.programmeName },
          { label: 'Account Officer', value: <UserName login={view.accountOfficer} /> },
          { label: 'Version', value: String(view.comparative.versionNo) },
          { label: 'Sign-off Due', value: formatDate(view.comparative.dueDate) },
          {
            label: 'Value Threshold',
            value: view.comparative.thresholdRules ? 'Exceeded' : 'Within',
          },
        ]}
      />
      <ErrorAlert error={download.error ?? save.error} />
      <PremiumMatrix
        matrix={view.matrix}
        recommendation={recommendation}
        editable={draft}
        onRecommend={(line, proposalId) =>
          setRecommendation({ ...recommendation, [line]: proposalId })
        }
      />
      <Card title="Recommendation">
        {draft ? (
          <div className="stack">
            <Field label="Summary for the client">
              {(fid) => (
                <textarea
                  id={fid}
                  className="textarea"
                  rows={3}
                  value={summary}
                  onChange={(e) => setSummary(e.target.value)}
                />
              )}
            </Field>
            <div>
              <Button busy={save.isPending} onClick={() => save.mutate(undefined)}>
                Save Recommendation
              </Button>
            </div>
          </div>
        ) : (
          <p>{view.summary ?? <span className="muted">No summary</span>}</p>
        )}
      </Card>
      <TermsMatrix matrix={view.matrix} />
      <DecisionsCard view={view} />
      <CommentsCard view={view} />
      {step && (
        <ConfirmDialog
          title={STEPS[step].label}
          record={view.comparative.comparativeNo}
          effect={STEPS[step].effect}
          confirmLabel={STEPS[step].label}
          reason={STEPS[step].reason}
          destructive={step === 'return'}
          busy={act.isPending}
          error={act.error}
          onConfirm={(remarks) => act.mutate({ step, remarks })}
          onClose={() => setStep(undefined)}
        />
      )}
    </>
  );
}

/**
 * Comparative (design 10.1): the validated proposals side by side per benefit line with the
 * lowest premium marked, the AO's recommendation and summary, the terms and capability ratings,
 * the sign-off and value-threshold approval, the comments, and the export in PDF and Excel.
 */
export default function ComparativePage() {
  const id = Number(useParams().id);
  const companyId = useCompanyId();
  const view = useQuery({
    queryKey: ['eb', 'comparative', id, companyId],
    queryFn: () => ebMarketApi.comparative(companyId, id),
    enabled: companyId > 0,
  });
  return (
    <div className="stack">
      {view.data ? (
        <Body key={`${String(id)}-${view.data.comparative.status}`} view={view.data} />
      ) : (
        <ErrorAlert error={view.error} onRetry={() => void view.refetch()} />
      )}
      {view.isLoading && <div className="card" aria-busy="true" aria-label="Loading" />}
    </div>
  );
}
