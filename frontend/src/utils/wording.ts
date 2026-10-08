import { acronymOf } from './acronyms';
import { formatAmount } from './format';

/**
 * Wording helpers for counts, names and amounts inside texts: the noun of a count in the right
 * number ("1 Claim", "2 Claims"), sentence case with acronyms kept ("EB statement of account") and
 * amounts written in a text with their thousands separators ("PHP 2,500.00").
 */

/** Nouns whose plural is the same word (uncountable): "1 Account funding", "3 Account funding". */
const UNCOUNTABLE = new Set(['funding', 'onboarding', 'reference', 'information', 'billing']);

/**
 * The plural of a noun phrase, by its last word: "Service fee run" → "Service fee runs", "Policy"
 * → "Policies", "Process" → "Processes", "Account funding" → "Account funding". A word in brackets
 * at the end is kept after the plural ("CWT certificate (2307)" → "CWT certificates (2307)").
 */
export function pluralOf(noun: string): string {
  const open = noun.lastIndexOf(' (');
  if (open > 0 && noun.endsWith(')')) {
    return `${pluralOf(noun.slice(0, open))}${noun.slice(open)}`;
  }
  const space = noun.lastIndexOf(' ');
  const head = noun.slice(0, space + 1);
  const word = noun.slice(space + 1);
  if (word === '' || UNCOUNTABLE.has(word.toLowerCase()) || word === word.toUpperCase()) {
    return noun;
  }
  if (/[^aeiou]y$/i.test(word)) {
    return `${head}${word.slice(0, -1)}ies`;
  }
  if (/(?:s|x|z|ch|sh)$/i.test(word)) {
    return `${head}${word}es`;
  }
  return `${head}${word}s`;
}

/** The noun of a count in the right number: 1 → "Claim", 0 or 2 → "Claims". */
export function nounFor(
  count: number,
  singular: string,
  plural: string = pluralOf(singular),
): string {
  return Math.abs(count) === 1 ? singular : plural;
}

/**
 * Sentence case for a Title Case label: the first word as written, the others in lower case,
 * acronyms and words with inner capitals kept ("Service Fee Run" → "Service fee run", "EB
 * Statement of Account" → "EB statement of account", "Instrument Status Change" → "Instrument
 * status change").
 */
export function sentenceCase(label: string): string {
  return label
    .split(' ')
    .map((word, i) => {
      if (i === 0 || word === '') {
        return word;
      }
      const letters = /^[A-Za-z0-9]+/.exec(word)?.[0] ?? '';
      if (acronymOf(letters) !== null || /[A-Z]/.test(word.slice(1))) {
        return word;
      }
      return word.toLowerCase();
    })
    .join(' ');
}

/**
 * An amount written in a text after its currency code: three capitals, a space and a number with
 * decimals or thousands separators ("PHP 2500.00", "USD 1,200"). A number without either (BIR
 * 2307) is not an amount.
 */
const MONEY = String.raw`\b([A-Z]{3}) (-?(?:\d{1,3}(?:,\d{3})+(?:\.\d+)?|\d+\.\d+))(?![\d.,]*\d)`;

const MONEY_IN_TEXT = new RegExp(MONEY, 'g');

/** An amount with its currency as users read it: "PHP 2,500.00"; the amount alone without one. */
export function formatMoney(currency: string | null | undefined, amount: number | string): string {
  const figure = formatAmount(typeof amount === 'string' ? amount.replace(/,/g, '') : amount);
  return currency ? `${currency} ${figure}` : figure;
}

/**
 * Amounts written in a text with their currency get thousands separators and two decimals:
 * "Mega Traders Inc. PHP 2500" → "Mega Traders Inc. PHP 2,500.00". Other numbers (BIR 2307) are
 * left as they are.
 */
export function formatMoneyInText(text: string): string {
  return text.replace(MONEY_IN_TEXT, (_, currency: string, amount: string) =>
    formatMoney(currency, amount),
  );
}

/** A text split into its words and the amount it ends with ("Maria Clara Santos PHP 500.00"). */
export interface TextWithAmount {
  text: string;
  currency?: string;
  amount?: number;
}

const TRAILING_MONEY = new RegExp(`^(.*?)[\\s,:;–-]*${MONEY}$`);

/**
 * Splits the amount off the end of a description, so a list shows the name and the amount in
 * their own columns: "Maria Clara Santos PHP 500.00" → text "Maria Clara Santos", PHP 500. A text
 * without a trailing amount is returned whole, with amounts inside it formatted.
 */
export function splitTrailingAmount(description: string): TextWithAmount {
  const match = TRAILING_MONEY.exec(description.trim());
  const amount = match === null ? Number.NaN : Number((match[3] ?? '').replace(/,/g, ''));
  if (match === null || (match[1] ?? '').trim() === '' || !Number.isFinite(amount)) {
    return { text: formatMoneyInText(description) };
  }
  return { text: formatMoneyInText((match[1] ?? '').trim()), currency: match[2], amount };
}
