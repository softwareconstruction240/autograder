import type {Submission} from "@/types/types";
import { Phase } from "@/types/types";
import {useAppConfigStore} from "@/stores/appConfig";

export const submissionsGet = async (phase: Phase): Promise<Submission[]> => {
    const response = await fetch(useAppConfigStore().backendUrl + '/api/submission/' + Phase[phase], {
        method: 'GET',
        credentials: 'include'
    });

    if (!response.ok) {
        console.error(response);
        return [];
    }

    return await response.json() as Submission[];
};

export const submissionPost = async (phase: Phase): Promise<void> => {
    const response = await fetch(useAppConfigStore().backendUrl + '/api/submit', {
        method: 'POST',
        credentials: 'include',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            "phase": Phase[phase],
        })
    });

    if (!response.ok) {
        console.error(response);
        throw new Error(await response.text());
    }
}

export const adminSubmissionPost = async (phase: Phase, repoUrl: String): Promise<void> => {
    const response = await fetch(useAppConfigStore().backendUrl + '/api/admin/submit', {
        method: 'POST',
        credentials: 'include',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            "phase": Phase[phase],
            "repoUrl": repoUrl
        })
    });

    if (!response.ok) {
        console.error(response);
        throw new Error(await response.text());
    }
}

type SubmitGetResponse = {
    inQueue: boolean,
}
export const submitGet = async (): Promise<boolean> => {
    const response = await fetch(useAppConfigStore().backendUrl + '/api/submit', {
        method: 'GET',
        credentials: 'include'
    });

    if (!response.ok) {
        console.error(response);
        throw new Error(await response.text());
    }

    const body = await response.json() as SubmitGetResponse;

    return body.inQueue;
}

export const reRunSubmissionsPost = async () => {
    const response = await fetch(useAppConfigStore().backendUrl + "/api/admin/submissions/rerun", {
        method: 'POST',
        credentials: 'include',
        headers: {
            'Content-Type': 'application/json',
        },
    });

    if (!response.ok) {
        console.error(response);
        throw new Error(await response.text());
    } else {
        return true;
    }
}