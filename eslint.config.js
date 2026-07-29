const js = require('@eslint/js');
const globals = require('globals');

module.exports = [
  {
    ignores: [
      'node_modules/',
      'resources/public/js/compiled/',
      'resources/public/js/lexical/',
      'target/',
      'out/',
      '.shadow-cljs/',
      '.cpcache/',
    ],
  },
  js.configs.recommended,
  {
    files: ['**/*.{js,mjs,cjs}'],
    languageOptions: {
      ecmaVersion: 2022,
      sourceType: 'module',
      globals: { ...globals.node, ...globals.browser },
    },
  },
  {
    files: ['**/*.cjs', 'eslint.config.js'],
    languageOptions: {
      sourceType: 'commonjs',
    },
  },
];
