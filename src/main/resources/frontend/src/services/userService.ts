import type { RepoUpdate } from '@/types/types'
import { ServerCommunicator } from '@/network/ServerCommunicator'

export const repoHistoryGet = (netId: String): Promise<RepoUpdate[]> => {
  return ServerCommunicator.getRequestGuaranteed<RepoUpdate[]>('/api/admin/repo/history?netId=' + netId, [])
};

export const studentUpdateRepoPatch = (repoUrl: string): Promise<null> => {
  return updateRepoPatch(repoUrl, '/api/repo')
}

export const adminUpdateRepoPatch = (repoUrl: string, netId: String): Promise<null> => {
  return updateRepoPatch(repoUrl, "/api/admin/repo/" + netId)
}

const updateRepoPatch = (repoUrl: string, endpoint: string): Promise<null> => {
  return ServerCommunicator.patchRequest(endpoint, {
    "repoUrl": repoUrl
  }, false)
}
