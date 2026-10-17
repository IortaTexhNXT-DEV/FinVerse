import { useContext } from 'react';
import { WorkspaceContext } from '@/context/workspaceContext';
import {
  useCoverTypeName,
  useCoverageName,
  useInsurerBranchName,
  useInsurerName,
  useLineName,
  useLovLabel,
  useProductName,
  useSalesUnitName,
} from './useLabels';

interface LovLabelProps {
  /** List of values the code belongs to (e.g. SOURCE_CHANNEL). */
  type: string;
  code: string | null | undefined;
  /** Text when there is no code. */
  empty?: string;
}

/** The label of a list-of-values code (never the code itself); the humanized code while loading. */
export function LovLabel({ type, code, empty = '—' }: Readonly<LovLabelProps>) {
  const label = useLovLabel(type);
  if (!code) {
    return <span className="muted">{empty}</span>;
  }
  return <>{label(code)}</>;
}

/** Labels of several codes of one list, comma separated; the given text when there is none. */
export function LovLabels({
  type,
  codes,
  empty = '—',
}: Readonly<{ type: string; codes: readonly string[]; empty?: string }>) {
  const label = useLovLabel(type);
  return <>{codes.length > 0 ? codes.map((c) => label(c)).join(', ') : empty}</>;
}

/** The name of a product line (e.g. "Personal Accident"), never its code. */
export function LineLabel({ code }: Readonly<{ code: string | null | undefined }>) {
  const name = useLineName();
  return <>{name(code)}</>;
}

/**
 * "Contractor's All Risk (Engineering)": the product name with the name of its line; the product
 * code is in the tooltip.
 */
export function ProductLineLabel({
  product,
  line,
}: Readonly<{ product: string; line: string | null | undefined }>) {
  const lineName = useLineName();
  const productName = useProductName();
  const name = productName(product);
  return <span title={product}>{line ? `${name} (${lineName(line)})` : name}</span>;
}

/**
 * The name of a product (never its code alone), with the code as a muted second line in a list
 * (`withCode`) or in the tooltip.
 */
export function ProductName({
  code,
  withCode = false,
  empty = '—',
}: Readonly<{ code: string | null | undefined; withCode?: boolean; empty?: string }>) {
  const name = useProductName();
  if (!code) {
    return <span className="muted">{empty}</span>;
  }
  const shown = name(code);
  // A product name that already carries its code ("Motor Comprehensive Package MTR10") is not repeated.
  if (!withCode || shown.includes(code)) {
    return <span title={code}>{shown}</span>;
  }
  return (
    <span className="cell-stack">
      <span>{shown}</span>
      <span className="muted">{code}</span>
    </span>
  );
}

/** The name of an insurer, from its party code (the code while the list loads). */
export function InsurerName({
  code,
  empty = '—',
}: Readonly<{ code: string | null | undefined; empty?: string }>) {
  const name = useInsurerName();
  if (!code) {
    return <span className="muted">{empty}</span>;
  }
  return <>{name(code)}</>;
}

/** Names of several insurers, comma separated; the given text when there is none. */
export function InsurerNames({
  codes,
  empty = '—',
}: Readonly<{ codes: readonly string[]; empty?: string }>) {
  const name = useInsurerName();
  return <>{codes.length > 0 ? codes.map((c) => name(c)).join(', ') : empty}</>;
}

/** The name of a cover type of a line (e.g. "Comprehensive"), never its code. */
export function CoverTypeLabel({
  line,
  code,
  empty = '—',
}: Readonly<{ line?: string | null; code: string | null | undefined; empty?: string }>) {
  const name = useCoverTypeName();
  if (!code) {
    return <span className="muted">{empty}</span>;
  }
  return <>{name(line, code)}</>;
}

/** The name of a coverage or peril of a line (e.g. "Fire and Lightning"), never its code. */
export function CoverageName({
  line,
  code,
}: Readonly<{ line: string | null | undefined; code: string }>) {
  const name = useCoverageName(line);
  return <>{name(code)}</>;
}

/** The name of a sales unit (team, department, region) from its code. */
export function SalesUnitName({
  code,
  empty = '—',
}: Readonly<{ code: string | null | undefined; empty?: string }>) {
  const name = useSalesUnitName();
  if (!code) {
    return <span className="muted">{empty}</span>;
  }
  return <>{name(code)}</>;
}

/**
 * "Mabuhay General Insurance Corp. / Makati": the insurer and its branch by their names (the branch
 * left out when there is none), never their codes.
 */
export function InsurerWithBranch({
  insurer,
  branch,
  empty = '—',
}: Readonly<{ insurer: string | null | undefined; branch?: string | null; empty?: string }>) {
  const insurerName = useInsurerName();
  const branchName = useInsurerBranchName(insurer);
  if (!insurer) {
    return <span className="muted">{empty}</span>;
  }
  return <>{branch ? `${insurerName(insurer)} / ${branchName(branch)}` : insurerName(insurer)}</>;
}

/** "Consumer Banking - NCR / CBG Team 1": the sales units of a record by their names, in order. */
export function SalesUnitPath({
  codes,
  empty = '—',
}: Readonly<{ codes: readonly (string | null | undefined)[]; empty?: string }>) {
  const name = useSalesUnitName();
  const known = codes.filter((c): c is string => Boolean(c));
  return <>{known.length > 0 ? known.map((c) => name(c)).join(' / ') : empty}</>;
}

/**
 * A branch of the company by its name with the code muted after it; the code alone, muted, when
 * it is not one of the company's branches.
 */
export function BranchName({
  code,
  empty = '—',
}: Readonly<{ code: string | null | undefined; empty?: string }>) {
  const branches = useContext(WorkspaceContext)?.branches ?? [];
  if (!code) {
    return <span className="muted">{empty}</span>;
  }
  const name = branches.find((b) => b.code === code)?.name;
  if (name === undefined) {
    return <span className="muted">{code}</span>;
  }
  return (
    <span className="nowrap" title={code}>
      {name} <span className="muted">{code}</span>
    </span>
  );
}

/** Business type of a submitted policy as the BRD-12 FRS names it. */
const BUSINESS_TYPES: Readonly<Record<string, string>> = {
  NB: 'New Business',
  RB: 'Renewal Business',
};

/** The business type by its FRS name with the code muted after it ("New Business NB"). */
export function BusinessTypeName({ code }: Readonly<{ code: string | null | undefined }>) {
  if (!code) {
    return <span className="muted">—</span>;
  }
  const name = BUSINESS_TYPES[code];
  if (name === undefined) {
    return <span className="muted">{code}</span>;
  }
  return (
    <span className="nowrap">
      {name} <span className="muted">{code}</span>
    </span>
  );
}
