let checkSolutionBtn;
let nextFlashcardBtn;
let progressDiv;
let correctionTextPar;
let currentFlashcard = null;
let flashcards = [];
let checkSolutionUrl;
let canSolve = true;
let maxNumOfCards = 10;
let answeredFlashcards = 0;
function readSolution(cardType) {
    let solutions = [];
    let selectedAnswers = [];
    switch (cardType) {
        case 'Word':
        case 'Text':
            const solutionInputs = Array.from(document.querySelectorAll('.translation_input'));
            solutions = solutionInputs
                .map((input, index) => {
                return {
                    id: index,
                    solution: input.value.trim()
                };
            })
                .filter((solution) => solution.solution.length !== 0);
            if (solutions.length === 0) {
                alert('Sie können keine leere Lösung abgeben!');
                return null;
            }
            break;
        case 'Blank':
            throw cardType;
        case 'Choice':
            selectedAnswers = Array.from(document.querySelectorAll('input[name=choice_answers]'))
                .filter((element) => element.checked)
                .map((element) => parseInt(element.dataset.choiceid));
            if (selectedAnswers.length === 0) {
                alert('Bitte wählen Sie mindestens eine Antwort aus.');
                return null;
            }
            break;
        default:
            console.error('There has been an internal error: ' + cardType);
            return undefined;
    }
    return {
        cardId: currentFlashcard.flashcard.cardId,
        collId: currentFlashcard.flashcard.collId,
        courseId: currentFlashcard.flashcard.courseId,
        solutions,
        selectedAnswers,
        frontToBack: currentFlashcard.frontToBack
    };
}
function onCorrectionSuccess(result, cardType) {
    console.info(JSON.stringify(result, null, 2));
    let correctionText = `Ihre Lösung war ${result.correct ? '' : 'nicht '} korrekt.`;
    if ((result.newTriesCount >= 2) && (result.maybeSampleSolution != null)) {
        correctionText += ` Die korrekte Lösung lautet '<code>${result.maybeSampleSolution}</code>'.`;
    }
    canSolve = !(result.correct || result.newTriesCount >= 2);
    correctionTextPar.innerHTML = correctionText;
    correctionTextPar.classList.remove(result.correct ? 'red-text' : 'green-text');
    correctionTextPar.classList.add(result.correct ? 'green-text' : 'red-text');
    checkSolutionBtn.disabled = !canSolve;
    nextFlashcardBtn.disabled = canSolve;
    if (!canSolve) {
        answeredFlashcards++;
        progressDiv.style.width = `${answeredFlashcards / maxNumOfCards * 100}%`;
        document.querySelector('#answeredFcCountSpan').innerHTML = answeredFlashcards.toString();
        if (currentFlashcard.flashcard.cardType in ['Text', 'Word']) {
            document.querySelectorAll('.translation_input').forEach((translationInput) => translationInput.disabled = true);
        }
        if (answeredFlashcards >= maxNumOfCards) {
            nextFlashcardBtn.onclick = () => {
                window.location.href = nextFlashcardBtn.dataset['endurl'];
            };
            nextFlashcardBtn.textContent = "Bearbeiten beenden";
        }
    }
    document.querySelector('#triesSpan').innerText = result.newTriesCount.toString();
    switch (cardType) {
        case 'Word':
        case 'Text':
            result.matchingResult.matches.forEach((match) => {
                const translationInput = document.querySelector(`#translation_input_${match.start.id}`);
                const isCorrect = match.distance === 0;
                translationInput.classList.remove(isCorrect ? 'invalid' : 'valid');
                translationInput.classList.add(isCorrect ? 'valid' : 'invalid');
            });
            break;
        case 'Blank':
            document.querySelectorAll('.translation_input').forEach((textInput) => {
                textInput.classList.remove(result.correct ? 'invalid' : 'valid');
                textInput.classList.add(result.correct ? 'valid' : 'invalid');
            });
            break;
        case 'Choice':
            console.error(JSON.stringify(result.answersSelection));
            break;
        default:
            console.error(cardType);
    }
    nextFlashcardBtn.focus();
}
function initialLoadNextFlashcards(loadFlashcardUrl) {
    fetch(loadFlashcardUrl).then(response => {
        if (response.status === 200) {
            response.json().then((loadedFlashcard) => {
                flashcards = loadedFlashcard;
                loadNextFlashcard();
            });
        }
        else if (response.status === 404) {
            alert("Sie haben alle Karteikarten abgearbeitet.");
            window.location.href = '/';
        }
    });
}
function loadNextFlashcard() {
    if (flashcards.length > 0) {
        [currentFlashcard, ...flashcards] = flashcards;
        canSolve = true;
        checkSolutionBtn.disabled = !canSolve;
        nextFlashcardBtn.disabled = canSolve;
        correctionTextPar.innerHTML = '&nbsp;';
        updateView(currentFlashcard);
    }
    else {
        throw Error('No flashcards loaded...');
    }
}
function checkSolution() {
    const solution = readSolution(currentFlashcard.flashcard.cardType);
    if (solution === null) {
        alert("Sie können keine leere Lösung abgeben!");
        return;
    }
    const headers = new Headers({
        'Content-Type': 'application/json',
        'Accept': 'application/json',
        'Csrf-Token': document.querySelector('input[name="csrfToken"]').value
    });
    fetch(checkSolutionUrl, { method: 'PUT', body: JSON.stringify(solution), headers })
        .then((response) => {
        if (response.status === 200) {
            return response.json()
                .then(obj => onCorrectionSuccess(obj, currentFlashcard.flashcard.cardType));
        }
        else {
            response.text().then(text => console.error(text));
            return Promise.reject("Error code was " + response.status);
        }
    })
        .catch(reason => {
        console.error(reason);
    });
}
function updateReadQuestionButton(readQuestionButton) {
    const numOfFoundVoices = window.speechSynthesis.getVoices().length;
    if (numOfFoundVoices > 0) {
        console.info("Found " + numOfFoundVoices + " voices!");
        readQuestionButton.classList.remove('hide');
        readQuestionButton.disabled = false;
        readQuestionButton.onclick = () => {
            const toRead = currentFlashcard.frontToBack ? currentFlashcard.flashcard.fronts : currentFlashcard.flashcard.backs;
            toRead.forEach((front) => {
                const utterance = new SpeechSynthesisUtterance(front);
                utterance.lang = 'fr';
                window.speechSynthesis.speak(utterance);
            });
        };
    }
    else {
        console.warn("Could not find any voices for Speech synthesis!");
    }
}
domReady(() => {
    const initialLoadBtn = document.querySelector('#loadFlashcardButton');
    const loadFlashcardUrl = initialLoadBtn.dataset['href'];
    initialLoadBtn.onclick = () => {
        initialLoadNextFlashcards(loadFlashcardUrl);
        initialLoadBtn.remove();
    };
    initialLoadBtn.click();
    correctionTextPar = document.querySelector('#correctionTextPar');
    const readQuestionButton = document.querySelector('#readQuestionButton');
    updateReadQuestionButton(readQuestionButton);
    window.speechSynthesis.onvoiceschanged = () => {
        updateReadQuestionButton(readQuestionButton);
    };
    window.speechSynthesis.getVoices();
    nextFlashcardBtn = document.querySelector('#nextFlashcardBtn');
    nextFlashcardBtn.onclick = () => loadNextFlashcard();
    checkSolutionBtn = document.querySelector('#checkSolutionBtn');
    checkSolutionBtn.onclick = checkSolution;
    checkSolutionUrl = checkSolutionBtn.dataset['href'];
    maxNumOfCards = parseInt(document.querySelector('#maxCardCountSpan').innerText);
    progressDiv = document.querySelector('#progressDiv');
    document.addEventListener('keypress', event => {
        if (event.key === 'Enter') {
            if (event.ctrlKey) {
                readQuestionButton.click();
            }
            else if (canSolve) {
                checkSolutionBtn.click();
            }
            else {
                nextFlashcardBtn.click();
            }
        }
        else if (currentFlashcard.flashcard.cardType === 'Choice' && canSolve) {
            const pressedKey = parseInt(event.key);
            const choiceParagraph = document.querySelector('#answerDiv')
                .querySelectorAll('p.choiceParagraph')
                .item(pressedKey - 1);
            if (choiceParagraph !== null) {
                choiceParagraph.querySelector('input').click();
            }
        }
    });
});
//# sourceMappingURL=/assets/learnRepeat.js.map