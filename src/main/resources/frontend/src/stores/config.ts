import { reactive, readonly, ref, type Ref } from 'vue'
import { defineStore } from 'pinia'
import {Phase, type RubricInfo, type RubricType} from '@/types/types'
import { getAdminConfig, getPublicConfig } from '@/services/configService'
import { useAuthStore } from '@/stores/auth'

type ImportMeta = {
    VITE_APP_BACKEND_URL: string
}

/**
 * Config available to be read by any user
 */
export type PublicConfig = {
  banner: {
    message: string
    link: string
    color: string
    expiration: string
  },

  shutdown: {
    timestamp: string
    warningMilliseconds: number
  },

  livePhases: Array<Phase>
}

/**
 * Config available to be read only by admins
 */
export type PrivateConfig = {
  penalty: {
    perDayLatePenalty: number
    gitCommitPenalty: number
    maxLateDaysPenalized: number
    linesChangedPerCommit: number
    clockForgivenessMinutes: number
  },

  ids?: {
    courseNumber?: number
    assignmentIds?: string // Map<Phase, number>
    rubricInfo?: string // Map<Phase, Map<RubricType, RubricInfo>>
  }
}

// @ts-ignore
const env: ImportMeta = import.meta.env;
export const useConfigStore = defineStore('config', () => {

  const publicConfig = reactive<PublicConfig>({
    banner: {
      message: '',
      link: '',
      color: '',
      expiration: ''
    },

    shutdown: {
      timestamp: '',
      warningMilliseconds: 0
    },

    livePhases: []
  })

  const privateConfig = reactive<PrivateConfig>({
    penalty: {
      perDayLatePenalty: -1,
      gitCommitPenalty: -1,
      maxLateDaysPenalized: -1,
      linesChangedPerCommit: -1,
      clockForgivenessMinutes: -1,
    }
  })

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
    if (useAuthStore().isLoggedIn) await updateAdminConfig();
    await updatePublicConfig()
  }

  const updatePublicConfig = async () => {
    const latestPublicConfig: PublicConfig = await getPublicConfig();

    Object.assign(publicConfig, latestPublicConfig)

    // Backend lets the front end choose the default banner color
    if (!publicConfig.banner.color) publicConfig.banner.color = "#4fa0ff"
  }

  const updateAdminConfig = async () => {
    const latestAdminConfig = await getAdminConfig();

    Object.assign(privateConfig, latestAdminConfig)
  }

  const backendUrl = ref<string>(env.VITE_APP_BACKEND_URL);

  // using the enum, if phaseActivationList[phase] == true, then that phase is active
  const activePhaseList: Ref<boolean[]> = ref<Array<boolean>>([]);
  const assignmentIds: Ref<Map<Phase, number>> = ref<Map<Phase, number>>(new Map<Phase, number>);
  const rubricInfo: Ref<Map<Phase, Map<RubricType, RubricInfo>>> =
      ref<Map<Phase, Map<RubricType, RubricInfo>>>(new Map<Phase, Map<RubricType, RubricInfo>>);
  const courseNumber: Ref<number> = ref<number>(-1);

  return {
    updateConfig,
    updatePublicConfig,
    updateAdminConfig,
    backendUrl: readonly(backendUrl),
    public: readonly(publicConfig),
    admin: readonly(privateConfig)
  };
})
