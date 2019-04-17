type CardType = 'Vocable' | 'Text' | 'Blank' | 'Choice';

interface Solution {
    solution: string
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
    operations: EditOperation[],
    answersSelection: AnswerSelectionResult
    newTriesCount: number
    maybeSampleSol: string | null
}
