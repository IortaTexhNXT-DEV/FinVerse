import { FilePlus2, ListChecks } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { ClaimsPlaceholder } from '../ClaimsPlaceholder';

/**
 * Claims home (BRCLM.025/034, NFR 15.03): the handler's open claims, follow-ups due, claims by
 * status and phase, ageing and premium flags. Landing screen of the foundation (CL0); wave CL1-B
 * adds the work tiles.
 */
export default function ClaimsHomePage() {
  const navigate = useNavigate();
  const { can } = useAuth();
  return (
    <ClaimsPlaceholder
      title="Claims Home"
      description="Your claims at a glance: open claims, follow-ups due today and overdue, claims by status, ageing and claims waiting on premium."
      cardTitle="My Claims"
      emptyMessage="No claims to show yet"
      actions={
        <>
          <Button
            variant="secondary"
            icon={<ListChecks size={16} />}
            onClick={() => void navigate('/claims-handling/worklist')}
          >
            Open Worklist
          </Button>
          {can('BCL_RECORD') && (
            <Button
              variant="primary"
              icon={<FilePlus2 size={16} />}
              onClick={() => void navigate('/claims-handling/new')}
            >
              Record Claim
            </Button>
          )}
        </>
      }
    />
  );
}
