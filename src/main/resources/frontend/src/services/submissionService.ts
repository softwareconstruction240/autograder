import type {Submission} from "@/types/types";
import { Phase } from "@/types/types";
import { ServerCommunicator } from '@/network/ServerCommunicator'

export const submissionsGet = async (phase: Phase | null): Promise<Submission[]> => {
    const endpoint: string = '/api/submission' + (phase === null ? "" : "/" + Phase[phase])
    return await ServerCommunicator.getRequestGuaranteed<Submission[]>(endpoint, [])
};

export const lastSubmissionGet = async (): Promise<Submission | null> => {
    return await ServerCommunicator.getRequestGuaranteed<Submission | null>("/api/latest", null)
};

export const submissionPost = async (phase: Phase): Promise<void> => {
    await ServerCommunicator.postRequest("/api/submit", { "phase": Phase[phase] }, false)
}

export const adminSubmissionPost = async (phase: Phase, repoUrl: String): Promise<void> => {
    await ServerCommunicator.postRequest("/api/admin/submit", {
        "phase": Phase[phase],
        "repoUrl": repoUrl
    }, false)
}

type SubmitGetResponse = {
    inQueue: boolean,
}
export const submitGet = async (): Promise<boolean> => {
    return (await ServerCommunicator.getRequest<SubmitGetResponse>("/api/submit")).inQueue
}

export const reRunSubmissionsPost = async () => {
    await ServerCommunicator.postRequest("/api/admin/submissions/rerun")
}
