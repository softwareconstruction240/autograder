import type {CanvasSection, Phase, Submission, User } from '@/types/types'
import type {Option} from "@/views/AdminView/Analytics.vue";
import { ServerCommunicator } from '@/network/ServerCommunicator'

export const usersGet = async (): Promise<User[]> => {
    return await ServerCommunicator.getRequestGuaranteed<User[]>('/api/admin/users', [])
}

export const submissionsForUserGet = async (netId: string): Promise<Submission[]> => {
    return await ServerCommunicator.getRequestGuaranteed<Submission[]>('/api/admin/submissions/student/' + netId, [])
}

export const approveSubmissionPost = async (netId: string, phase: Phase, penalize: boolean) => {
    await ServerCommunicator.postRequest('/api/admin/submissions/approve',
      {
        netId,
        phase,
        penalize,
    })
}

export const submissionsLatestGet = async (batchSize?: number): Promise<Submission[]> => {
    batchSize = batchSize ? batchSize : -1
    return await ServerCommunicator.getRequestGuaranteed<Submission[]>('/api/admin/submissions/latest/' + batchSize, [])
}

export const testStudentModeGet = async (): Promise<null> => {
    return await ServerCommunicator.getRequestGuaranteed<null>('/api/admin/test_mode', null)
}

type QueueStatusResponse = {
    currentlyGrading: string[],
    inQueue: string[]
}
export const getQueueStatus = async (): Promise<QueueStatusResponse> => {
    return await ServerCommunicator.getRequestGuaranteed<QueueStatusResponse>(
      '/api/admin/submissions/active', {
                currentlyGrading: [],
                inQueue: []
            })
}

export const commitAnalyticsGet = async (option: Option): Promise<string> => {
    try {
        return (await ServerCommunicator.doUnprocessedRequest("GET", '/api/admin/analytics/commit/' + option)).text()
    } catch (e) {
        return ''
    }
}

export const honorCheckerZipGet = async (section: number): Promise<Blob> => {
    try {
        return (await ServerCommunicator.doUnprocessedRequest("GET", '/api/admin/honorChecker/zip/' + section)).blob()
    } catch (e) {
        return new Blob()
    }
}

export const sectionsGet = async (): Promise<CanvasSection[]> => {
    return await ServerCommunicator.getRequestGuaranteed<CanvasSection[]>('/api/admin/sections', [])
}
