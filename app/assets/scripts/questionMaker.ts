/// <reference path="learnRepeatBasics.ts"/>

const textAnswerInput: string = `
<div class="row">
    <div class="input-field col s12">
        <input type="text" id="translation_input" autofocus autocomplete="off" autocorrect="off" autocapitalize="off" spellcheck="false">
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
    const numOfCorrectAnswers = choiceAnswers.filter(ca => ca.correctness !== 'Wrong').length;
    const choiceInputType: string = numOfCorrectAnswers == 1 ? 'radio' : 'checkbox';

    return shuffleArray(choiceAnswers).map(choiceAnswer => `
<p class="choiceParagraph">
    <label for="choice_${choiceAnswer.answerId}">
        <input id="choice_${choiceAnswer.answerId}" name="choice_answers" type="${choiceInputType}" data-choiceid="${choiceAnswer.answerId}">
        <span>${choiceAnswer.answer}</span>
    </label>
</p>`.trim()).join('\n');
}

function updateView(flashcard: Flashcard): void {

    // Update question text
    let questionText: string;
    if (flashcard.frontToBack) {
        questionText = flashcard.front + (flashcard.frontHint !== undefined ? ` <i>${flashcard.frontHint}</i>` : '');
    } else {
        questionText = flashcard.back + (flashcard.backHint !== undefined ? ` <i>${flashcard.backHint}</i>` : '');
    }
    document.querySelector<HTMLHeadingElement>('#questionDiv').innerHTML = questionText;

    // Update tries counter
    document.querySelector<HTMLSpanElement>('#triesSpan').innerText = flashcard.currentTries.toFixed(0);

    // Update bucket counter
    if (flashcard.currentBucket !== undefined) {
        document.querySelector<HTMLSpanElement>('#bucketSpan').innerText = flashcard.currentBucket.toFixed(0);
    } else {
        document.querySelector<HTMLSpanElement>('#bucketSpan').innerText = '--';
    }

    // Set answering inputs
    const answerDiv = document.querySelector<HTMLDivElement>('#answerDiv');
    switch (flashcard.cardType) {
        case 'Text':
        case 'Word' :
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
