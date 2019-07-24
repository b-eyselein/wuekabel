type CardType = 'Word' | 'Text' | 'Blank' | 'Choice';

type CorrectnessType = 'Correct' | 'Optional' | 'Wrong';

function shuffleArray<T>(array: T[]): T[] {
    let newArray: T[] = array.slice(0);

    for (let i = newArray.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [newArray[i], newArray[j]] = [newArray[j], newArray[i]];
    }

    return newArray;
}

function domReady(callBack: () => void): void {
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', callBack);
    } else {
        callBack();
    }
}

interface BlanksAnswerFragment {
    answerId: number;
    answer: string;
}

interface ChoiceAnswer {
    answerId: number;
    answer: string;
    correctness: CorrectnessType;
}

interface Flashcard {
    cardId: number;
    collId: number;
    courseId: number;

    cardType: CardType;

    fronts: string[];
    frontHint: string | undefined;
    backs: string[];
    backHint: string | undefined;


    blanksAnswers: BlanksAnswerFragment[];
    choiceAnswers: ChoiceAnswer[];
}

interface FlashcardToAnswer {
    flashcard: Flashcard;
    frontToBack: boolean;
    currentTries: number;
    currentBucket: undefined | number;
}

interface Solution {
    cardId: number;
    collId: number;
    courseId: number;

    solutions: string[];
    selectedAnswers: number[];
    frontToBack: boolean;
}

interface EditOperation {
    operationType: "Replace" | "Insert" | "Delete"
    index: number
    char: string | null
}

interface AnswerSelectionResult {
    wrong: number[]
    correct: number[]
    missing: number[]
}

interface CorrectionResult {
    correct: boolean
    operations: EditOperation[]
    answersSelection: AnswerSelectionResult
    newTriesCount: number
    maybeSampleSolution: string | null
}
