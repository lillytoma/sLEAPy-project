// sockjs-client expects the Node-style global name in some browser bundles.
(globalThis as typeof globalThis & { global?: typeof globalThis }).global = globalThis;
