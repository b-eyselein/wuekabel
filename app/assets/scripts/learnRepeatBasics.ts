type CardType = 'Vocable' | 'Text' | 'Blank' | 'Choice';

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
    correct: boolean;
}

interface Flashcard {
    cardId: number;
    collId: number;
    courseId: number;

    cardType: CardType;

    front: string;
    frontHint: string | undefined;

    frontToBack: boolean;

    blanksAnswers: BlanksAnswerFragment[];
    choiceAnswers: ChoiceAnswer[];

    currentTries: number;
    currentBucket: undefined | number;
}

interface Solution {
    cardId: number;
    collId: number;
    courseId: number;

    solution: string;
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
    maybeSampleSol: string | null
}
