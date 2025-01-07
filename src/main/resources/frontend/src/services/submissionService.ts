import type { Submission } from '@/types/types';
import { Phase } from '@/types/types';
import { ServerCommunicator } from '@/network/ServerCommunicator';

export const submissionsGet = (phase: Phase | null): Promise<Submission[]> => {
  const endpoint: string = '/api/submission' + (phase === null ? '' : '/' + Phase[phase]);
  return ServerCommunicator.getRequestGuaranteed<Submission[]>(endpoint, []);
};

export const lastSubmissionGet = (): Promise<Submission | null> => {
  return ServerCommunicator.getRequestGuaranteed<Submission | null>('/api/latest', null);
};

export const submissionPost = (phase: Phase): Promise<null> => {
  return ServerCommunicator.postRequest('/api/submit', { phase: Phase[phase] }, false);
};

export const adminSubmissionPost = (phase: Phase, repoUrl: String): Promise<null> => {
  return ServerCommunicator.postRequest(
    '/api/admin/submit',
    {
      phase: Phase[phase],
      repoUrl: repoUrl,
    },
    false,
  );
};

type SubmitGetResponse = {
  inQueue: boolean;
};
export const submitGet = async (): Promise<boolean> => {
  return (await ServerCommunicator.getRequest<SubmitGetResponse>('/api/submit')).inQueue;
};

export const reRunSubmissionsPost = () => {
  return ServerCommunicator.postRequest('/api/admin/submissions/rerun');
};
