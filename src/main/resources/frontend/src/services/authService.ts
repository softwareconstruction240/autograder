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

        if (response.status === 403)
            return 403;

        return await response.json() as MeResponse | 403 | null;
    } catch (e) {
        return null;
    }
}

export const registerPost = async (firstName: string, lastName: string, repoUrl: string) => {
    const response = await fetch(useAppConfigStore().backendUrl + '/auth/register', {
        method: 'POST',
        credentials: 'include',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({firstName, lastName, repoUrl})
    });

    return response.ok;
}