import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import {
  RELOAD_FLAG,
  importWithRetry,
  installPreloadErrorHandler,
  isChunkLoadError,
  reloadOnce,
} from './loadRecovery';

const chunkError = () =>
  new TypeError('Failed to fetch dynamically imported module: /assets/UsersPage-abc123.js');

describe('loadRecovery', () => {
  beforeEach(() => window.sessionStorage.clear());
  afterEach(() => vi.restoreAllMocks());

  it('recognises errors of missing script files only', () => {
    expect(isChunkLoadError(chunkError())).toBe(true);
    expect(isChunkLoadError(new Error('Importing a module script failed.'))).toBe(true);
    expect(isChunkLoadError(new Error('Cannot read properties of undefined'))).toBe(false);
    expect(isChunkLoadError('Failed to fetch dynamically imported module')).toBe(false);
  });

  it('retries a failed import once and returns the module', async () => {
    const load = vi.fn().mockRejectedValueOnce(chunkError()).mockResolvedValueOnce({ default: 1 });
    const reload = vi.fn();
    await expect(importWithRetry(load, { delayMs: 0, reload })).resolves.toEqual({ default: 1 });
    expect(load).toHaveBeenCalledTimes(2);
    expect(reload).not.toHaveBeenCalled();
  });

  it('reloads the page once when the retry fails with a missing script file', async () => {
    const load = vi.fn().mockRejectedValue(chunkError());
    const reload = vi.fn();
    const pending = importWithRetry(load, { delayMs: 0, reload });
    await vi.waitFor(() => expect(reload).toHaveBeenCalledTimes(1));
    expect(load).toHaveBeenCalledTimes(2);
    expect(window.sessionStorage.getItem(RELOAD_FLAG)).not.toBeNull();
    void pending;
  });

  it('does not reload a second time, so the error message shows instead of a loop', async () => {
    window.sessionStorage.setItem(RELOAD_FLAG, '1');
    const reload = vi.fn();
    const load = vi.fn().mockRejectedValue(chunkError());
    await expect(importWithRetry(load, { delayMs: 0, reload })).rejects.toThrow(
      'dynamically imported module',
    );
    expect(reload).not.toHaveBeenCalled();
  });

  it('passes other errors on without a reload', async () => {
    const reload = vi.fn();
    const load = vi.fn().mockRejectedValue(new Error('broken page'));
    await expect(importWithRetry(load, { delayMs: 0, reload })).rejects.toThrow('broken page');
    expect(load).toHaveBeenCalledTimes(2);
    expect(reload).not.toHaveBeenCalled();
  });

  it('keeps the reload guard for a minute after a reload, whatever else loads', async () => {
    window.sessionStorage.setItem(RELOAD_FLAG, String(Date.now()));
    await importWithRetry(() => Promise.resolve({ default: 1 }), { delayMs: 0 });
    expect(window.sessionStorage.getItem(RELOAD_FLAG)).not.toBeNull();
  });

  it('clears the reload guard after a successful load later on', async () => {
    window.sessionStorage.setItem(RELOAD_FLAG, '1');
    await importWithRetry(() => Promise.resolve({ default: 1 }), { delayMs: 0 });
    expect(window.sessionStorage.getItem(RELOAD_FLAG)).toBeNull();
  });

  it('does not reload when the session storage is blocked', () => {
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new Error('blocked');
    });
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('blocked');
    });
    const reload = vi.fn();
    expect(reloadOnce(reload)).toBe(false);
    expect(reload).not.toHaveBeenCalled();
  });

  it('reloads once on a Vite preload error', () => {
    const reload = vi.fn();
    const remove = installPreloadErrorHandler(window, reload);
    const first = new Event('vite:preloadError', { cancelable: true });
    window.dispatchEvent(first);
    window.dispatchEvent(new Event('vite:preloadError', { cancelable: true }));
    remove();
    expect(reload).toHaveBeenCalledTimes(1);
    expect(first.defaultPrevented).toBe(true);
  });
});
