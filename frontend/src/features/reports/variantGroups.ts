import type { ReportVariant } from '@/api/nbReports';

/** The variants of a report in the groups of the variant list. */
export interface VariantGroups {
  /** The user's own variants (not the standard ones). */
  mine: ReportVariant[];
  /** The non-empty groups in list order: Standard, My Variants, Shared by Others. */
  sections: { label: string; variants: ReportVariant[] }[];
}

/** Groups the variants: the standard ones first, then the user's, then those shared by others. */
export function variantGroups(variants: readonly ReportVariant[]): VariantGroups {
  const standard = variants.filter((v) => v.standard);
  const mine = variants.filter((v) => !v.standard && v.mine);
  const shared = variants.filter((v) => !v.standard && !v.mine);
  const sections = [
    { label: 'Standard', variants: standard },
    { label: 'My Variants', variants: mine },
    { label: 'Shared by Others', variants: shared },
  ].filter((s) => s.variants.length > 0);
  return { mine, sections };
}
