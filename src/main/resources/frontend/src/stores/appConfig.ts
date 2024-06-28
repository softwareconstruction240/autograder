import { ref, type Ref } from 'vue'
import { defineStore } from 'pinia'
import { listOfPhases, Phase } from '@/types/types'
import { getConfig } from '@/services/configService'

type ImportMeta = {
    VITE_APP_BACKEND_URL: string
}

export type Config = {
  bannerMessage: string
  phases: Array<Phase>
}

// @ts-ignore
const env: ImportMeta = import.meta.env;
export const useAppConfigStore = defineStore('appConfig', () => {
  const backendUrl = ref<string>(env.VITE_APP_BACKEND_URL);

  const updateConfig = async () => {
    const latestConfig = await getConfig();

    bannerMessage.value = latestConfig.bannerMessage
    for (const phase of listOfPhases() as Phase[]) {
      activePhaseList.value[phase] = latestConfig.phases.includes(phase);
    }
  }

  const bannerMessage: Ref<string> = ref<string>("")
  // using the enum, if phaseActivationList[phase] == true, then that phase is active
  const activePhaseList: Ref<boolean[]> = ref<Array<boolean>>([])

  return { updateConfig, backendUrl, bannerMessage, phaseActivationList: activePhaseList }
})
