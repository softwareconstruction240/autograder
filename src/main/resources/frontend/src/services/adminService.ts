import {useAppConfigStore} from "@/stores/appConfig";
import type {CanvasSection, Phase, Submission, User } from '@/types/types'
import type {Option} from "@/views/AdminView/Analytics.vue";

export const usersGet = async (): Promise<User[]> => {
    try {
        const response = await fetch(useAppConfigStore().backendUrl + '/api/admin/users', {
            method: 'GET',
            credentials: 'include'
        });

        return await response.json();
    } catch (e) {
        return [];
    }
}

export const submissionsForUserGet = async (netId: string): Promise<Submission[]> => {
    try {
        const response = await fetch(useAppConfigStore().backendUrl + '/api/admin/submissions/student/' + netId, {
            method: 'GET',
            credentials: 'include'
        });

        return await response.json();
    } catch (e) {
        return [];
    }
}

export const approveSubmissionPost = async (netId: string, phase: Phase, penalize: boolean) => {
    const response = await fetch(useAppConfigStore().backendUrl + '/api/admin/submissions/approve', {
        method: 'POST',
        credentials: 'include',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            netId,
            phase,
            penalize,
        })
    });

    if (!response.ok) {
        console.error(response);
        throw new Error(await response.text());
    }
}

interface UserPatch {
    netId: string,
    firstName?: string,
    lastName?: string,
    repoUrl?: string,
    role?: string

}

/**
 * this sends to /api/admin/user/{netId}
 *
 * query params can be zero or more of the following: firstName, lastName, repoUrl, role. these are added only if needed to the url
 * @param user
 */
export const userPatch = async (user: UserPatch)=> {
    const paramsString = new URLSearchParams();

    if (user.firstName)
        paramsString.append('firstName', user.firstName);
    if (user.lastName)
        paramsString.append('lastName', user.lastName);
    if (user.repoUrl)
        paramsString.append('repoUrl', user.repoUrl);
    if (user.role)
        paramsString.append('role', user.role);

    try {
        const response = await fetch(useAppConfigStore().backendUrl + '/api/admin/user/' + user.netId + '?' + paramsString.toString(), {
            method: 'PATCH',
            credentials: 'include'
        });

    } catch (e) {
        console.error('Failed to update user: ', e);
    }
}

export const submissionsLatestGet = async (batchSize?: number): Promise<Submission[]> => {
    batchSize = batchSize ? batchSize : -1
    try {
        const response = await fetch(useAppConfigStore().backendUrl + '/api/admin/submissions/latest/' + batchSize, {
            method: 'GET',
            credentials: 'include'
        });

        return await response.json();
    } catch (e) {
        return [];
    }
}

export const testStudentModeGet = async (): Promise<null> => {
    try {
        const response = await fetch(useAppConfigStore().backendUrl + '/api/admin/test_mode', {
            method: 'GET',
            credentials: 'include'
        });

        return null;
    } catch (e) {
        console.error('Failed to activate test mode: ', e);
        return null;
    }
}

type QueueStatusResponse = {
    currentlyGrading: string[],
    inQueue: string[]
}
export const getQueueStatus = async (): Promise<QueueStatusResponse> => {
    try {
        const response = await fetch(useAppConfigStore().backendUrl + '/api/admin/submissions/active', {
            method: 'GET',
            credentials: 'include'
        });

        return await response.json();
    } catch (e) {
        console.error('Failed to get queue status: ', e);
        return {
            currentlyGrading: [],
            inQueue: []
        };
    }
}

export const commitAnalyticsGet = async (option: Option): Promise<string> => {
    try {
        return (await fetch(useAppConfigStore().backendUrl + '/api/admin/analytics/commit/' + option, {
            method: 'GET',
            credentials: 'include'
        })).text()
    } catch (e) {
        console.error('Failed to get data: ', e)
        return ''
    }
}

export const honorCheckerZipGet = async (section: number): Promise<Blob> => {
    try {
        return (await fetch(useAppConfigStore().backendUrl + '/api/admin/honorChecker/zip/' + section, {
            method: 'GET',
            credentials: 'include'
        })).blob()
    } catch (e) {
        console.error('Failed to get data: ', e)
        return new Blob()
    }
}

export const sectionsGet = async (): Promise<CanvasSection[]> => {
    try {
        const response = await fetch(useAppConfigStore().backendUrl + '/api/admin/sections', {
            method: 'GET',
            credentials: 'include'
        });

        return await response.json();
    } catch (e) {
        console.error('Failed to get data: ', e)
        return [];
    }
}