// FIXME: use https://github.com/Microsoft/TypeScript/blob/b8def16e92f609327971f07232757fa6c7d29a56/lib/lib.webworker.d.ts

self.addEventListener('install', event => {
    console.warn('Installing service worker...' + event);
});

self.addEventListener('fetch', event => {
    console.info(event);
});
