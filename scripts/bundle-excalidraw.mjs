#!/usr/bin/env node
/**
 * Pre-bundle @excalidraw/excalidraw for shadow-cljs / Closure Compiler.
 *
 * Excalidraw 0.18 ships ESM-only builds that use import.meta, which Closure
 * cannot transpile. esbuild rewrites that into a CJS bundle that shadow can
 * consume, with React left as an external peer.
 *
 * Mermaid (diagram → drawing) is stubbed out — we don't use that Excalidraw
 * feature and it pulls a ~10MB dependency graph that breaks shadow-cljs
 * package-exports resolution (@upsetjs/venn.js).
 */
import * as esbuild from 'esbuild';
import { mkdirSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const outfile = join(root, 'target/vendor/excalidraw.cjs.js');

mkdirSync(dirname(outfile), { recursive: true });

const stubMermaid = {
  name: 'stub-mermaid',
  setup(build) {
    build.onResolve(
      { filter: /^(mermaid|@excalidraw\/mermaid-to-excalidraw)(\/.*)?$/ },
      (args) => ({ path: args.path, namespace: 'mermaid-stub' }),
    );
    build.onLoad({ filter: /.*/, namespace: 'mermaid-stub' }, () => ({
      contents: 'module.exports = {};',
      loader: 'js',
    }));
  },
};

await esbuild.build({
  entryPoints: ['@excalidraw/excalidraw'],
  bundle: true,
  format: 'cjs',
  platform: 'browser',
  outfile,
  // Keep React as peer deps so the app's single React copy is used.
  external: ['react', 'react-dom', 'react/jsx-runtime', 'react/jsx-dev-runtime'],
  plugins: [stubMermaid],
  logLevel: 'info',
  // Workers / import.meta.url get inlined or stubbed by esbuild for CJS.
  define: {
    'import.meta.url': '""',
  },
});

console.log(`Wrote ${outfile}`);
