import { defineStore } from "pinia";
import { ref } from "vue";
import type { Phase, Submission } from "@/types/types";
import { lastSubmissionGet, submissionsGet, submitGet } from "@/services/submissionService";
import { useConfigStore } from "@/stores/config";
import { useAuthStore } from "@/stores/auth";

type SubmissionsByPhase = {
  [phase: string]: Submission[];
};

export const useSubmissionStore = defineStore("submission", () => {
  const submissionsByPhase = ref<SubmissionsByPhase>({});

  const getSubmissions = async (phase: Phase) => {
    submissionsByPhase.value[phase] = await submissionsGet(phase);
  };

  const getAllSubmissions = async () => {
    const submissions = await submissionsGet(null);

    submissions.forEach((submission) => {
        submissionsByPhase.value[submission.phase].push(submission);
    });

    return submissionsByPhase;
  };

  const currentlyGrading = ref(false);
  const checkGrading = async () => {
    currentlyGrading.value = await submitGet();
  };

  const getLastSubmission = () => lastSubmissionGet();

  return {
    submissionsByPhase,
    getSubmissions,
    getAllSubmissions,
    currentlyGrading,
    checkGrading,
    getLastSubmission,
  };
});

export const subscribeToGradingUpdates = (eventHandler: (event: MessageEvent) => void) => {
  const wsBackendUrl = useConfigStore().backendUrl.replace(/^http/, "ws") + "/ws";
  const ws = new WebSocket(wsBackendUrl);
  ws.onopen = () => {
    const token = useAuthStore().token;
    ws.send(token);
  };
  ws.onmessage = (event) => {
    eventHandler(event);
  };
  ws.onerror = (event) => {
    console.error("ws error", event);
  };
};
