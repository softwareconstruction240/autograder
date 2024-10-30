import { type Config, useAppConfigStore } from '@/stores/appConfig'
import {Phase, type RubricInfo, type RubricType} from '@/types/types'
import { useAuthStore } from '@/stores/auth'
import { ServerCommunicator } from '@/network/ServerCommunicator'

export const getConfig = async ():Promise<Config> => {
  let endpoint = "/api"
  if (useAuthStore().user?.role == 'ADMIN') {
    endpoint += "/admin"
  }
  endpoint += "/config"

  return await ServerCommunicator.getRequest<Config>(endpoint)
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

export const setCanvasCourseIds = async (): Promise<void> => {
  await doSetConfigItem("GET", "/api/admin/config/courseIds", {});
}

const convertRubricInfoToObj = (rubricInfo: Map<Phase, Map<RubricType, RubricInfo>>): object => {
    let obj: any = {};
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
  if (method == "GET") {
    await ServerCommunicator.getRequest(path, false)
  } else {
    await ServerCommunicator.postRequest(path, body, false)
  }
  await useAppConfigStore().updateConfig();
}
