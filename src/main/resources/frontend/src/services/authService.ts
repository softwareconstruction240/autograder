import { useAuthStore } from '@/stores/auth'
import { ServerCommunicator } from '@/network/ServerCommunicator'

type MeResponse = {
    netId: string,
    firstName: string,
    lastName: string,
    repoUrl: string,
    role: 'STUDENT' | 'ADMIN'
}
export const meGet = async () => {
    try {
        return await ServerCommunicator.getRequest<MeResponse>('/api/me')
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
    await ServerCommunicator.postRequest( "/auth/logout", null, false)
}
