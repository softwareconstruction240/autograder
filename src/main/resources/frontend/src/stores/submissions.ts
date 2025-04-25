import { defineStore } from "pinia";
import { ref } from "vue";
import { Phase, type Submission } from "@/types/types";
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

  const loadAllSubmissions = async () => {
    const submissions = await submissionsGet(null);
    if (!submissions) return;

    for (const value of Object.values(Phase)) {
      submissionsByPhase.value[value] ||= [];
    }

    submissions.forEach((submission) => {
      submissionsByPhase.value[submission.phase].push(submission);
    });
  };

  const addSubmission = (sub: Submission) => {
    submissionsByPhase.value[sub.phase].push(sub);
  };

  const currentlyGrading = ref(false);
  const checkGrading = async () => {
    currentlyGrading.value = await submitGet();
  };

  const getLastSubmission = () => lastSubmissionGet();

  return {
    submissionsByPhase,
    getSubmissions,
    loadAllSubmissions,
    addSubmission,
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
