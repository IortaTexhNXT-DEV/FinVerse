/// <reference types="vitest/config" />
import { readFileSync } from 'node:fs';
import { fileURLToPath, URL } from 'node:url';
import react from '@vitejs/plugin-react';
import { defineConfig, type Plugin } from 'vite';

// Theme pack of the deployment (src/theme/packs/<pack>): branding, colours, fonts and logo of the
// client. Chosen at build time with VITE_THEME_PACK; "bdoi" unless set.
const themePack = process.env.VITE_THEME_PACK ?? 'bdoi';
if (!/^[a-z0-9-]+$/.test(themePack)) {
  throw new Error(`VITE_THEME_PACK must be a pack folder name: ${themePack}`);
}
const packDir = fileURLToPath(new URL(`./src/theme/packs/${themePack}/`, import.meta.url));

/** Fills the title, description and icon of index.html from the theme pack. */
function themeHtml(): Plugin {
  const theme = JSON.parse(readFileSync(`${packDir}theme.json`, 'utf8')) as {
    product: string;
    productName: string;
    description: string;
  };
  const icon = readFileSync(`${packDir}favicon.svg`).toString('base64');
  const escape = (text: string) =>
    text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/"/g, '&quot;');
  return {
    name: 'theme-pack-html',
    transformIndexHtml: (html) =>
      html
        .replace('__THEME_TITLE__', escape(`${theme.product} – ${theme.productName}`))
        .replace('__THEME_DESCRIPTION__', escape(theme.description))
        .replace('__THEME_ICON__', `data:image/svg+xml;base64,${icon}`),
  };
}

// Development server proxies /api to the Spring Boot backend so the browser sees one origin.
export default defineConfig({
  plugins: [react(), themeHtml()],
  resolve: {
    alias: {
      '@theme-pack': `${packDir}index.ts`,
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: process.env.BROKERVERSE_API_URL ?? 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  build: {
    sourcemap: false,
    chunkSizeWarningLimit: 900,
  },
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    css: false,
    coverage: {
      provider: 'v8',
      reporter: ['text-summary', 'lcov'],
      include: ['src/**/*.{ts,tsx}'],
      exclude: ['src/main.tsx', 'src/test/**', 'src/**/*.d.ts'],
    },
  },
});
