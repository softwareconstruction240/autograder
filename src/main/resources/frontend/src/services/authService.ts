import {useAppConfigStore} from "@/stores/appConfig";

type MeResponse = {
    netId: string,
    firstName: string,
    lastName: string,
    repoUrl: string,
    role: 'STUDENT' | 'ADMIN'
}
export const meGet = async () => {
    const response = await fetch(useAppConfigStore().backendUrl + '/api/me', {
        method: 'GET',
        credentials: 'include'
    });

    return await response.json() as MeResponse | null;

}