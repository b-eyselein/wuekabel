// FIXME: use https://gist.github.com/tiernan/c18a380935e45a6d942ac1e88c5bbaf3

self.addEventListener('install', event => {
    console.warn('Installing service worker...' + event);
});

self.addEventListener('fetch', event => {
    console.info(event);
});
