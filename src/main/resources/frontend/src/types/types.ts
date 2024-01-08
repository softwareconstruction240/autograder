export type Phase = '0' | '1' | '3' | '4' | '6';

export type TestResult = {
    testName: string,
    passed: boolean,
    errorMessage: string,
    children: TestResult[],
    numTestsPassed: number,
    numTestsFailed: number,

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