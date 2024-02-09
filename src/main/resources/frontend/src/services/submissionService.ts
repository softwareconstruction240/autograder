import type {Phase, Submission} from "@/types/types";
import {useAppConfigStore} from "@/stores/appConfig";

export const submissionsGet = async (phase: Phase): Promise<Submission[]> => {
    const response = await fetch(useAppConfigStore().backendUrl + '/api/submission/' + phase, {
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
            phase,
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