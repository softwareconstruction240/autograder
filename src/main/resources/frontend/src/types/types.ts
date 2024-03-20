export type Phase = '0' | '1' | '3' | '4' | '5' | '6';

export type TestResult = {
    testName: string,
    passed: boolean,
    ecCategory: string,
    errorMessage: string,
    children: TestResult[],
    numTestsPassed: number,
    numTestsFailed: number,
    numExtraCreditPassed: number,
    numExtraCreditFailed: number,
}

export type RubricItemResults = {
    notes: string,
    score: number,
    possiblePoints: number,
    testResults: TestResult,
    textResults: string,
}

export type RubricItem = {
    category: string,
    criteria: string,
    results: RubricItemResults,
}

export type Rubric = {
    passoffTests: RubricItem,
    unitTests: RubricItem,
    quality: RubricItem,
    passed: boolean,
    notes: string,
}

export type Submission = {
    netId: string,
    repoUrl: string,
    headHash: string,
    timestamp: string,
    phase: Phase,
    score: number,
    notes: string,
    rubric: Rubric,
    passed: boolean,
    admin: boolean
}

export type User = {
    netId: string,
    firstName: string,
    lastName: string,
    repoUrl: string,
    role: 'STUDENT' | 'ADMIN'
}
