export type Phase = '0' | '1' | '3' | '4' | '6';

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
    testResults: TestResult,
    textResults: string,
}

export type RubricItem = {
    description: string,
    results: RubricItemResults,
}

export type Rubric = {
    passoffTests: RubricItem,
    unitTests: RubricItem,
    styleTests: RubricItem,
}

export type Submission = {
    netId: string,
    repoUrl: string,
    headHash: string,
    timestamp: string,
    phase: Phase,
    score: number,
    notes: string,
    rubric: TestResult,
    passed: boolean,
}

export type User = {
    netId: string,
    firstName: string,
    lastName: string,
    repoUrl: string,
    role: 'STUDENT' | 'ADMIN'
}