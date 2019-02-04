interface Solution {
    learnerSolution: string
    selectedAnswers: number[]
}

interface EditOperation {
    operationType: "Replace" | "Insert" | "Delete"
    index: number
    char: string | null
}

interface AnswerSelectionResult {
    wrong: number[],
    correct: number[],
    missing: number[]
}

interface CorrectionResult {
    correct: boolean,
    cardType: 'Vocable' | 'Text' | 'SingleChoice' | 'MultipleChoice'
    learnerSolution: Solution,
    operations: EditOperation[],
    answerSelection: AnswerSelectionResult
    newTriesCount: number
    maybeSampleSol: string | null
}

let correctionTextPar: JQuery<HTMLParagraphElement>;
let checkSolutionBtn: JQuery<HTMLButtonElement>;
let checkSolutionUrl: string;

function readSolution(cardType: string): Solution | null {
    switch (cardType) {
        case 'Vocable':
        case 'Text':
            const learnerSolution: string = $('#translation_input').val() as string;

            if (learnerSolution.length === 0) {
                return null;
            }

            return {learnerSolution, selectedAnswers: []};

        case 'SingleChoice':
        case 'MultipleChoice':
            const selectedAnswers: number[] = [];

            $('input[name=choice_answers]').each((_, element: HTMLElement) => {
                if (element instanceof HTMLInputElement && element.checked) {
                    selectedAnswers.push(parseInt(element.dataset.choiceid));
                }
            });

            if (selectedAnswers.length === 0) {
                return null;
            }

            return {learnerSolution: "", selectedAnswers};
        default:
            alert('There has been an internal error: ' + cardType);
            return null;
    }
}

function onCorrectionSuccess(result: CorrectionResult): void {
    console.info(JSON.stringify(result, null, 2));

    let correctionText = 'Ihre Lösung war ' + (result.correct ? '' : 'nicht ') + 'korrekt.';

    if ((result.newTriesCount >= 2) && (result.maybeSampleSol != null)) {
        correctionText += ` Die korrekte Lösung lautet '<code>${result.maybeSampleSol}</code>'.`;
    }

    correctionTextPar.html(correctionText).removeClass(result.correct ? 'red-text' : 'green-text').addClass(result.correct ? 'green-text' : 'red-text');

    checkSolutionBtn.prop('disabled', result.correct || (result.newTriesCount >= 2));

    if (result.correct || result.newTriesCount >= 2) {
        $('#nextFlashcardBtn').removeClass('disabled');
    }

    $('#triesSpan').text(result.newTriesCount);


    switch (result.cardType) {
        case 'Vocable':
        case 'Text':
            $('#translation_input').removeClass(result.correct ? 'invalid' : 'valid').addClass(result.correct ? 'valid' : 'invalid');
            break;
        case 'SingleChoice':
        case 'MultipleChoice':
            console.error(JSON.stringify(result.answerSelection));
            break;
        default:
            console.error(result.cardType);
    }
}

function checkSolution(): void {

    const cardType = $('#flashcardDiv').data('cardtype');

    const solution = readSolution(cardType);

    if (solution === null) {
        alert("Sie können keine leere Lösung abgeben!");
        return;
    }

    // console.warn(JSON.stringify(solution, null, 2));

    $.ajax({
        url: checkSolutionUrl,
        method: 'POST',
        contentType: 'application/json',
        data: JSON.stringify(solution),
        dataType: 'json',
        beforeSend: (xhr) => {
            const token = $('input[name="csrfToken"]').val() as string;
            xhr.setRequestHeader("Csrf-Token", token)
        },
        success: onCorrectionSuccess,
        error: (jqXHR) => {
            console.error(jqXHR.responseText);
        }
    })

}

$(() => {
    correctionTextPar = $('#correctionTextPar');
    checkSolutionBtn = $('#checkSolutionBtn');
    checkSolutionUrl = checkSolutionBtn.data('href');
});