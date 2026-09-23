import '@fontsource/montserrat/600.css';
import '@fontsource/montserrat/700.css';
import '@fontsource/montserrat/800.css';
import '@fontsource/open-sans/400.css';
import '@fontsource/open-sans/600.css';
import './styles/tokens.css';
import './styles/base.css';
import './styles/components.css';
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { App } from './App';

const root = document.getElementById('root');
if (root === null) {
  throw new Error('Root element #root is missing from index.html');
}
createRoot(root).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
