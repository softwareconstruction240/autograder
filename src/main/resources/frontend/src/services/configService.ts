import { useAppConfigStore } from '@/stores/appConfig'
import type { Config } from '@/types/types'

export const getConfig = async ():Promise<Config> => {
  try {
    const response = await fetch(useAppConfigStore().backendUrl + '/api/admin/config', {
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
}