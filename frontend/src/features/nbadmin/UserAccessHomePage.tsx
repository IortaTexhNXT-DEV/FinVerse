import { Grid3x3, UserCog } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/auth/authContext';
import { SectionLanding } from '@/components/broking/SectionLanding';
import { Button } from '@/components/ui/Button';

/**
 * User Access request queues (BRD 1.008, 2.002; UAM-NFR-40): my requests, assigned to me, second
 * approval and for implementation, once the request lifecycle adds them. Until then the page leads
 * to the access requests and the user access matrix.
 */
export default function UserAccessHomePage() {
  const navigate = useNavigate();
  const { can } = useAuth();
  const canRequest = can('ACCESS_REQUEST') || can('ACCESS_APPROVE');
  return (
    <SectionLanding
      section="Setup & Administration"
      title="Access Request Queues"
      description="Requests to enroll, modify, deactivate and reactivate users and to change group profiles: drafts, returned requests, requests waiting for you, second approvals and approved group-profile requests to implement."
      cardTitle="Request Queues"
      emptyMessage="No access requests in your queues"
      actions={
        canRequest ? (
          <>
            <Button
              variant="secondary"
              icon={<Grid3x3 size={16} />}
              onClick={() => void navigate('/broking-setup/access-matrix')}
            >
              Open User Access Matrix
            </Button>
            <Button
              icon={<UserCog size={16} />}
              onClick={() => void navigate('/broking-setup/access-requests')}
            >
              Open Access Requests
            </Button>
          </>
        ) : undefined
      }
    />
  );
}
