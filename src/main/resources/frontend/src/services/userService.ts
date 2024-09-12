import {useAppConfigStore} from "@/stores/appConfig";
import type { User } from '@/types/types'

export const userGet = async (netId: String): Promise<User | null> => {
  const response = await fetch(useAppConfigStore().backendUrl + "/api/admin/user/" + netId, {
    method: 'GET',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json'
    }
  });

  if (!response.ok) {
    console.error(response);
    throw new Error(await response.text());
  }

  return await response.json() as User | null;
}

export const studentUpdateRepoPatch = async (repoUrl: String): Promise<void> => {
  await updateRepoPatch(repoUrl, '/api/repo')
}

export const adminUpdateRepoPatch = async (repoUrl: String, netId: String): Promise<void> => {
  await updateRepoPatch(repoUrl, "/api/admin/repo/" + netId)
}

const updateRepoPatch = async (repoUrl: String, endpoint: String): Promise<void> => {
  const response = await fetch(useAppConfigStore().backendUrl + endpoint, {
    method: 'PATCH',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      "repoUrl": repoUrl
    })
  });

  if (!response.ok) {
    console.error(response);
    throw new Error(await response.text());
  }
}