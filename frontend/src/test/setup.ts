import '@testing-library/jest-dom/vitest';
import { afterEach } from 'vitest';

// The column choices of the lists are kept in the browser storage: every test starts from the
// default columns.
afterEach(() => {
  window.localStorage.clear();
});
