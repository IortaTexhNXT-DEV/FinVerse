import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Coins } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { closingApi } from '@/api/closing';
import { periodApi } from '@/api/periods';
import type { Period } from '@/api/periods';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { ChecklistView } from './ChecklistView';
import { PeriodSelectors } from './PeriodSelectors';
import type { usePeriodPicker } from './usePeriodPicker';

type Transition = 'startClosing' | 'close';

interface ActionProps {
  period: Period;
  ready: boolean;
  busy: boolean;
  onTransition: (action: Transition) => void;
}

/** Soft-close / close buttons, shown for the statuses they apply to. */
function PeriodActions({ period, ready, busy, onTransition }: Readonly<ActionProps>) {
  const closable = period.status === 'CLOSING' || period.status === 'OPEN';
  return (
    <>
      {period.status === 'OPEN' && (
        <Button variant="secondary" busy={busy} onClick={() => onTransition('startClosing')}>
          Start Soft Close
        </Button>
      )}
      {closable && (
        <Button
          variant="accent"
          busy={busy}
          disabled={!ready}
          onClick={() => onTransition('close')}
        >
          Close Period
        </Button>
      )}
    </>
  );
}

/** Monthly close: checklist of the selected period, FX revaluation link, soft close and close. */
export function PeriodEndPanel({
  picker,
}: Readonly<{ picker: ReturnType<typeof usePeriodPicker> }>) {
  const { companyId, period } = picker;
  const periodId = period?.id ?? 0;
  const { can } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const checklist = useQuery({
    queryKey: ['period-checklist', companyId, periodId],
    queryFn: () => closingApi.periodChecklist(companyId, periodId),
    enabled: companyId > 0 && periodId > 0,
  });
  const transition = useMutation({
    mutationFn: (action: Transition) => periodApi[action](periodId),
    onSuccess: async (p) => {
      await queryClient.invalidateQueries({ queryKey: ['periods'] });
      await queryClient.invalidateQueries({ queryKey: ['period-checklist'] });
      toast.success(`Period ${p.name} is now ${p.status}`);
    },
  });

  return (
    <>
      <Card>
        <div className="form-grid">
          <PeriodSelectors picker={picker} />
          <div className="row" style={{ alignSelf: 'end' }}>
            {period && <StatusBadge status={period.status} />}
            <Button
              variant="secondary"
              icon={<Coins size={16} />}
              onClick={() => void navigate('/planning/fx-revaluation')}
            >
              FX Revaluation
            </Button>
            {period && can('PERIOD_MANAGE') && (
              <PeriodActions
                period={period}
                ready={checklist.data?.ready === true}
                busy={transition.isPending}
                onTransition={(a) => transition.mutate(a)}
              />
            )}
          </div>
        </div>
      </Card>
      <ErrorAlert error={checklist.error ?? transition.error} />
      <Card title={`Closing checklist ${period?.name ?? ''}`} flush>
        <ChecklistView checklist={checklist.data} loading={checklist.isLoading} />
      </Card>
    </>
  );
}
