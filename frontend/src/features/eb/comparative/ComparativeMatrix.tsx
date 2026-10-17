import type { ComparativeMatrix as Matrix, MatrixColumn } from '@/api/ebMarket';
import { Amount } from '@/components/ui/Amount';
import { Card } from '@/components/ui/Card';
import { Tag } from '@/components/ui/Tag';
import { formatDate } from '@/utils/format';
import { ebLabel } from '../common/ebCodes';

const key = (c: MatrixColumn) => String(c.proposalId);

function ColumnHeads({ columns }: Readonly<{ columns: MatrixColumn[] }>) {
  return (
    <>
      {columns.map((c) => (
        <th key={c.proposalId} scope="col">
          <div>{c.insurerName}</div>
          <div className="muted">
            {c.proposalNo} · {ebLabel(c.kind)}
            {c.validUntil ? ` · valid to ${formatDate(c.validUntil)}` : ''}
          </div>
        </th>
      ))}
    </>
  );
}

/**
 * The premium matrix: one row per benefit line and one column per validated proposal, with the
 * lowest premium marked; in a draft the AO picks the recommended proposal of each line.
 */
export function PremiumMatrix({
  matrix,
  recommendation,
  editable,
  onRecommend,
}: Readonly<{
  matrix: Matrix;
  recommendation: Record<string, number>;
  editable: boolean;
  onRecommend: (benefitLine: string, proposalId: number) => void;
}>) {
  const columns = matrix.proposals;
  return (
    <Card title="Premiums per Benefit Line" flush>
      <div className="table-wrap">
        <table className="table eb-matrix">
          <thead>
            <tr>
              <th scope="col">Benefit Line</th>
              <ColumnHeads columns={columns} />
            </tr>
          </thead>
          <tbody>
            {matrix.lines.map((line) => (
              <tr key={line.benefitLine}>
                <th scope="row">{line.label}</th>
                {columns.map((c) => {
                  const offer = line.offers[key(c)];
                  if (!offer) {
                    return (
                      <td key={c.proposalId} className="muted">
                        Not offered
                      </td>
                    );
                  }
                  const chosen = recommendation[line.benefitLine] === c.proposalId;
                  return (
                    <td key={c.proposalId} className={chosen ? 'eb-recommended' : undefined}>
                      <div className="num">
                        <Amount value={offer.annualPremium} />
                      </div>
                      <div className="muted num">
                        Sum insured <Amount value={offer.sumInsured} />
                      </div>
                      <div className="muted">{offer.plans.map((p) => p.planCode).join(', ')}</div>
                      {line.lowestProposalId === c.proposalId && <Tag tone="info">Lowest</Tag>}{' '}
                      {editable ? (
                        <label className="checkbox">
                          <input
                            type="radio"
                            name={`recommend-${line.benefitLine}`}
                            checked={chosen}
                            onChange={() => onRecommend(line.benefitLine, c.proposalId)}
                          />
                          Recommend
                        </label>
                      ) : (
                        chosen && <Tag tone="flag">Recommended</Tag>
                      )}
                    </td>
                  );
                })}
              </tr>
            ))}
            <tr>
              <th scope="row">Total</th>
              {columns.map((c) => (
                <td key={c.proposalId} className="num">
                  <Amount value={c.totalPremium} />
                </td>
              ))}
            </tr>
          </tbody>
        </table>
      </div>
    </Card>
  );
}

/** The insurers' answers to the terms of reference, with the deviations marked. */
export function TermsMatrix({ matrix }: Readonly<{ matrix: Matrix }>) {
  const columns = matrix.proposals;
  if (matrix.items.length === 0 && matrix.factors.length === 0) {
    return null;
  }
  return (
    <Card title="Terms and Capability" flush>
      <div className="table-wrap">
        <table className="table eb-matrix">
          <thead>
            <tr>
              <th scope="col">Requirement</th>
              <ColumnHeads columns={columns} />
            </tr>
          </thead>
          <tbody>
            {matrix.items.map((item) => (
              <tr key={item.torItemId}>
                <th scope="row">
                  <div>{item.description}</div>
                  <div className="muted">
                    {item.benefitLine} · {item.requirement}
                  </div>
                </th>
                {columns.map((c) => {
                  const a = item.answers[key(c)];
                  return (
                    <td key={c.proposalId} className={a?.deviation ? 'eb-deviation' : undefined}>
                      {a ? a.offeredValue : <span className="muted">No answer</span>}
                      {a?.deviation && (
                        <>
                          {' '}
                          <Tag tone="danger">Deviation</Tag>
                        </>
                      )}
                      {a?.remark && <div className="muted">{a.remark}</div>}
                    </td>
                  );
                })}
              </tr>
            ))}
            {matrix.factors.map((f) => (
              <tr key={f.factorCode}>
                <th scope="row">{f.label}</th>
                {columns.map((c) => {
                  const r = f.ratings[key(c)];
                  return (
                    <td key={c.proposalId}>
                      {r?.rating !== null && r?.rating !== undefined
                        ? `${String(r.rating)} / 5`
                        : ''}
                      {r?.value && <div className="muted">{r.value}</div>}
                    </td>
                  );
                })}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </Card>
  );
}
