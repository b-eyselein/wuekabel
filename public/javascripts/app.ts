if ('serviceWorker' in navigator) {
    console.warn('Installing service worker...');
    navigator.serviceWorker.register('service-worker.js', {scope: '/'})
        .then(registration => console.log(registration))
        .catch(error => console.error(error));

}