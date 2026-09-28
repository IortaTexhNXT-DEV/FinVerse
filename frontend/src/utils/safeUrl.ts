/**
 * An external link's address when it is a web address (http or https); anything else (javascript:,
 * data:, relative or malformed text) gives undefined so the caller shows the text without a link.
 * Every dynamic `href` goes through this function (lint rule in eslint.config.js).
 */
export function safeUrl(url: string | undefined | null): string | undefined {
  if (url === undefined || url === null) {
    return undefined;
  }
  const trimmed = url.trim();
  return /^https?:\/\/[^\s/]/i.test(trimmed) ? trimmed : undefined;
}
