import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { authApi } from '@/api/auth';
import type { UserSessionEntry } from '@/api/auth';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Modal } from '@/components/ui/Modal';
import { PageFooter } from '@/components/ui/Pager';
import { useToast } from '@/components/ui/toastContext';
import { SessionsTable } from '@/features/profile/SessionsTable';

const PAGE_SIZE = 10;

/**
 * The sessions of one user for the System Administrator (UAM-NFR-35; FR-UA-004): sign-in, last
 * activity, end and reason; End Session signs the user out on every instance (ADMIN_ENDED).
 */
export function UserSessionsDialog({
  username,
  onClose,
}: Readonly<{ username: string; onClose: () => void }>) {
  const toast = useToast();
  const queries = useQueryClient();
  const [page, setPage] = useState(0);
  const sessions = useQuery({
    queryKey: ['admin-sessions', username, page],
    queryFn: () => authApi.sessions({ username, page, size: PAGE_SIZE }),
  });
  const end = useMutation({
    mutationFn: (s: UserSessionEntry) => authApi.endSession(s.sessionId),
    onSuccess: () => {
      toast.success('Session ended');
      void queries.invalidateQueries({ queryKey: ['admin-sessions'] });
      void queries.invalidateQueries({ queryKey: ['online-users'] });
    },
  });
  return (
    <Modal
      title={`Sessions of ${username}`}
      open
      onClose={onClose}
      footer={
        <Button variant="secondary" onClick={onClose}>
          Close
        </Button>
      }
    >
      <div className="stack">
        <ErrorAlert error={end.error} />
        <SessionsTable
          rows={sessions.data?.content ?? []}
          loading={sessions.isLoading}
          onEnd={(s) => end.mutate(s)}
        />
        <PageFooter data={sessions.data} noun="sessions" onPage={setPage} />
      </div>
    </Modal>
  );
}
