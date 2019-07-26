"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
class LevenshteinDistance {
    constructor(a, b, distance, editOps) {
        this.a = a;
        this.b = b;
        this.distance = distance;
        this.editOps = editOps;
    }
}
exports.LevenshteinDistance = LevenshteinDistance;
function levenshtein(a, b) {
    const an = a ? a.length : 0;
    const bn = b ? b.length : 0;
    if (an === 0) {
        return new LevenshteinDistance(a, b, bn, []);
    }
    if (bn === 0) {
        return new LevenshteinDistance(a, b, an, []);
    }
    const matrix = new Array(bn + 1);
    for (let i = 0; i <= bn; ++i) {
        let row = matrix[i] = new Array(an + 1);
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
            }
            else {
                matrix[i][j] = Math.min(matrix[i - 1][j - 1], matrix[i][j - 1], matrix[i - 1][j]) + 1;
            }
        }
    }
    return new LevenshteinDistance(a, b, matrix[bn][an], levenshteinBacktrace(a, b, matrix, an, bn));
}
exports.levenshtein = levenshtein;
function levenshteinBacktrace(a, b, matrix, an, bn) {
    if (an > 0 && matrix[an - 1][bn] + 1 == matrix[an][bn]) {
        return levenshteinBacktrace(a, b, matrix, an - 1, bn).concat({ operationType: 'Delete', char: a[an], index: an });
    }
    else if (bn > 0 && matrix[an][bn - 1] == matrix[an][bn]) {
        return levenshteinBacktrace(a, b, matrix, an, bn - 1);
    }
    else if (an > 0 && bn > 0 && matrix[an - 1][bn - 1] + 1 == matrix[an][bn]) {
        return levenshteinBacktrace(a, b, matrix, an - 1, bn - 1);
    }
    else if (an > 0 && bn > 0 && matrix[an - 1][bn - 1] == matrix[an][bn]) {
        return levenshteinBacktrace(a, b, matrix, an - 1, bn - 1);
    }
    else {
        return [];
    }
}
//# sourceMappingURL=/assets/levenshtein.js.map