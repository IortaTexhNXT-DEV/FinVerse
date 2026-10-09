import { countOf } from '@/utils/format';

/** The length rules of a comment or reason. */
export interface CommentRules {
  required?: boolean;
  /** Least number of characters once something is entered (or always, when required). */
  min?: number;
  max?: number;
  /** The name of the field in messages ("reason"); "comment" by default. */
  noun?: string;
}

/** The default longest comment of a dialog. */
export const COMMENT_MAX = 500;

/** What is wrong with a comment, in words, or undefined when it may be saved. */
export function commentProblem(text: string, rules: Readonly<CommentRules>): string | undefined {
  const length = text.trim().length;
  const max = rules.max ?? COMMENT_MAX;
  const noun = rules.noun ?? 'comment';
  if (rules.required === true && length === 0) {
    return `Enter the ${noun}.`;
  }
  if (rules.min !== undefined && length > 0 && length < rules.min) {
    return `Enter at least ${countOf(rules.min, 'character')}.`;
  }
  if (length > max) {
    return `Shorten the ${noun} to ${countOf(max, 'character')}.`;
  }
  return undefined;
}
