interface AnswerType {

}

class Solution {
    constructor(public cardId: number, public collId: number, public langId: number,
                public cardType: 'vocable' | 'single_choice' | 'multiple_choice',
                public answer: AnswerType) {
    }
}

class TranslationSolution implements AnswerType {
    constructor(public solution: string) {
    }
}

class ChoiceSolution implements AnswerType {
    constructor(public selectedAnswers: number[]) {
    }
}

interface CorrectionResult {
    correct: boolean,
    learnerSolution: Solution,
    sampleSolution: string
}
