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

export type Submission = {
    netId: string,
    repoUrl: string,
    headHash: string,
    timestamp: string,
    phase: Phase,
    score: number,
    notes: string,
    testResults: TestResult,
}

export type User = {
    netId: string,
    firstName: string,
    lastName: string,
    repoUrl: string,
    role: 'STUDENT' | 'ADMIN'
}