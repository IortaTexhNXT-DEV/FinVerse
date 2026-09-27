import { LovLabel } from '@/components/broking/LovLabel';

/** Benefit line codes (comma separated) shown with their labels. */
export function BenefitLines({ codes }: Readonly<{ codes: string | null | undefined }>) {
  const list = (codes ?? '')
    .split(',')
    .map((c) => c.trim())
    .filter((c) => c !== '');
  if (list.length === 0) {
    return <span className="muted">—</span>;
  }
  return (
    <>
      {list.map((c, i) => (
        <span key={c}>
          {i > 0 ? ', ' : ''}
          {c}
        </span>
      ))}
    </>
  );
}

/** A list-of-values label of an EB code. */
export function EbLov({ type, code }: Readonly<{ type: string; code: string | null | undefined }>) {
  if (code === null || code === undefined || code === '') {
    return <span className="muted">—</span>;
  }
  return <LovLabel type={type} code={code} />;
}
