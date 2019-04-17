/// <reference path="interfaces.ts"/>

let correctionTextPar: HTMLParagraphElement;
let checkSolutionBtn: HTMLButtonElement;
let checkSolutionUrl: string;
let canSolve: boolean = true;

function readSolution(cardType: CardType): undefined | Solution {
    switch (cardType) {
        case 'Vocable':
        case 'Text':
            const solution: string = document.querySelector<HTMLInputElement>('#translation_input').value;

            if (solution.length === 0) {
                return null;
            }

            return {solution, selectedAnswers: []};

        case 'Blank':
            throw cardType;
        // return {solution: '', selectedAnswers: []};

        case 'Choice':
            const selectedAnswers: number[] = [];

            document.querySelectorAll<HTMLInputElement>('input[name=choice_answers]').forEach(
                (element: HTMLElement) => {
                    if (element instanceof HTMLInputElement && element.checked) {
                        selectedAnswers.push(parseInt(element.dataset.choiceid));
                    }
                });

            if (selectedAnswers.length === 0) {
                return null;
            }

            return {solution: "", selectedAnswers};
        default:
            console.error('There has been an internal error: ' + cardType);
            return undefined;
    }
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

    checkSolutionBtn.disabled = !canSolve;

    if (result.correct || result.newTriesCount >= 2) {
        document.querySelector('#nextFlashcardBtn').classList.remove('disabled');
    }

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

    const cardType: CardType = document.querySelector<HTMLDivElement>('#flashcardDiv').dataset['cardtype'] as CardType;

    const solution: Solution = readSolution(cardType);

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
        .then(obj => onCorrectionSuccess(obj, cardType))
        .catch(reason => {
            console.error(reason)
        });
}

function domReady(callBack: () => void): void {
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', callBack);
    } else {
        callBack();
    }
}

domReady(() => {
    correctionTextPar = document.querySelector<HTMLParagraphElement>('#correctionTextPar');
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
