/** Shape shared by the depreciation and investment run previews. */
interface PreviewLike<L> {
  posted: boolean;
  total: number;
  lines: L[];
  run?: { createdBy: string };
}

export interface RunSummary<L> {
  lines: L[];
  count: number;
  total: number;
  posted: boolean;
  status: 'POSTED' | 'DRAFT';
  postedBy: string | undefined;
  /** Whether posting makes sense: not yet posted and something is due. */
  postable: boolean;
}

/** Summarizes a (possibly still loading) run preview for display. */
export function summarize<L>(preview: PreviewLike<L> | undefined): RunSummary<L> {
  if (preview === undefined) {
    return {
      lines: [],
      count: 0,
      total: 0,
      posted: false,
      status: 'DRAFT',
      postedBy: undefined,
      postable: false,
    };
  }
  return {
    lines: preview.lines,
    count: preview.lines.length,
    total: preview.total,
    posted: preview.posted,
    status: preview.posted ? 'POSTED' : 'DRAFT',
    postedBy: preview.run?.createdBy,
    postable: !preview.posted && preview.lines.length > 0,
  };
}

/** A validation rule: the field, a failure test and the message. */
export type Rule<T> = [field: string, fails: (form: T) => boolean, message: string];

/** Applies validation rules; returns messages by field (first failing rule per field). */
export function check<T>(form: T, rules: readonly Rule<T>[]): Record<string, string> {
  const errors: Record<string, string> = {};
  for (const [field, fails, message] of rules) {
    if (errors[field] === undefined && fails(form)) {
      errors[field] = message;
    }
  }
  return errors;
}
