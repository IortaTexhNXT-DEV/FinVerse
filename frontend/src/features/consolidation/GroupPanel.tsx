import { Play } from 'lucide-react';
import type { ConsolidationGroup } from '@/api/consolidation';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { Field } from '@/components/ui/Field';

interface Props {
  groups: ConsolidationGroup[];
  group?: ConsolidationGroup;
  asOf: string;
  canRun: boolean;
  running: boolean;
  companyCode: (id?: number) => string;
  onGroup: (id: number) => void;
  onAsOf: (date: string) => void;
  onRun: () => void;
}

function membersText(group: ConsolidationGroup, code: (id?: number) => string): string {
  const members = group.members.map((m) => `${code(m.companyId)} ${m.ownershipPct}%`).join(', ');
  return `Parent ${code(group.parentCompanyId)} · ${group.currency} · members: ${members || 'none'}`;
}

/** Group selection, as-of date and the run button. */
export function GroupPanel(props: Readonly<Props>) {
  const { groups, group, asOf, canRun, running, companyCode } = props;
  return (
    <Card title="Group">
      <div className="form-grid">
        <Field label="Consolidation group">
          {(id) => (
            <select
              id={id}
              className="select"
              value={group?.id ?? 0}
              onChange={(e) => props.onGroup(Number(e.target.value))}
            >
              {groups.map((g) => (
                <option key={g.id} value={g.id}>
                  {g.code} – {g.name}
                </option>
              ))}
            </select>
          )}
        </Field>
        <Field label="As of date">
          {(id) => (
            <input
              id={id}
              className="input"
              type="date"
              value={asOf}
              onChange={(e) => props.onAsOf(e.target.value)}
            />
          )}
        </Field>
        {canRun && (
          <Button
            variant="accent"
            icon={<Play size={16} />}
            busy={running}
            disabled={group === undefined}
            onClick={props.onRun}
            style={{ alignSelf: 'end' }}
          >
            Run consolidation
          </Button>
        )}
      </div>
      {group && <p className="muted">{membersText(group, companyCode)}</p>}
    </Card>
  );
}
