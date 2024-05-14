import { type Config, useAppConfigStore } from '@/stores/appConfig'
import { Phase } from '@/types/types'
import { useAuthStore } from '@/stores/auth'

export const getConfig = async ():Promise<Config> => {
  let path = "/api"
  if (useAuthStore().user?.role == 'ADMIN') {
    path += "/admin"
  }
  path += "/config"

  try {
    const response = await fetch(useAppConfigStore().backendUrl + path, {
      method: 'GET',
      credentials: 'include'
    });

    return await response.json();
  } catch (e) {
    console.error('Failed to get configuration: ', e);
    throw "Failed to get configuration"
  }
}

export const setBannerMessage = async (message: String): Promise<void> => {
  const response = await fetch(useAppConfigStore().backendUrl + '/api/admin/config/banner', {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      "bannerMessage": message,
    })
  });

  if (!response.ok) {
    console.error(response);
    throw new Error(await response.text());
  }
  await useAppConfigStore().updateConfig();
}

export const setLivePhases = async (phases: Array<Phase>): Promise<void> => {
  const response = await fetch(useAppConfigStore().backendUrl + '/api/admin/config/phases', {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      "phases": phases,
    })
  });

  if (!response.ok) {
    console.error(response);
    throw new Error(await response.text());
  }
  await useAppConfigStore().updateConfig();
}