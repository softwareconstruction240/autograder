import type { RepoUpdate } from '@/types/types'
import { ServerCommunicator } from '@/network/ServerCommunicator'

export const repoHistoryGet = async (netId: String): Promise<RepoUpdate[]> => {
  try {
    return await ServerCommunicator.getRequest<RepoUpdate[]>('/api/admin/repo/history?netId=' + netId)
  } catch (e) {
    return []
  }
};

export const studentUpdateRepoPatch = async (repoUrl: string): Promise<void> => {
  await updateRepoPatch(repoUrl, '/api/repo')
}

export const adminUpdateRepoPatch = async (repoUrl: string, netId: String): Promise<void> => {
  await updateRepoPatch(repoUrl, "/api/admin/repo/" + netId)
}

const updateRepoPatch = async (repoUrl: string, endpoint: string): Promise<void> => {
  await ServerCommunicator.patchRequest(endpoint, {
    "repoUrl": repoUrl
  }, false)
}