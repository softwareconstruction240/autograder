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