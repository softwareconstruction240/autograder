import {useAppConfigStore} from "@/stores/appConfig";

export const studentUpdateRepo = async (repoUrl: String): Promise<void> => {
  await updateRepo(repoUrl, '/api/repo')
}

export const adminUpdateRepo = async (repoUrl: String, netId: String): Promise<void> => {
  await updateRepo(repoUrl, "/api/admin/repo/" + netId)
}

const updateRepo = async (repoUrl: String, endpoint: String): Promise<void> => {
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