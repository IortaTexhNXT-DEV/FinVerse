import { lazy, type ComponentType, type LazyExoticComponent } from 'react';

/**
 * Recovery from screens that fail to load. After a new release the browser may still hold the old
 * index.html, whose script files no longer exist on the server; a short network drop has the same
 * effect. A failed load is retried once; when it fails again because a script file is missing, the
 * page reloads once to pick up the new release. A session flag stops a reload loop.
 */

export const RELOAD_FLAG = 'bibs.loadRecovery.reloaded';
export const RETRY_DELAY_MS = 800;

const CHUNK_ERROR_PATTERN =
  /Failed to fetch dynamically imported module|error loading dynamically imported module|Importing a module script failed|Unable to preload CSS|Loading (CSS )?chunk [\w-]+ failed|ChunkLoadError/i;

/** True when the error says a script or style file of the application could not be loaded. */
export function isChunkLoadError(error: unknown): boolean {
  if (!(error instanceof Error)) {
    return false;
  }
  return error.name === 'ChunkLoadError' || CHUNK_ERROR_PATTERN.test(error.message);
}

function readFlag(): string | null {
  try {
    return window.sessionStorage.getItem(RELOAD_FLAG);
  } catch {
    return null;
  }
}

function writeFlag(value: string | null): void {
  try {
    if (value === null) {
      window.sessionStorage.removeItem(RELOAD_FLAG);
    } else {
      window.sessionStorage.setItem(RELOAD_FLAG, value);
    }
  } catch {
    // Storage blocked: no automatic reload, the error message offers Reload instead.
  }
}

/**
 * Reloads the page once per browser tab session. Returns false when a reload was already tried
 * (or storage is unavailable), so the caller shows the error message instead.
 */
export function reloadOnce(reload: () => void = () => window.location.reload()): boolean {
  if (readFlag() !== null) {
    return false;
  }
  writeFlag(String(Date.now()));
  if (readFlag() === null) {
    return false;
  }
  reload();
  return true;
}

/** Clears the reload guard after a screen loaded, so a later release can recover the same way. */
export function markLoadSucceeded(): void {
  writeFlag(null);
}

const wait = (ms: number) =>
  new Promise<void>((resolve) => {
    setTimeout(resolve, ms);
  });

/**
 * Runs a dynamic import, retries it once after a short delay and reloads the page once when the
 * second attempt fails with a missing script file. The error is passed on otherwise, so the error
 * boundary shows it.
 */
export async function importWithRetry<T>(
  load: () => Promise<T>,
  options: { delayMs?: number; reload?: () => void } = {},
): Promise<T> {
  try {
    const module = await load();
    markLoadSucceeded();
    return module;
  } catch {
    await wait(options.delayMs ?? RETRY_DELAY_MS);
  }
  try {
    const module = await load();
    markLoadSucceeded();
    return module;
  } catch (error) {
    if (isChunkLoadError(error) && reloadOnce(options.reload)) {
      // The page is reloading: keep the loading state until the browser navigates away.
      return new Promise<T>(() => undefined);
    }
    throw error;
  }
}

/** React.lazy with the retry and reload of {@link importWithRetry}. */
export function lazyWithRetry<P extends object>(
  load: () => Promise<{ default: ComponentType<P> }>,
): LazyExoticComponent<ComponentType<P>> {
  return lazy(() => importWithRetry(load));
}

/**
 * Vite reports a failed preload of a screen's files with 'vite:preloadError'; the page reloads once
 * the same way. Returns a function that removes the handler.
 */
export function installPreloadErrorHandler(
  target: Window = window,
  reload?: () => void,
): () => void {
  const handler = (event: Event) => {
    if (reloadOnce(reload)) {
      event.preventDefault();
    }
  };
  target.addEventListener('vite:preloadError', handler);
  return () => target.removeEventListener('vite:preloadError', handler);
}
