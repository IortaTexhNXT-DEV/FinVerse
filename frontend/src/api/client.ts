/**
 * Thin, typed HTTP client for the BrokerVerse REST API.
 *
 * - Attaches the bearer token and renews it shortly before it expires (the short access token is
 *   renewed with the refresh token, an HttpOnly cookie the page never reads), and once more when a
 *   request is refused with 401; the user is not signed out while working.
 * - Converts RFC 7807 problem responses into {@link ApiError} with the stable backend `code`.
 * - Signals an ended session through {@link onUnauthorized} so the app can return to login.
 */

const TOKEN_KEY = 'brokerverse.token';
const EXPIRES_KEY = 'brokerverse.expiresAt';
const ACCESS_EXPIRES_KEY = 'brokerverse.accessExpiresAt';

/** The access token is renewed when it expires within this time. */
export const REFRESH_MARGIN_MS = 60_000;

/** Header the server requires on the renewal (a cross-site form cannot set it). */
const REFRESH_HEADER = 'X-Requested-With';

export interface ProblemDetail {
  title?: string;
  detail?: string;
  status?: number;
  code?: string;
  errors?: Record<string, string>;
  /** Correlation id of the request, quoted to support. */
  correlationId?: string;
  /** Reference of an unexpected error in the server log. */
  reference?: string;
}

/** Status of a request that never reached the server (network down, server stopped). */
export const NETWORK_STATUS = 0;

/**
 * Business-language message of a failed request without a detail from the server: never an HTTP
 * code, a stack trace or raw JSON.
 */
export function defaultErrorMessage(status: number): string {
  if (status === NETWORK_STATUS) {
    return 'The system could not be reached. Check your connection and try again.';
  }
  if (status === 403) {
    return 'You do not have access to this function.';
  }
  if (status === 404) {
    return 'The record or page was not found.';
  }
  if (status === 409) {
    return 'The record was changed by another user. Reload it and try again.';
  }
  if (status >= 500) {
    return 'The system could not complete the request. Try again; if it continues, contact support with the reference below.';
  }
  return 'The request could not be completed.';
}

const CLASS_NAME = /^([A-Z][a-z0-9]+(?:[A-Z][a-z0-9]*)+)( not found| already exists)\b/;

/**
 * A server message as users read it: a record named by its class name at the start of a "not
 * found" or "already exists" message (AutoBookRule not found: 7) is written in words (Auto book
 * rule not found: 7). The server names records in words; this keeps older messages readable.
 */
export function businessDetail(detail: string): string {
  const m = CLASS_NAME.exec(detail);
  if (m === null) {
    return detail;
  }
  const [, name = '', rest = ''] = m;
  const words = name.replace(/([a-z0-9])([A-Z])/g, '$1 $2').toLowerCase();
  const text = words.charAt(0).toUpperCase() + words.slice(1);
  return text + rest + detail.slice(name.length + rest.length);
}

export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly fieldErrors: Record<string, string>;
  /** Reference for support: the correlation id or the server log reference. */
  readonly reference?: string;

  constructor(status: number, problem: ProblemDetail) {
    super(
      problem.detail === undefined ? defaultErrorMessage(status) : businessDetail(problem.detail),
    );
    this.name = 'ApiError';
    this.status = status;
    this.code = problem.code ?? (status === NETWORK_STATUS ? 'NETWORK_ERROR' : 'UNKNOWN');
    this.fieldErrors = problem.errors ?? {};
    this.reference = problem.reference ?? problem.correlationId;
  }

  /** Whether trying again may help (network or server failure). */
  get retryable(): boolean {
    return this.status === NETWORK_STATUS || this.status >= 500;
  }
}

let unauthorizedHandler: (() => void) | undefined;

export function onUnauthorized(handler: () => void): void {
  unauthorizedHandler = handler;
}

/**
 * The bearer token, the absolute end of the session and the expiry of the token, kept per tab in
 * sessionStorage (never localStorage); other tabs obtain them through the session handshake
 * (session/tabSync.ts, BRNB.082).
 */
export const tokenStore = {
  get: (): string | null => sessionStorage.getItem(TOKEN_KEY),
  /** End of the sign-in session. */
  expiresAt: (): string | null => sessionStorage.getItem(EXPIRES_KEY),
  /** Expiry of the access token. */
  accessExpiresAt: (): string | null => sessionStorage.getItem(ACCESS_EXPIRES_KEY),
  set: (token: string, expiresAt?: string, accessExpiresAt?: string): void => {
    sessionStorage.setItem(TOKEN_KEY, token);
    if (expiresAt === undefined) {
      sessionStorage.removeItem(EXPIRES_KEY);
    } else {
      sessionStorage.setItem(EXPIRES_KEY, expiresAt);
    }
    if (accessExpiresAt === undefined) {
      sessionStorage.removeItem(ACCESS_EXPIRES_KEY);
    } else {
      sessionStorage.setItem(ACCESS_EXPIRES_KEY, accessExpiresAt);
    }
  },
  clear: (): void => {
    sessionStorage.removeItem(TOKEN_KEY);
    sessionStorage.removeItem(EXPIRES_KEY);
    sessionStorage.removeItem(ACCESS_EXPIRES_KEY);
  },
};

/** Whether the access token expires within the margin (pure, for tests). */
export function renewalDue(
  accessExpiresAt: string | null,
  now: number,
  marginMs = REFRESH_MARGIN_MS,
): boolean {
  if (accessExpiresAt === null) {
    return false;
  }
  const end = Date.parse(accessExpiresAt);
  return !Number.isNaN(end) && end - now <= marginMs;
}

interface RefreshAnswer {
  accessToken: string;
  accessTokenExpiresAt: string;
  expiresAt: string;
}

let renewal: Promise<boolean> | undefined;

/**
 * Renews the access token with the refresh token cookie; one renewal at a time per tab. Resolves
 * false when the session cannot be renewed (signed out, ended, idle) and true when renewed; a
 * network failure keeps the current token.
 */
export function renewAccessToken(): Promise<boolean> {
  renewal ??= (async () => {
    try {
      const response = await fetch('/api/v1/auth/refresh', {
        method: 'POST',
        credentials: 'same-origin',
        headers: { [REFRESH_HEADER]: 'BrokerVerse' },
      });
      if (!response.ok) {
        return false;
      }
      const answer = (await response.json()) as RefreshAnswer;
      tokenStore.set(answer.accessToken, answer.expiresAt, answer.accessTokenExpiresAt);
      return true;
    } catch {
      return true;
    } finally {
      renewal = undefined;
    }
  })();
  return renewal;
}

/** Paths that never trigger a renewal (the sign-in steps themselves). */
function signInPath(path: string): boolean {
  return (
    path.startsWith('/auth/login') ||
    path.startsWith('/auth/refresh') ||
    path.startsWith('/auth/mfa/verify') ||
    path.startsWith('/auth/mfa/enrolment') ||
    path.startsWith('/auth/sso') ||
    path.startsWith('/auth/password-reset') ||
    path.startsWith('/auth/sign-in-options')
  );
}

export interface DownloadedFile {
  blob: Blob;
  fileName: string;
}

type QueryValue = string | number | boolean | null | undefined;

export function toQuery(params: Record<string, QueryValue>): string {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      search.append(key, String(value));
    }
  });
  const text = search.toString();
  return text ? `?${text}` : '';
}

/** Fetches, turning a request that never reached the server into a network ApiError. */
async function fetchOrNetworkError(url: string, init: RequestInit): Promise<Response> {
  try {
    return await fetch(url, init);
  } catch {
    throw new ApiError(NETWORK_STATUS, {});
  }
}

/** The ApiError of a failed response, with the correlation id of the request. */
async function problemOf(response: Response): Promise<ApiError> {
  const problem = (await response.json().catch(() => ({}))) as ProblemDetail;
  problem.correlationId ??= response.headers.get('X-Correlation-Id') ?? undefined;
  return new ApiError(response.status, problem);
}

function requestInit(method: string, body?: unknown): RequestInit {
  const headers: Record<string, string> = {};
  const token = tokenStore.get();
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }
  const isForm = body instanceof FormData;
  if (body !== undefined && !isForm) {
    headers['Content-Type'] = 'application/json';
  }
  let payload: BodyInit | undefined;
  if (isForm) {
    payload = body;
  } else if (body !== undefined) {
    payload = JSON.stringify(body);
  }
  return { method, headers, body: payload };
}

async function send(method: string, path: string, body?: unknown): Promise<Response> {
  const renewable = tokenStore.get() !== null && !signInPath(path);
  if (renewable && renewalDue(tokenStore.accessExpiresAt(), Date.now())) {
    await renewAccessToken();
  }
  const url = `/api/v1${path}`;
  let response = await fetchOrNetworkError(url, requestInit(method, body));
  if (response.status === 401 && renewable && (await renewAccessToken())) {
    response = await fetchOrNetworkError(url, requestInit(method, body));
  }
  if (response.status === 401 && renewable) {
    tokenStore.clear();
    unauthorizedHandler?.();
  }
  if (!response.ok) {
    throw await problemOf(response);
  }
  return response;
}

/** Decodes an RFC 5987 ext-value such as `UTF-8''na%C3%AFve.pdf` (undefined when malformed). */
function decodeExtendedValue(value: string): string | undefined {
  const match = /^([\w!#$&+^`{}~-]+)'[^']*'(.+)$/.exec(value);
  if (match === null) {
    return undefined;
  }
  const charset = (match[1] ?? '').toUpperCase();
  const encoded = match[2] ?? '';
  try {
    if (charset === 'UTF-8') {
      return decodeURIComponent(encoded);
    }
    // ISO-8859-1, the only other charset RFC 5987 requires: one byte per character.
    return encoded.replace(/%([\da-f]{2})/gi, (_, hex: string) =>
      String.fromCodePoint(Number.parseInt(hex, 16)),
    );
  } catch {
    return undefined;
  }
}

/**
 * File name of a Content-Disposition header. The RFC 5987 `filename*` parameter (percent encoded,
 * any character) wins; the plain `filename` is only the ASCII fallback for old clients.
 */
export function fileNameOf(disposition: string | null, fallback: string): string {
  const header = disposition ?? '';
  const extended = /filename\*\s*=\s*([^;]+)/i.exec(header);
  const decoded =
    extended?.[1] === undefined ? '' : (decodeExtendedValue(extended[1].trim()) ?? '');
  return decoded || plainFileName(header) || fallback;
}

/** The plain (quoted or token) `filename` parameter, or '' when there is none. */
function plainFileName(header: string): string {
  const plain = /(?:^|;)\s*filename\s*=\s*(?:"((?:[^"\\]|\\.)*)"|([^;]*))/i.exec(header);
  if (plain === null) {
    return '';
  }
  return plain[1] === undefined ? (plain[2] ?? '').trim() : plain[1].replace(/\\(.)/g, '$1');
}

async function fileOf(response: Response, fallback: string): Promise<DownloadedFile> {
  const fileName = fileNameOf(response.headers.get('Content-Disposition'), fallback);
  return { blob: await response.blob(), fileName };
}

async function json<T>(method: string, path: string, body?: unknown): Promise<T> {
  const response = await send(method, path, body);
  if (response.status === 204) {
    return undefined as T;
  }
  // An accepted request without a body (202, e.g. a password reset request) has nothing to read.
  const text = await response.text();
  return (text.trim() === '' ? undefined : JSON.parse(text)) as T;
}

export const api = {
  get: <T>(path: string): Promise<T> => json<T>('GET', path),
  post: <T>(path: string, body?: unknown): Promise<T> => json<T>('POST', path, body ?? {}),
  put: <T>(path: string, body: unknown): Promise<T> => json<T>('PUT', path, body),
  delete: (path: string): Promise<undefined> => json<undefined>('DELETE', path),
  /** POSTs a multipart form (file uploads). */
  upload: <T>(path: string, form: FormData): Promise<T> => json<T>('POST', path, form),
  /** POSTs and returns the response body as a downloadable file. */
  download: async (path: string, body: unknown): Promise<DownloadedFile> =>
    fileOf(await send('POST', path, body), 'report'),
  /** GETs a file (attachments, templates). */
  getFile: async (path: string): Promise<DownloadedFile> =>
    fileOf(await send('GET', path), 'download'),
};

/** Event raised on window when a PDF has been saved (detail: {@link DownloadedFile}). */
export const PDF_SAVED_EVENT = 'brokerverse:pdf-saved';

/** Whether a saved file is a PDF (by type or name). */
export function isPdf(blob: Blob, fileName: string): boolean {
  return blob.type === 'application/pdf' || /\.pdf$/i.test(fileName);
}

/**
 * Saves a file in the browser. A saved PDF is announced ({@link PDF_SAVED_EVENT}) so the Word copy
 * of a generated document can be offered (client requirement 16).
 */
export function saveFile(blob: Blob, fileName: string): void {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  link.click();
  URL.revokeObjectURL(url);
  if (isPdf(blob, fileName)) {
    const detail: DownloadedFile = { blob, fileName };
    globalThis.dispatchEvent(new CustomEvent(PDF_SAVED_EVENT, { detail }));
  }
}
