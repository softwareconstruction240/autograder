import type { RepoUpdate } from "@/types/types";
import { ServerCommunicator } from "@/network/ServerCommunicator";

export const repoHistoryGet = (netId: String): Promise<RepoUpdate[]> => {
  return ServerCommunicator.getRequestGuaranteed<RepoUpdate[]>(
    "/api/admin/repo/history?netId=" + netId,
    [],
  );
};

export const studentUpdateRepo = (repoUrl: string): Promise<null> => {
  return updateRepoPost(repoUrl, "/api/repo");
};

export const adminUpdateRepo = (repoUrl: string, netId: String): Promise<null> => {
  return updateRepoPost(repoUrl, "/api/admin/repo/" + netId);
};

const updateRepoPost = (repoUrl: string, endpoint: string): Promise<null> => {
  return ServerCommunicator.postRequest(
    endpoint,
    {
      repoUrl: repoUrl,
    },
    false,
  );
};
