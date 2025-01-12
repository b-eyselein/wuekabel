/// <reference path="./learnRepeatBasics.ts"/>
/// <reference path="./questionMaker.ts"/>

let checkSolutionBtn: HTMLButtonElement;
let nextFlashcardBtn: HTMLButtonElement;

let progressDiv: HTMLDivElement;

let correctionTextPar: HTMLParagraphElement;

let currentFlashcard: null | FlashcardToAnswer = null;
let flashcards: FlashcardToAnswer[] = [];

let checkSolutionUrl: string;
let canSolve: boolean = true;

let maxNumOfCards: number = 10;
let answeredFlashcards: number = 0;

function readSolution(cardType: CardType): undefined | Solution {
  let solutions: StringSolution[] = [];
  let selectedAnswers: number[] = [];

  switch (cardType) {
    case 'Word':
    case 'Text':
      const solutionInputs: HTMLInputElement[] = Array.from(document.querySelectorAll<HTMLInputElement>('.translation_input'));

      solutions = solutionInputs
        .map<StringSolution>((input: HTMLInputElement, index: number) => {
          return {
            id: index,
            solution: input.value.trim()
          };
        })
        .filter((solution: StringSolution) => solution.solution.length !== 0);

      if (solutions.length === 0) {
        alert('Sie können keine leere Lösung abgeben!');
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
    cardId: currentFlashcard.flashcard.cardId,
    collId: currentFlashcard.flashcard.collId,
    courseId: currentFlashcard.flashcard.courseId,
    solutions,
    selectedAnswers,
    frontToBack: currentFlashcard.frontToBack
  };
}

function onCorrectionSuccess(result: CorrectionResult, cardType: CardType): void {

  console.info(JSON.stringify(result, null, 2));

  let correctionText = `Ihre Lösung war ${result.correct ? '' : 'nicht '} korrekt.`;

  if ((result.newTriesCount >= 2) && (result.maybeSampleSolution != null)) {
    correctionText += ` Die korrekte Lösung lautet '<code>${result.maybeSampleSolution}</code>'.`;
  }

  canSolve = !(result.correct || result.newTriesCount >= 2);

  correctionTextPar.innerHTML = correctionText;
  correctionTextPar.classList.remove(result.correct ? 'red-text' : 'green-text');
  correctionTextPar.classList.add(result.correct ? 'green-text' : 'red-text');

  // Update buttons
  checkSolutionBtn.disabled = !canSolve;
  nextFlashcardBtn.disabled = canSolve;

  if (!canSolve) {
    answeredFlashcards++;
    progressDiv.style.width = `${answeredFlashcards / maxNumOfCards * 100}%`;

    document.querySelector('#answeredFcCountSpan').innerHTML = answeredFlashcards.toString();

    // FIXME: disable solution inputs?
    if (currentFlashcard.flashcard.cardType in ['Text', 'Word']) {
      document.querySelectorAll<HTMLInputElement>('.translation_input').forEach(
        (translationInput: HTMLInputElement) => translationInput.disabled = true
      );
    }

    if (answeredFlashcards >= maxNumOfCards) {
      nextFlashcardBtn.onclick = () => {
        window.location.href = nextFlashcardBtn.dataset['endurl'];
      };
      nextFlashcardBtn.textContent = "Bearbeiten beenden";
    }
  }

  document.querySelector<HTMLSpanElement>('#triesSpan').innerText = result.newTriesCount.toString();

  switch (cardType) {
    case 'Word':
    case 'Text':
      result.matchingResult.matches.forEach((match: Match) => {
        const translationInput: HTMLInputElement = document.querySelector<HTMLInputElement>(`#translation_input_${match.start.id}`);

        const isCorrect = match.distance === 0;

        translationInput.classList.remove(isCorrect ? 'invalid' : 'valid');
        translationInput.classList.add(isCorrect ? 'valid' : 'invalid');
      });
      break;
    case 'Blank':
      document.querySelectorAll<HTMLInputElement>('.translation_input').forEach((textInput) => {
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

function initialLoadNextFlashcards(loadFlashcardUrl: string): void {
  fetch(loadFlashcardUrl).then(response => {
    if (response.status === 200) {
      response.json().then((loadedFlashcard: FlashcardToAnswer[]) => {

        flashcards = loadedFlashcard;

        loadNextFlashcard();
      })
    } else if (response.status === 404) {
      alert("Sie haben alle Karteikarten abgearbeitet.");
      window.location.href = '/';
    }
  })
}

function loadNextFlashcard(): void {
  if (flashcards.length > 0) {
    [currentFlashcard, ...flashcards] = flashcards;

    canSolve = true;

    // Update buttons
    checkSolutionBtn.disabled = !canSolve;
    nextFlashcardBtn.disabled = canSolve;

    correctionTextPar.innerHTML = '&nbsp;';

    updateView(currentFlashcard);
  } else {
    throw Error('No flashcards loaded...');
  }
}

function checkSolution(): void {
  const solution: Solution = readSolution(currentFlashcard.flashcard.cardType);

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
          return response.json()
            .then(obj => onCorrectionSuccess(obj, currentFlashcard.flashcard.cardType));
        } else {
          response.text().then(text => console.error(text));
          return Promise.reject("Error code was " + response.status);
        }
      }
    )
    .catch(reason => console.error(reason));
}

function updateReadQuestionButton(readQuestionButton: HTMLButtonElement): void {
  if (window.speechSynthesis.getVoices().length > 0) {
    readQuestionButton.classList.remove('hide');
    readQuestionButton.disabled = false;

    readQuestionButton.onclick = () => {
      const toRead: string[] = currentFlashcard.frontToBack ? currentFlashcard.flashcard.frontsJson : currentFlashcard.flashcard.backsJson;

      toRead.forEach((front) => {
        const utterance = new SpeechSynthesisUtterance(front);
        utterance.lang = 'fr';
        window.speechSynthesis.speak(utterance);
      });
    };
  }
}

domReady(() => {
  const initialLoadBtn = document.querySelector<HTMLButtonElement>('#loadFlashcardButton');

  const loadFlashcardUrl = initialLoadBtn.dataset['href'];

  initialLoadBtn.onclick = () => {
    initialLoadNextFlashcards(loadFlashcardUrl);
    initialLoadBtn.remove();
  };
  initialLoadBtn.click();

  correctionTextPar = document.querySelector<HTMLParagraphElement>('#correctionTextPar');

  // FIXME: activate...?
  const readQuestionButton = document.querySelector<HTMLButtonElement>('#readQuestionButton');

  updateReadQuestionButton(readQuestionButton);

  window.speechSynthesis.onvoiceschanged = () => {
    updateReadQuestionButton(readQuestionButton);
  };
  window.speechSynthesis.getVoices();

  nextFlashcardBtn = document.querySelector<HTMLButtonElement>('#nextFlashcardBtn');
  nextFlashcardBtn.onclick = () => loadNextFlashcard();

  checkSolutionBtn = document.querySelector<HTMLButtonElement>('#checkSolutionBtn');
  checkSolutionBtn.onclick = checkSolution;

  checkSolutionUrl = checkSolutionBtn.dataset['href'];

  maxNumOfCards = parseInt(document.querySelector<HTMLSpanElement>('#maxCardCountSpan').innerText);

  progressDiv = document.querySelector<HTMLDivElement>('#progressDiv');

  document.addEventListener('keypress', event => {
    if (event.key === 'Enter') {
      if (event.ctrlKey) {
        readQuestionButton.click();
      } else if (canSolve) {
        checkSolutionBtn.click();
      } else {
        nextFlashcardBtn.click();
      }
    } else if (currentFlashcard.flashcard.cardType === 'Choice' && canSolve) {
      const pressedKey: number = parseInt(event.key);

      const choiceParagraph: null | Element = document.querySelector<HTMLDivElement>('#answerDiv')
        .querySelectorAll('p.choiceParagraph')
        .item(pressedKey - 1);

      if (choiceParagraph !== null) {
        choiceParagraph.querySelector<HTMLInputElement>('input').click();
      }
    }
  });
});
