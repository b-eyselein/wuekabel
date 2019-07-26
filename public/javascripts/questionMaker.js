function buildTextAnswerInput(answers) {
    return answers.map((_, index) => `
<div class="row">
    <div class="input-field col s12">
        <input type="text" class="translation_input"
            id="translation_input_${index}" data-index="${index}"
            autocomplete="off" autocorrect="off" autocapitalize="off" spellcheck="false">
        <label for="translation_input_${index}">Übersetzung ${index + 1}</label>
    </div>
</div>`.trim()).join('\n');
}
function buildAnswerFragments(answerFragments) {
    return answerFragments.map(answerFragment => `
<div class="row">
    <div class="input-field col s12">
        <input type="text" id="blanksInput_${answerFragment.answerId}" autofocus autocomplete="off">
        <label for="blanksInput_${answerFragment.answerId}">Übersetzung</label>
    </div>
</div>`.trim()).join('\n');
}
function buildChoiceAnswers(choiceAnswers) {
    const numOfCorrectAnswers = choiceAnswers.filter(ca => ca.correctness !== 'Wrong').length;
    const choiceInputType = numOfCorrectAnswers == 1 ? 'radio' : 'checkbox';
    return shuffleArray(choiceAnswers).map(choiceAnswer => `
<p class="choiceParagraph">
    <label for="choice_${choiceAnswer.answerId}">
        <input id="choice_${choiceAnswer.answerId}" name="choice_answers" type="${choiceInputType}" data-choiceid="${choiceAnswer.answerId}">
        <span>${choiceAnswer.answer}</span>
    </label>
</p>`.trim()).join('\n');
}
function updateView(flashcard) {
    let questionText;
    if (flashcard.frontToBack) {
        questionText = flashcard.flashcard.fronts.join('/') + (flashcard.flashcard.frontHint !== undefined ? ` <i>${flashcard.flashcard.frontHint}</i>` : '');
    }
    else {
        questionText = flashcard.flashcard.backs.join('/') + (flashcard.flashcard.backHint !== undefined ? ` <i>${flashcard.flashcard.backHint}</i>` : '');
    }
    document.querySelector('#questionDiv').innerHTML = questionText;
    document.querySelector('#triesSpan').innerText = flashcard.currentTries.toFixed(0);
    if (flashcard.currentBucket !== undefined) {
        document.querySelector('#bucketSpan').innerText = flashcard.currentBucket.toFixed(0);
    }
    else {
        document.querySelector('#bucketSpan').innerText = '--';
    }
    const answerDiv = document.querySelector('#answerDiv');
    switch (flashcard.flashcard.cardType) {
        case 'Text':
        case 'Word':
            answerDiv.innerHTML = buildTextAnswerInput(flashcard.frontToBack ? flashcard.flashcard.backs : flashcard.flashcard.fronts);
            document.querySelector('#translation_input_0').focus();
            break;
        case 'Blank':
            answerDiv.innerHTML = buildAnswerFragments(flashcard.flashcard.blanksAnswers);
            break;
        case 'Choice':
            answerDiv.innerHTML = buildChoiceAnswers(flashcard.flashcard.choiceAnswers);
            break;
    }
}
//# sourceMappingURL=/assets/questionMaker.js.map