import type { TriangleData } from '@/api/reserves';
import { Amount } from '@/components/ui/Amount';
import { formatFactor } from './reserveMath';

/** A triangle cell: blank for a development age not yet observed. */
function Cell({ value }: Readonly<{ value: number | undefined }>) {
  return value === undefined ? null : <Amount value={value} />;
}

/**
 * Cumulative development triangle of the projected basis (accident periods down, development ages
 * across), with the age-to-age factors under each age and the projection per accident period.
 */
export function TriangleGrid({ data }: Readonly<{ data: TriangleData }>) {
  const triangle = data.basis === 'PAID' ? data.paid : data.incurred;
  const ages = triangle.reduce((max, row) => Math.max(max, row.length), 0);
  const ageHeaders = Array.from({ length: ages }, (_, k) => k);
  return (
    <div className="table-wrap">
      <table className="table">
        <caption className="visually-hidden">
          {`${data.businessLine} ${data.basis.toLowerCase()} development triangle`}
        </caption>
        <thead>
          <tr>
            <th>Accident period</th>
            {ageHeaders.map((k) => (
              <th key={k} className="num">
                Age {k}
              </th>
            ))}
            <th className="num">Factor to ultimate</th>
            <th className="num">Ultimate</th>
            <th className="num">Incurred</th>
            <th className="num">IBNR</th>
          </tr>
        </thead>
        <tbody>
          {data.rows.map((r, i) => (
            <tr key={r.accidentPeriod}>
              <th scope="row">{r.accidentPeriod}</th>
              {ageHeaders.map((k) => (
                <td key={k} className="num">
                  <Cell value={triangle[i]?.[k]} />
                </td>
              ))}
              <td className="num">{formatFactor(r.cumulativeFactor)}</td>
              <td className="num">
                <Amount value={r.ultimate} />
              </td>
              <td className="num">
                <Amount value={r.incurred} />
              </td>
              <td className="num">
                <Amount value={r.ibnr} />
              </td>
            </tr>
          ))}
        </tbody>
        <tfoot>
          <tr>
            <th scope="row">Age-to-age factor</th>
            {ageHeaders.map((k) => (
              <td key={k} className="num">
                {formatFactor(data.factors[k])}
              </td>
            ))}
            <td colSpan={3} className="num">
              IBNR of the line
            </td>
            <td className="num">
              <strong>
                <Amount value={data.ibnr} />
              </strong>
            </td>
          </tr>
        </tfoot>
      </table>
    </div>
  );
}
