/// <reference path="learnRepeatBasics.ts"/>

const textAnswerInput: string = `
<div class="row">
    <div class="input-field col s12">
        <input type="text" id="translation_input" autofocus autocomplete="off">
        <label for="translation_input">Übersetzung</label>
    </div>
</div>`.trim();

function buildAnswerFragments(answerFragments: BlanksAnswerFragment[]): string {
    return answerFragments.map(answerFragment => `
<div class="row">
    <div class="input-field col s12">
        <input type="text" id="blanksInput_${answerFragment.answerId}" autofocus autocomplete="off">
        <label for="blanksInput_${answerFragment.answerId}">Übersetzung</label>
    </div>
</div>`.trim()).join('\n');
}

function buildChoiceAnswers(choiceAnswers: ChoiceAnswer[]): string {
    const choiceInputType: string = choiceAnswers.filter(ca => ca.correct).length > 0 ? 'radio' : 'checkbox';

    return shuffleArray(choiceAnswers).map(choiceAnswer => `
<p>
    <label for="choice_${choiceAnswer.answerId}">
        <input id="choice_${choiceAnswer.answerId}" name="choice_answers" type="${choiceInputType}" data-choiceid="${choiceAnswer.answerId}">
        <span>${choiceAnswer.answer}</span>
    </label>
</p>`.trim()).join('\n');
}

function updateQuestionText(flashcard: Flashcard): void {
    let questionText = flashcard.front;
    if (flashcard.frontHint !== undefined) {
        questionText += ` <i>${flashcard.frontHint}</i>`;
    }

    document.querySelector<HTMLHeadingElement>('#questionDiv').innerHTML = questionText;
}

function updateView(flashcard: Flashcard): void {

    updateQuestionText(flashcard);

    document.querySelector<HTMLSpanElement>('#triesSpan').innerText = flashcard.currentTries.toFixed(0);

    if (flashcard.currentBucket !== undefined) {
        document.querySelector<HTMLSpanElement>('#bucketSpan').innerText = flashcard.currentBucket.toFixed(0);
    } else {
        document.querySelector<HTMLSpanElement>('#bucketSpan').innerText = '--';
    }

    // Set answering inputs
    const answerDiv = document.querySelector<HTMLDivElement>('#answerDiv');
    switch (flashcard.cardType) {
        case 'Text':
        case 'Vocable' :
            answerDiv.innerHTML = textAnswerInput;
            document.querySelector<HTMLInputElement>('#translation_input').focus();
            break;

        case 'Blank':
            answerDiv.innerHTML = buildAnswerFragments(flashcard.blanksAnswers);
            break;

        case 'Choice':
            answerDiv.innerHTML = buildChoiceAnswers(flashcard.choiceAnswers);
            break;
    }
}
