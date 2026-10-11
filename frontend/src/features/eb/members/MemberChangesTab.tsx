import { UserPlus } from 'lucide-react';
import { useState } from 'react';
import type { ProgrammeView } from '@/api/eb';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { MemberChangeDialog } from './MemberChangeDialog';
import { MemberChangesTable } from './MemberChangesTable';

/** Member Changes tab: the member changes of the programme; Capture Member Change. */
export function MemberChangesTab({ programme }: Readonly<{ programme: ProgrammeView }>) {
  const { can } = useAuth();
  const [capturing, setCapturing] = useState(false);
  return (
    <Card
      flush
      title="Member Changes"
      actions={
        can('EB_MARKET') && (
          <Button
            variant="secondary"
            size="sm"
            icon={<UserPlus size={14} />}
            onClick={() => setCapturing(true)}
          >
            Capture Member Change
          </Button>
        )
      }
    >
      <MemberChangesTable filters={{ programmeId: programme.id }} showProgramme={false} />
      {capturing && (
        <MemberChangeDialog programme={programme} onClose={() => setCapturing(false)} />
      )}
    </Card>
  );
}
