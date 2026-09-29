// iNXT BrokerVerse frontend lint rules.
// eslint-plugin-sonarjs is SonarSource's own rule set (the same rules SonarQube runs on
// JavaScript/TypeScript), so `npm run lint` is the local SonarQube quality gate.
import js from '@eslint/js';
import jsxA11y from 'eslint-plugin-jsx-a11y';
import reactHooks from 'eslint-plugin-react-hooks';
import reactRefresh from 'eslint-plugin-react-refresh';
import sonarjs from 'eslint-plugin-sonarjs';
import globals from 'globals';
import tseslint from 'typescript-eslint';

export default tseslint.config(
  { ignores: ['dist', 'coverage', 'node_modules'] },
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      js.configs.recommended,
      ...tseslint.configs.strictTypeChecked,
      ...tseslint.configs.stylisticTypeChecked,
      sonarjs.configs.recommended,
      jsxA11y.flatConfigs.strict,
    ],
    languageOptions: {
      ecmaVersion: 2022,
      globals: globals.browser,
      parserOptions: {
        projectService: { allowDefaultProject: ['eslint.config.js'] },
        tsconfigRootDir: import.meta.dirname,
      },
    },
    plugins: {
      'react-hooks': reactHooks,
      'react-refresh': reactRefresh,
    },
    rules: {
      ...reactHooks.configs.recommended.rules,
      'react-refresh/only-export-components': ['error', { allowConstantExport: true }],
      // Cyclomatic limit 12 is stricter than the Sonar way profile, which gates on cognitive
      // complexity (15, below); ESLint also counts ?? and ?. as branches.
      complexity: ['error', 12],
      'max-lines': ['error', { max: 400, skipBlankLines: true, skipComments: true }],
      'max-depth': ['error', 3],
      'no-console': 'error',
      eqeqeq: 'error',
      'sonarjs/cognitive-complexity': ['error', 15],
      '@typescript-eslint/restrict-template-expressions': ['error', { allowNumber: true }],
      '@typescript-eslint/no-confusing-void-expression': ['error', { ignoreArrowShorthand: true }],
      // A link address that is not a literal must pass through safeUrl (web addresses only), so a
      // value from the server or a list of values can never become a javascript: or data: link.
      'no-restricted-syntax': [
        'error',
        {
          selector:
            "JSXAttribute[name.name='href'] > JSXExpressionContainer > :not(Literal, CallExpression[callee.name='safeUrl'])",
          message: 'Pass a dynamic href through safeUrl() from @/utils/safeUrl.',
        },
      ],
    },
  },
  {
    // Client neutrality (platform code): no client names and no currency codes in the screens.
    // The client's names come from its company master (useClientNames) or its theme pack, the
    // currency from the company (useBaseCurrency) or the currency master. Tests, fixtures and
    // the theme packs are exempt; backend ClientNeutralityTest checks the same on every build.
    files: ['src/**/*.{ts,tsx}'],
    ignores: [
      '**/*.test.{ts,tsx}',
      'src/test/**',
      '**/*[fF]ixtures.ts',
      '**/testWrapper.tsx',
      'src/theme/packs/**',
    ],
    rules: {
      'no-restricted-syntax': [
        'error',
        {
          selector: 'Literal[value=/\\bBDOI?\\b/]',
          message: 'No client name in platform code: use useClientNames/groupLabel or the theme pack.',
        },
        {
          selector: 'TemplateElement[value.raw=/\\bBDOI?\\b/]',
          message: 'No client name in platform code: use useClientNames/groupLabel or the theme pack.',
        },
        {
          selector: 'JSXText[value=/\\bBDOI?\\b/]',
          message: 'No client name in platform code: use useClientNames/groupLabel or the theme pack.',
        },
        {
          selector:
            'Literal[value=/\\b(PHP|USD|EUR|GBP|JPY|SGD|HKD|AUD|CAD|CHF|CNY)\\b/]',
          message: 'No currency code in the screens: use useBaseCurrency or the currency master.',
        },
        {
          selector: 'JSXText[value=/\\b(PHP|USD|EUR|GBP|JPY|SGD|HKD|AUD|CAD|CHF|CNY)\\b/]',
          message: 'No currency code in the screens: use useBaseCurrency or the currency master.',
        },
      ],
    },
  },
  {
    files: ['**/*.test.{ts,tsx}', 'src/test/**'],
    rules: {
      'sonarjs/no-hardcoded-passwords': 'off',
      '@typescript-eslint/no-non-null-assertion': 'off',
    },
  },
);
