export enum Phase {
    Phase0,
    Phase1,
    Phase3,
    Phase4,
    Phase5,
    Phase6,
    Quality
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
    passed: boolean,
    score: number,
    notes: string,
    rubric: Rubric,
    admin: boolean,
    verifiedStatus: VerifiedStatus,
    verification: ScoreVerification
}

export enum VerifiedStatus {
    Unapproved,
    ApprovedAutomatically,
    ApprovedManually,
    PreviouslyApproved,
}

export type ScoreVerification = {
    originalScore: number,
    approvingNetId: string,
    approvedTimestamp: string,
    penaltyPct: number
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
