import './styles/tokens.css';
import '@theme-pack';
import './styles/base.css';
import './styles/components.css';
import './styles/broking.css';
import './styles/crm.css';
import './styles/placement.css';
import './styles/quotation.css';
import './styles/patterns.css';
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
