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