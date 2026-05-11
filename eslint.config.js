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
    files: ['**/*.js'],
    languageOptions: {
      ecmaVersion: 2022,
      sourceType: 'commonjs',
      globals: { ...globals.node, ...globals.browser },
    },
  },
];
