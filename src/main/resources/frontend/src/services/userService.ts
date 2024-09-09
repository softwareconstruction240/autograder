import {useAppConfigStore} from "@/stores/appConfig";

export const studentUpdateRepo = async (repoUrl: String): Promise<void> => {
  const response = await fetch(useAppConfigStore().backendUrl + '/api/repo', {
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