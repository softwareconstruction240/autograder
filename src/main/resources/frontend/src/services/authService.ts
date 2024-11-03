import {useAppConfigStore} from "@/stores/appConfig";
import { useAuthStore } from '@/stores/auth'

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

export const loadUser = async () => {
    const loggedInUser = await meGet()
    if (loggedInUser == null)
        return;

    useAuthStore().user = loggedInUser;
}

export const logoutPost = async () => {
    const response = await fetch(useAppConfigStore().backendUrl + "/auth/logout", {
        method: 'POST',
        credentials: 'include',
        headers: {
            'Content-Type': 'application/json',
        },
    });

    if (!response.ok) {
        console.error(response);
        throw new Error(await response.text());
    }
}
