/// <reference path="learnRepeatBasics.ts"/>

function buildTextAnswerInput(answers: string[]): string {
    return answers.map((_, index) => `
<div class="row">
    <div class="input-field col s12">
        <input type="text" class="translation_input"
            id="translation_input_${index}" data-index="${index}"
            autocomplete="off" autocorrect="off" autocapitalize="off" spellcheck="false">
        <label for="translation_input_${index}">Übersetzung ${index + 1}</label>
    </div>
</div>`.trim()
    ).join('\n');
}

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

function updateView(flashcard: FlashcardToAnswer): void {

    // Update question text
    let questionText: string;
    if (flashcard.frontToBack) {
        questionText = flashcard.flashcard.frontsJson.join('/') + (flashcard.flashcard.frontHint !== undefined ? ` <i>${flashcard.flashcard.frontHint}</i>` : '');
    } else {
        questionText = flashcard.flashcard.backsJson.join('/') + (flashcard.flashcard.backHint !== undefined ? ` <i>${flashcard.flashcard.backHint}</i>` : '');
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
    switch (flashcard.flashcard.cardType) {
        case 'Text':
        case 'Word' :
            answerDiv.innerHTML = buildTextAnswerInput(flashcard.frontToBack ? flashcard.flashcard.backsJson : flashcard.flashcard.frontsJson);
            document.querySelector<HTMLInputElement>('#translation_input_0').focus();
            break;

        case 'Blank':
            answerDiv.innerHTML = buildAnswerFragments(flashcard.flashcard.blanksAnswerFragmentsJson);
            break;

        case 'Choice':
            answerDiv.innerHTML = buildChoiceAnswers(flashcard.flashcard.choiceAnswersJson);
            break;
    }
}
