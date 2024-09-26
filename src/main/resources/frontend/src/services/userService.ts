import {useAppConfigStore} from "@/stores/appConfig";
import type { RepoUpdate } from '@/types/types'

export const repoHistoryGet = async (netId: String): Promise<RepoUpdate[]> => {
  let url = useAppConfigStore().backendUrl + '/api/admin/repo/history?netId=' + netId
  const response = await fetch(url, {
    method: 'GET',
    credentials: 'include'
  });

  if (!response.ok) {
    console.error(response);
    return [];
  }

  return await response.json() as RepoUpdate[];
};

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