import { type PrivateConfig, type PublicConfig, useConfigStore } from '@/stores/config'
import {Phase, type RubricInfo, type RubricType} from '@/types/types'
import { ServerCommunicator } from '@/network/ServerCommunicator'
import { ServerError } from '@/network/ServerError'

export const getPublicConfig = async ():Promise<PublicConfig> => {
  return await ServerCommunicator.getRequest<PublicConfig>("/api/config")
}

export const getAdminConfig = async ():Promise<PrivateConfig> => {
  return await ServerCommunicator.getRequest<PrivateConfig>("/api/admin/config")
}

export const setPenalties = async (maxLateDaysPenalized: number,
                                   gitCommitPenalty: number,
                                   perDayLatePenalty: number,
                                   linesChangedPerCommit: number,
                                   clockForgivenessMinutes: number) => {
  await doSetConfigItem("POST", '/api/admin/config/penalties', {
    maxLateDaysPenalized: maxLateDaysPenalized,
    gitCommitPenalty: gitCommitPenalty,
    perDayLatePenalty: perDayLatePenalty,
    linesChangedPerCommit: linesChangedPerCommit,
    clockForgivenessMinutes: clockForgivenessMinutes
  })
}

export const setBanner = async (message: String, link: String, color: String, expirationTimestamp: String): Promise<void> => {
  await doSetConfigItem("POST", '/api/admin/config/banner', {
    "bannerMessage": message,
    "bannerLink": link,
    "bannerColor": color,
    "bannerExpiration": expirationTimestamp
    }
  );
}

export const setLivePhases = async (phases: Array<Phase>): Promise<void> => {
  await doSetConfigItem("POST", '/api/admin/config/phases', {"phases": phases});
}

export const setGraderShutdown = async (shutdownTimestamp: string, shutdownWarningHours: number): Promise<void> => {
  if (shutdownWarningHours < 0) shutdownWarningHours = 0

  await doSetConfigItem("POST", "/api/admin/config/phases/shutdown", {
    "shutdownTimestamp": shutdownTimestamp,
    "shutdownWarningMilliseconds": Math.trunc(shutdownWarningHours * 60 * 60 * 1000) // convert to milliseconds
  })
}

export const setCanvasCourseIds = async (): Promise<void> => {
  await doSetConfigItem("GET", "/api/admin/config/courseIds", {});
}

const convertRubricInfoToObj = (rubricInfo: Map<Phase, Map<RubricType, RubricInfo>>): object => {
    const obj: any = {};
    rubricInfo.forEach((rubricTypeMap, phase) => {
        obj[phase] = Object.fromEntries(rubricTypeMap.entries());
    });
    return obj;
}

export const setCourseIds = async (
    courseNumber: number,
    assignmentIds: Map<Phase, number>,
    rubricInfo: Map<Phase, Map<RubricType, RubricInfo>>
): Promise<void> => {
    const body = {
        "courseNumber": courseNumber,
        "assignmentIds": Object.fromEntries(assignmentIds.entries()),
        "rubricInfo": convertRubricInfoToObj(rubricInfo)
    };
    await doSetConfigItem("POST", "/api/admin/config/courseIds", body);
}

const doSetConfigItem = async (method: string, path: string, body: Object): Promise<void> => {
  try {
    if (method == "GET") {
      await ServerCommunicator.getRequest(path, false)
    } else {
      await ServerCommunicator.postRequest(path, body, false)
    }
  } catch (e) {
    if (e instanceof ServerError) {
      alert(e.message)
    }
  }

  await useConfigStore().updateConfig();
}
