/**
 * Thin, typed HTTP client for the FinVerse REST API.
 *
 * - Attaches the bearer token.
 * - Converts RFC 7807 problem responses into {@link ApiError} with the stable backend `code`.
 * - Signals an expired session through {@link onUnauthorized} so the app can return to login.
 */

const TOKEN_KEY = 'finverse.token';

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

export const tokenStore = {
  get: (): string | null => sessionStorage.getItem(TOKEN_KEY),
  set: (token: string): void => {
    sessionStorage.setItem(TOKEN_KEY, token);
  },
  clear: (): void => {
    sessionStorage.removeItem(TOKEN_KEY);
  },
};

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
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
  }
  const response = await fetch(`/api/v1${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  });
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
  /** POSTs and returns the response body as a downloadable file. */
  download: async (path: string, body: unknown): Promise<{ blob: Blob; fileName: string }> => {
    const response = await send('POST', path, body);
    const disposition = response.headers.get('Content-Disposition') ?? '';
    const match = /filename="?([^";]+)"?/.exec(disposition);
    return { blob: await response.blob(), fileName: match?.[1] ?? 'report' };
  },
};

export function saveFile(blob: Blob, fileName: string): void {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  link.click();
  URL.revokeObjectURL(url);
}
