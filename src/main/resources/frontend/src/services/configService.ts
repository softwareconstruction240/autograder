import { type Config, useAppConfigStore } from '@/stores/appConfig'
import {Phase, type RubricInfo, type RubricType} from '@/types/types'
import { useAuthStore } from '@/stores/auth'

export const getConfig = async ():Promise<Config> => {
  let path = "/api"
  if (useAuthStore().user?.role == 'ADMIN') {
    path += "/admin"
  }
  path += "/config"

  try {
    const response = await fetch(useAppConfigStore().backendUrl + path, {
      method: 'GET',
      credentials: 'include'
    });

    return await response.json();
  } catch (e) {
    console.error('Failed to get configuration: ', e);
    throw "Failed to get configuration"
  }
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
    await doSetConfigItem("GET", "/api/admin/config/courseIds", null);
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

const doSetConfigItem = async (method: string, path: string, body: Object | null): Promise<void> => {
    const baseOptions: RequestInit = {
        method: method,
        credentials: 'include',
        headers: {
            'Content-Type': 'application/json'
        }
    };
    const fetchOptions: RequestInit = (method !== "GET")
        ? {
            ...baseOptions,
            body: JSON.stringify(body)
        } : baseOptions;

    const response = await fetch(useAppConfigStore().backendUrl + path, fetchOptions);

    if (!response.ok) {
        console.error(response);
        throw new Error(await response.text());
    }
    await useAppConfigStore().updateConfig();
}
