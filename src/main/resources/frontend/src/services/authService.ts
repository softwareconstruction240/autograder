import {useAppConfigStore} from "@/stores/appConfig";

type MeResponse = {
    netId: string,
    firstName: string,
    lastName: string,
    repoUrl: string,
    role: 'STUDENT' | 'ADMIN'
}
export const meGet = async () => {
    try {
        const response = await fetch(useAppConfigStore().backendUrl + '/api/me', {
            method: 'GET',
            credentials: 'include'
        });

        return await response.json() as MeResponse | null;
    } catch (e) {
        return null;
    }
}

export const registerPost = async (firstName: string, lastName: string, repoUrl: string) => {
    const response = await fetch(useAppConfigStore().backendUrl + '/api/register', {
        method: 'POST',
        credentials: 'include',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({firstName, lastName, repoUrl})
    });

    return response.ok;
}