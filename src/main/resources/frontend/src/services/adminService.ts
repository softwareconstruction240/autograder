import {useAppConfigStore} from "@/stores/appConfig";
import type {User} from "@/types/types";

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