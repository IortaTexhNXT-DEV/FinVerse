import { useId } from 'react';
import { COMMENT_MAX, commentProblem } from './commentRules';
import type { CommentRules } from './commentRules';

interface CommentFieldProps extends CommentRules {
  label?: string;
  value: string;
  onChange: (value: string) => void;
  /** Show the problem under the field (after the first attempt to confirm). */
  showProblem?: boolean;
  /** One line of help under the field, e.g. where the comment is shown. */
  helper?: string;
  rows?: number;
  disabled?: boolean;
  onBlur?: () => void;
}

/** The length rule as users read it: "Up to 500 characters", "10 to 200 characters". */
function lengthRule(min: number | undefined, max: number): string {
  return min === undefined
    ? `Up to ${String(max)} characters`
    : `${String(min)} to ${String(max)} characters`;
}

/** Under the comment: its rule (or what is wrong) on the left, the counter on the right. */
function CommentFoot({
  id,
  length,
  max,
  problem,
  note,
}: Readonly<{ id: string; length: number; max: number; problem?: string; note: string }>) {
  return (
    <span className="comment-foot">
      {problem === undefined ? (
        <span className="field-format">{note}</span>
      ) : (
        <span className="field-error" role="alert">
          {problem}
        </span>
      )}
      <span
        id={`${id}-count`}
        className={length > max * 0.9 ? 'comment-count near' : 'comment-count'}
        aria-label={`${String(length)} of ${String(max)} characters`}
      >
        {length} / {max}
      </span>
    </span>
  );
}

/**
 * The comment or reason of a dialog: a text area with its required mark, the least and most
 * characters, a live counter and the problem shown under the field.
 */
export function CommentField({
  label = 'Comment',
  value,
  onChange,
  required = false,
  min,
  max = COMMENT_MAX,
  showProblem = false,
  helper,
  rows = 3,
  disabled = false,
  onBlur,
}: Readonly<CommentFieldProps>) {
  const id = useId();
  const problem = showProblem
    ? commentProblem(value, { required, min, max, noun: label.toLowerCase() })
    : undefined;
  const length = value.trim().length;
  const rule = lengthRule(min, max);
  return (
    <div className="field comment-field">
      <span className="label-row">
        <label htmlFor={id} className={required ? 'required' : undefined}>
          {label}
        </label>
      </span>
      <textarea
        id={id}
        className="textarea"
        rows={rows}
        maxLength={max}
        required={required}
        disabled={disabled}
        aria-invalid={problem === undefined ? undefined : true}
        aria-describedby={`${id}-count`}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        onBlur={onBlur}
      />
      <CommentFoot id={id} length={length} max={max} problem={problem} note={helper ?? rule} />
    </div>
  );
}
