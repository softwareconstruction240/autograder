import {defineStore} from "pinia";
import {ref} from "vue";
import type {Phase, Submission} from "@/types/types";
import {submissionsGet} from "@/services/submissionService";

type SubmissionsByPhase = {
    [phase: string]: Submission[];
}

export const useSubmissionStore = defineStore('submission', () => {
    const submissionsByPhase = ref<SubmissionsByPhase>({});

    const getSubmissions = async (phase: Phase) => {
        submissionsByPhase.value[phase] = await submissionsGet(phase);
    };

    return {
        submissionsByPhase,
        getSubmissions
    };
});