interface LoadedFlashcardAnswer {
    id: number
    flashcardId: number
    collId: number
    langId: number
    answer: string
    correctness: 'CORRECT' | 'OPTIONAL' | 'WRONG'
}

interface LoadedFlashcard {
    id: number
    collId: number
    langId: number
    cardType: 'vocable' | 'single_choice' | 'multiple_choice'
    question: string
    meaning: string | null
    answers: LoadedFlashcardAnswer[]
}

let flashcards: LoadedFlashcard[] = [];
let currentFlashcard: LoadedFlashcard | null = null;

let flashcardDiv: JQuery<HTMLElement>;

function buildInputForVocable(/*vocableFlashcard: LoadedFlashcard*/): string {
    return `
<div class="row">
    <div class="input-field col s12">
        <input type="text" id="translation_input">
        <label for="translation_input">Übersetzung</label>
    </div>
</div>`.trim();
}

function buildInputForChoiceFlashcard(choiceFlashcard: LoadedFlashcard): string {

    const inputType: string = choiceFlashcard.cardType == 'single_choice' ? 'radio' : 'checkbox';

    let answerInputs: string[] = [];
    for (const answer of choiceFlashcard.answers) {
        answerInputs.push(`
<p>
    <label for="choice_${answer.id}">
        <input id="choice_${answer.id}" name="choice_answers" type="${inputType}" data-choiceid="${answer.id}">
        <span>${answer.answer}</span>
    </label>
</p>`.trim());
    }

    return answerInputs.join('\n');
}

function updateHtml(): void {
    [currentFlashcard, ...flashcards] = flashcards;

    if (currentFlashcard != undefined) {
        let toAppend: string = `
<div class="card-panel">
    <h4 class="center-align">${currentFlashcard.question}</h4>
</div>`.trim();

        if (currentFlashcard.cardType == 'vocable') {
            toAppend += buildInputForVocable(/*currentFlashcard*/);
        } else if (currentFlashcard.cardType === 'single_choice' || currentFlashcard.cardType === 'multiple_choice') {
            toAppend += buildInputForChoiceFlashcard(currentFlashcard);
        }

        flashcardDiv.html(toAppend);
    }
}

function onLoadFlashcardsSuccess(loadedFlashcards: LoadedFlashcard[]): void {
    flashcards = loadedFlashcards;
    flashcardDiv = $('#flashcardDiv');

    // console.info(JSON.stringify(loadedFlashcards, null, 2));

    updateHtml();
}

function onLoadFlashcardsError(jqXHR): void {
    console.error(jqXHR.responseText);
}