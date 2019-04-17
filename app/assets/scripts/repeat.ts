/// <reference path="helpers.ts"/>
/// <reference path="questionMaker.ts"/>

let correctionTextPar: HTMLParagraphElement;

let checkSolutionBtn: HTMLButtonElement;
let nextFlashcardBtn: HTMLButtonElement;

let initialLoadBtn: HTMLButtonElement;

let checkSolutionUrl: string;

let canSolve: boolean = true;

let flashcard: Flashcard;

let repeatedFlashcards: number = 0;

function readSolution(cardType: CardType): undefined | Solution {
    let solution: string = '';
    let selectedAnswers: number[] = [];

    switch (cardType) {
        case 'Vocable':
        case 'Text':
            solution = document.querySelector<HTMLInputElement>('#translation_input').value;

            if (solution.length === 0) {
                return null;
            }
            break;

        case 'Blank':
            throw cardType;
        // return {solution: '', selectedAnswers: []};

        // break;

        case 'Choice':
            selectedAnswers = Array.from(document.querySelectorAll<HTMLInputElement>('input[name=choice_answers]'))
                .filter((element: HTMLInputElement) => element.checked)
                .map((element: HTMLInputElement) => parseInt(element.dataset.choiceid));

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
        cardId: flashcard.cardId,
        collId: flashcard.collId,
        courseId: flashcard.courseId,
        solution,
        selectedAnswers
    };
}

function onCorrectionSuccess(result: CorrectionResult, cardType: CardType): void {
    console.info(JSON.stringify(result, null, 2));

    let correctionText = `Ihre Lösung war ${result.correct ? '' : 'nicht '} korrekt.`;

    if ((result.newTriesCount >= 2) && (result.maybeSampleSol != null)) {
        correctionText += ` Die korrekte Lösung lautet '<code>${result.maybeSampleSol}</code>'.`;
    }

    canSolve = !(result.correct || result.newTriesCount >= 2);

    correctionTextPar.innerHTML = correctionText;
    correctionTextPar.classList.remove(result.correct ? 'red-text' : 'green-text');
    correctionTextPar.classList.add(result.correct ? 'green-text' : 'red-text');

    // Update buttons
    checkSolutionBtn.disabled = !canSolve;
    nextFlashcardBtn.disabled = canSolve;

    document.querySelector<HTMLSpanElement>('#triesSpan').innerText = result.newTriesCount.toString();

    switch (cardType) {
        case 'Vocable':
        case 'Text':
        case 'Blank':
            const textInput = document.querySelector<HTMLInputElement>('#translation_input');
            textInput.classList.remove(result.correct ? 'invalid' : 'valid');
            textInput.classList.add(result.correct ? 'valid' : 'invalid');
            break;
        case 'Choice':
            console.error(JSON.stringify(result.answersSelection));
            break;
        default:
            console.error(cardType);
    }
}

function checkSolution(): void {
    const solution: Solution = readSolution(flashcard.cardType);

    if (solution === null) {
        alert("Sie können keine leere Lösung abgeben!");
        return;
    }

    const headers: Headers = new Headers({
        'Content-Type': 'application/json',
        'Accept': 'application/json',
        'Csrf-Token': document.querySelector<HTMLInputElement>('input[name="csrfToken"]').value
    });

    fetch(checkSolutionUrl, {method: 'PUT', body: JSON.stringify(solution), headers})
        .then((response: Response) => {
                if (response.status === 200) {
                    return response.json();
                } else {
                    response.text().then(text => console.error(text));
                    return Promise.reject("Error code was " + response.status);
                }
            }
        )
        .then(obj => onCorrectionSuccess(obj, flashcard.cardType))
        .catch(reason => {
            console.error(reason)
        });
}

function loadNextFlashcard(): void {
    if (flashcard !== undefined && repeatedFlashcards++ > 10) {
        console.warn(repeatedFlashcards);
    }

    const loadFlashcardUrl: string = initialLoadBtn.dataset['href'];
    fetch(loadFlashcardUrl)
        .then(response => {
                if (response.status === 200) {
                    return response.json().then(loadedFlashcard => {
                        flashcard = loadedFlashcard;

                        canSolve = true;

                        // Update buttons
                        checkSolutionBtn.disabled = !canSolve;
                        nextFlashcardBtn.disabled = canSolve;

                        correctionTextPar.innerHTML = '&nbsp;';

                        updateView(flashcard);
                    });
                } else if (response.status === 404) {
                    // FIXME Keine weitere Karteikarte mehr...
                    alert("Sie haben alle Karteikarten wiederholt...");
                    window.location.href = '/';
                }
            }
        )
        .catch(reason => console.error(reason));
}

function performInitialFlashcardLoad(): void {
    initialLoadBtn.remove();
    loadNextFlashcard();
}

domReady(() => {
    initialLoadBtn = document.querySelector<HTMLButtonElement>('#loadFlashcardButton');
    initialLoadBtn.click();

    correctionTextPar = document.querySelector<HTMLParagraphElement>('#correctionTextPar');
    nextFlashcardBtn = document.querySelector<HTMLButtonElement>('#nextFlashcardBtn');
    checkSolutionBtn = document.querySelector<HTMLButtonElement>('#checkSolutionBtn');

    checkSolutionUrl = checkSolutionBtn.dataset['href'];

    document.addEventListener('keypress', event => {
        if (event.key === 'Enter') {
            if (canSolve) {
                checkSolution();
            } else {
                document.getElementById('nextFlashcardBtn').click();
            }
        }
    });
});
