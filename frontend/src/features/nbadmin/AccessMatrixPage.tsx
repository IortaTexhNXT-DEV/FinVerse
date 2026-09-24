import { useMutation, useQuery } from '@tanstack/react-query';
import { Check, FileDown } from 'lucide-react';
import { useState } from 'react';
import { saveFile } from '@/api/client';
import { nbadminApi } from '@/api/nbadmin';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { useToast } from '@/components/ui/toastContext';

/**
 * The agreed User Access Matrix (BRD 3.3.4): every role against every permission as granted
 * today, with the number of enabled users per role. Read-only; export it to Excel for sign-off.
 */
export default function AccessMatrixPage() {
  const toast = useToast();
  const [filter, setFilter] = useState('');
  const matrix = useQuery({ queryKey: ['nbadmin', 'matrix'], queryFn: nbadminApi.matrix });
  const download = useMutation({
    mutationFn: nbadminApi.exportMatrix,
    onSuccess: (file) => {
      saveFile(file.blob, file.fileName);
      toast.success(`${file.fileName} downloaded`);
    },
  });
  const needle = filter.trim().toUpperCase();
  const rows = (matrix.data?.permissions ?? []).filter((p) => p.permission.includes(needle));
  const roles = matrix.data?.roles ?? [];
  return (
    <div className="stack">
      <PageHeader
        section="Broking Setup"
        title="User Access Matrix"
        description="Roles and the functions (permissions) each grants. Changes to roles go through access requests and the Roles & Permissions screen."
        actions={
          <Button
            variant="secondary"
            icon={<FileDown size={16} />}
            busy={download.isPending}
            onClick={() => download.mutate()}
          >
            Export to Excel
          </Button>
        }
      />
      <ErrorAlert error={matrix.error ?? download.error} />
      <Card>
        <Field label="Find a permission">
          {(id) => (
            <input
              id={id}
              className="input"
              placeholder="e.g. CLIENT"
              value={filter}
              onChange={(e) => setFilter(e.target.value)}
            />
          )}
        </Field>
      </Card>
      <Card flush>
        {matrix.isLoading ? (
          <span className="spinner" aria-label="Loading" />
        ) : (
          <div className="matrix-wrap">
            <table className="matrix">
              <caption className="visually-hidden">Roles against permissions</caption>
              <thead>
                <tr>
                  <th scope="col">Permission</th>
                  {roles.map((r) => (
                    <th key={r.code} scope="col" title={r.name}>
                      {r.code}
                      <div className="muted">{r.enabledUsers} users</div>
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {rows.map((p) => (
                  <tr key={p.permission}>
                    <th scope="row">{p.permission}</th>
                    {roles.map((r) => (
                      <td key={r.code}>
                        {p.roles.includes(r.code) && (
                          <Check size={14} className="matrix-granted" aria-label="granted" />
                        )}
                      </td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>
    </div>
  );
}
