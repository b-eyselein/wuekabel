/// <reference path="./learnRepeatBasics.ts"/>

export class LevenshteinDistance {
    constructor(public a: string, public b: string, public distance: number, public editOps: EditOperation[]) {
    }
}

export function levenshtein(a: string, b: string): LevenshteinDistance {
    const an = a ? a.length : 0;

    const bn = b ? b.length : 0;

    if (an === 0) {
        return new LevenshteinDistance(a, b, bn, []);
    }

    if (bn === 0) {
        return new LevenshteinDistance(a, b, an, []);
    }

    const matrix = new Array<number[]>(bn + 1);

    for (let i = 0; i <= bn; ++i) {
        let row = matrix[i] = new Array<number>(an + 1);
        row[0] = i;
    }

    const firstRow = matrix[0];

    for (let j = 1; j <= an; ++j) {
        firstRow[j] = j;
    }

    for (let i = 1; i <= bn; ++i) {
        for (let j = 1; j <= an; ++j) {
            if (b.charAt(i - 1) === a.charAt(j - 1)) {
                matrix[i][j] = matrix[i - 1][j - 1];
            } else {
                matrix[i][j] = Math.min(
                    matrix[i - 1][j - 1], // substitution
                    matrix[i][j - 1], // insertion
                    matrix[i - 1][j] // deletion
                ) + 1;
            }
        }
    }

    return new LevenshteinDistance(a, b, matrix[bn][an], levenshteinBacktrace(a, b, matrix, an, bn));
}

function levenshteinBacktrace(a: string, b: string, matrix: number[][], an: number, bn: number): EditOperation[] {
    if (an > 0 && matrix [an - 1][bn] + 1 == matrix [an][bn]) {
        return levenshteinBacktrace(a, b, matrix, an - 1, bn).concat({operationType: 'Delete', char: a[an], index: an})
    } else if (bn > 0 && matrix [an][bn - 1] == matrix[an][bn]) {
        return levenshteinBacktrace(a, b, matrix, an, bn - 1)
    } else if (an > 0 && bn > 0 && matrix [an - 1][bn - 1] + 1 == matrix[an][bn]) {
        return levenshteinBacktrace(a, b, matrix, an - 1, bn - 1)
    } else if (an > 0 && bn > 0 && matrix [an - 1][bn - 1] == matrix[an][bn]) {
        return levenshteinBacktrace(a, b, matrix, an - 1, bn - 1)
    } else {
        return [];
    }

}
