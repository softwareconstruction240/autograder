import {useAppConfigStore} from "@/stores/appConfig";

export const logsGet = async (): Promise<string[]> => {
    try {
        const response = await fetch(useAppConfigStore().backendUrl + '/api/admin/logs', {
            method: 'GET',
            credentials: 'include'
        });

        return await response.json();
    } catch (e) {
        return [];
    }
};

export const logGet = async (logFile: string): Promise<string> => {
    const response = await fetch(useAppConfigStore().backendUrl + '/api/admin/logs/' + logFile, {
        method: 'GET',
        credentials: 'include'
    });

    if (!response.ok) {
        console.error(response);
        return "Could not fetch log " + logFile;
    }

    return response.text();
};