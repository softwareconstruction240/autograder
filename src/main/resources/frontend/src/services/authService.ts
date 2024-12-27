import { useAuthStore } from '@/stores/auth'
import { ServerCommunicator } from '@/network/ServerCommunicator'

type MeResponse = {
    netId: string,
    firstName: string,
    lastName: string,
    repoUrl: string,
    role: 'STUDENT' | 'ADMIN'
}
export const meGet = () => {
    return ServerCommunicator.getRequestGuaranteed<MeResponse | null>('/api/me', null)
}

export const loadUser = async () => {
    const loggedInUser = await meGet()
    if (loggedInUser == null)
        return;

    useAuthStore().user = loggedInUser;
}

export const logoutPost = () => {
    return ServerCommunicator.postRequest( "/auth/logout", null, false)
}
