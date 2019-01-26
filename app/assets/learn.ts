/// <reference path="loadFlashcards.ts"/>
/// <reference path="solutionTypes.ts"/>


let checkSolutionUrl: string;


function checkSolution(): void {
    let answer: AnswerType;

    switch (currentFlashcard.cardType) {
        case 'vocable':
            const learnerSolution: string = $('#translation_input').val() as string;

            if (learnerSolution.length === 0) {
                alert('Lösung ist leer!');
                return;
            }

            answer = new TranslationSolution(learnerSolution);
            break;


        case 'single_choice':
        case 'multiple_choice':
            const selectedAnswers: number[] = [];

            $('input[name=choice_answers]').each((_, element: HTMLElement) => {
                if (element instanceof HTMLInputElement && element.checked) {
                    selectedAnswers.push(parseInt(element.dataset.choiceid));
                }
            });

            if (selectedAnswers.length === 0) {
                alert('Sie haben keine Lösung ausgewählt!');
                return;
            }

            answer = new ChoiceSolution(selectedAnswers);
            break;
        default:
            alert('There has been an internal error...');
            return;
    }

    let solution: Solution = new Solution(currentFlashcard.id, currentFlashcard.collId, currentFlashcard.langId, currentFlashcard.cardType, answer);
    console.warn(JSON.stringify(solution, null, 2));

    $.ajax({
        url: checkSolutionUrl,
        method: 'PUT',
        contentType: 'application/json',
        data: JSON.stringify(solution),
        dataType: 'json',
        success: (result: CorrectionResult) => {
            console.info(JSON.stringify(result, null, 2));

            $('#translation_input').removeClass(result.correct ? 'invalid' : 'valid').addClass(result.correct ? 'valid' : 'invalid');
        },
        error: (jqXHR) => {
            console.error(jqXHR.responseText);
        }
    })

}

function nextFlashcard(): void {
    console.error("TODO: next flashcard...");
    updateHtml();
}

$(() => {
    const startLearningBtn = $('#startLearningBtn');
    const loadFlashcardsUrl = startLearningBtn.data('href');
    startLearningBtn.remove();

    checkSolutionUrl = $('#checkSolutionBtn').data('href');

    $.ajax({
        url: loadFlashcardsUrl,
        success: onLoadFlashcardsSuccess,
        error: onLoadFlashcardsError
    });
});