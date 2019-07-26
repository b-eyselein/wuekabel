function shuffleArray(array) {
    let newArray = array.slice(0);
    for (let i = newArray.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [newArray[i], newArray[j]] = [newArray[j], newArray[i]];
    }
    return newArray;
}
function domReady(callBack) {
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', callBack);
    }
    else {
        callBack();
    }
}
//# sourceMappingURL=/assets/learnRepeatBasics.js.map