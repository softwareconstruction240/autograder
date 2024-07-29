import { ref, type Ref } from 'vue'
import { defineStore } from 'pinia'
import {listOfPhases, Phase, type RubricInfo, type RubricType} from '@/types/types'
import { getConfig } from '@/services/configService'

type ImportMeta = {
    VITE_APP_BACKEND_URL: string
}

export type Config = {
  bannerMessage: string
  phases: Array<Phase>
  courseNumber?: number
  assignmentIds?: string // Map<Phase, number>
  rubricInfo?: string // Map<Phase, Map<RubricType, RubricInfo>>
}

// @ts-ignore
const env: ImportMeta = import.meta.env;
export const useAppConfigStore = defineStore('appConfig', () => {

  const parseAssignmentIds = (idsString: string): Map<Phase, number> => {
    const idsObject = JSON.parse(idsString);
    return new Map<Phase, number>(Object.entries(idsObject) as unknown as [Phase, number][]);
  }

  const parseRubricInfo = (idsString: string): Map<Phase, Map<RubricType, RubricInfo>> => {
    const idsObject = JSON.parse(idsString);
    const rubricMap = new Map<Phase, Map<RubricType, RubricInfo>>();
    for (const phase in idsObject) {
      rubricMap.set(
          phase as unknown as Phase,
          new Map(Object.entries(idsObject[phase]) as unknown as [RubricType, RubricInfo][])
      );
    }
    return rubricMap;
  }

  const updateConfig = async () => {
    const latestConfig = await getConfig();
    bannerMessage.value = latestConfig.bannerMessage;
    for (const phase of listOfPhases() as Phase[]) {
      activePhaseList.value[phase] = latestConfig.phases.includes(phase);
    }
    if (latestConfig.courseNumber) {
      courseNumber.value = latestConfig.courseNumber;
    }
    if (latestConfig.assignmentIds) {
      assignmentIds.value = parseAssignmentIds(latestConfig.assignmentIds);
    }
    if (latestConfig.rubricInfo) {
      rubricInfo.value = parseRubricInfo(latestConfig.rubricInfo);
    }
  }

  const backendUrl = ref<string>(env.VITE_APP_BACKEND_URL);
  const bannerMessage: Ref<string> = ref<string>("");
  // using the enum, if phaseActivationList[phase] == true, then that phase is active
  const activePhaseList: Ref<boolean[]> = ref<Array<boolean>>([]);
  const assignmentIds: Ref<Map<Phase, number>> = ref<Map<Phase, number>>(new Map<Phase, number>);
  const rubricInfo: Ref<Map<Phase, Map<RubricType, RubricInfo>>> =
      ref<Map<Phase, Map<RubricType, RubricInfo>>>(new Map<Phase, Map<RubricType, RubricInfo>>);
  const courseNumber: Ref<number> = ref<number>(-1);

  return {
    updateConfig,
    backendUrl,
    bannerMessage,
    phaseActivationList: activePhaseList,
    rubricInfo,
    assignmentIds,
    courseNumber
  };
})
