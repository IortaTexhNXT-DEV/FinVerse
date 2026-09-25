/**
 * Thin, typed HTTP client for the BrokerVerse REST API.
 *
 * - Attaches the bearer token.
 * - Converts RFC 7807 problem responses into {@link ApiError} with the stable backend `code`.
 * - Signals an expired session through {@link onUnauthorized} so the app can return to login.
 */

const TOKEN_KEY = 'brokerverse.token';
const EXPIRES_KEY = 'brokerverse.expiresAt';

export interface ProblemDetail {
  title?: string;
  detail?: string;
  status?: number;
  code?: string;
  errors?: Record<string, string>;
}

export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly fieldErrors: Record<string, string>;

  constructor(status: number, problem: ProblemDetail) {
    super(problem.detail ?? problem.title ?? `Request failed (${status})`);
    this.name = 'ApiError';
    this.status = status;
    this.code = problem.code ?? 'UNKNOWN';
    this.fieldErrors = problem.errors ?? {};
  }
}

let unauthorizedHandler: (() => void) | undefined;

export function onUnauthorized(handler: () => void): void {
  unauthorizedHandler = handler;
}

/**
 * The bearer token and its absolute expiry, kept per tab in sessionStorage (never localStorage);
 * other tabs obtain them through the session handshake (session/tabSync.ts, BRNB.082).
 */
export const tokenStore = {
  get: (): string | null => sessionStorage.getItem(TOKEN_KEY),
  expiresAt: (): string | null => sessionStorage.getItem(EXPIRES_KEY),
  set: (token: string, expiresAt?: string): void => {
    sessionStorage.setItem(TOKEN_KEY, token);
    if (expiresAt === undefined) {
      sessionStorage.removeItem(EXPIRES_KEY);
    } else {
      sessionStorage.setItem(EXPIRES_KEY, expiresAt);
    }
  },
  clear: (): void => {
    sessionStorage.removeItem(TOKEN_KEY);
    sessionStorage.removeItem(EXPIRES_KEY);
  },
};

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

async function send(method: string, path: string, body?: unknown): Promise<Response> {
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
  const response = await fetch(`/api/v1${path}`, { method, headers, body: payload });
  if (response.status === 401 && token) {
    tokenStore.clear();
    unauthorizedHandler?.();
  }
  if (!response.ok) {
    const problem = (await response.json().catch(() => ({}))) as ProblemDetail;
    throw new ApiError(response.status, problem);
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
  return (await response.json()) as T;
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
