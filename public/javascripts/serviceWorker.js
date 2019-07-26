self.addEventListener('install', event => {
    console.warn('Installing service worker...' + event);
});
self.addEventListener('fetch', event => {
    console.info(event);
});
//# sourceMappingURL=/assets/serviceWorker.js.map