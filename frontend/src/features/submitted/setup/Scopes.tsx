import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { submittedApi } from '@/api/submitted';
import type { ScopeView } from '@/api/submitted';
import { LovLabels } from '@/components/broking/LovLabel';
import { LovSelect } from '@/components/broking/LovSelect';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { useToast } from '@/components/ui/toastContext';
import { UserName } from '@/components/ui/UserName';
import { useCompanyId } from '@/context/workspaceContext';
import { SBM_LOV } from '../common/submittedCodes';
import { UserSelect } from '../common/UserSelect';

/** The segment and own-record scopes of the users. */
export function Scopes() {
  const companyId = useCompanyId();
  const queryClient = useQueryClient();
  const toast = useToast();
  const [username, setUsername] = useState('');
  const [segment, setSegment] = useState('');
  const [own, setOwn] = useState(false);
  const scopes = useQuery({
    queryKey: ['submitted', 'scopes', companyId],
    queryFn: () => submittedApi.scopes(companyId),
    enabled: companyId > 0,
  });
  const save = useMutation({
    mutationFn: () =>
      submittedApi.saveScope(companyId, {
        username,
        segments: segment === '' ? [] : [segment],
        ownRecordsOnly: own,
      }),
    onSuccess: () => {
      toast.success('Scope saved');
      void queryClient.invalidateQueries({ queryKey: ['submitted', 'scopes'] });
    },
  });
  return (
    <div className="stack">
      <Card title="Set the Scope of a User">
        <ErrorAlert error={save.error} />
        <div className="form-grid">
          <Field label="User" required>
            {(id) => (
              <UserSelect
                id={id}
                permission="SBM_MAINTAIN"
                value={username}
                onChange={setUsername}
              />
            )}
          </Field>
          <Field label="Segment">
            {(id) => (
              <LovSelect
                id={id}
                type={SBM_LOV.segment}
                value={segment}
                placeholder="All segments"
                onChange={setSegment}
              />
            )}
          </Field>
          <Field label="Own Records Only">
            {(id) => (
              <input
                id={id}
                type="checkbox"
                checked={own}
                onChange={(e) => setOwn(e.target.checked)}
              />
            )}
          </Field>
        </div>
        <div className="form-actions">
          <Button disabled={username === '' || save.isPending} onClick={() => save.mutate()}>
            Save Scope
          </Button>
        </div>
      </Card>
      <Card flush>
        <DataTable<ScopeView>
          loading={scopes.isLoading}
          rows={scopes.data ?? []}
          rowKey={(s) => s.username}
          emptyMessage="Every user sees every record"
          columns={[
            { key: 'user', header: 'User', render: (s) => <UserName login={s.username} /> },
            {
              key: 'segments',
              header: 'Segments',
              render: (s) => <LovLabels type={SBM_LOV.segment} codes={s.segments} empty="All" />,
            },
            {
              key: 'own',
              header: 'Own Records Only',
              render: (s) => (s.ownRecordsOnly ? 'Yes' : 'No'),
            },
          ]}
        />
      </Card>
    </div>
  );
}
