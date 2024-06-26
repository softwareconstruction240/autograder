export enum Phase {
    Phase0,
    Phase1,
    Phase3,
    Phase4,
    Phase5,
    Phase6,
    Quality,
    GitHub
}

export type TestNode = {
    testName: string,
    passed: boolean,
    ecCategory: string,
    errorMessage: string,
    children: TestNode[],
    numTestsPassed: number,
    numTestsFailed: number,
    numExtraCreditPassed: number,
    numExtraCreditFailed: number,
}

export type TestResult = {
    root: TestNode,
    extraCredit: TestNode,
    error: string
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

export type RubricType = "PASSOFF_TESTS" | "UNIT_TESTS" | "QUALITY" | "GIT_COMMITS"

export type Rubric = {
    items: Record<RubricType, RubricItem>,
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
    admin: boolean,
    verifiedStatus: VerifiedStatus
}

export enum VerifiedStatus {
    Unapproved,
    ApprovedAutomatically,
    ApprovedManually,
    PreviouslyApproved,
}

export type User = {
    netId: string,
    firstName: string,
    lastName: string,
    repoUrl: string,
    role: 'STUDENT' | 'ADMIN'
}

export type CanvasSection = {
    id: number,
    name: string
}
