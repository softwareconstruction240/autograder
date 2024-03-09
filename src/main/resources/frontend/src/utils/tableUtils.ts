import type { ValueGetterParams } from 'ag-grid-community'
import {useAdminStore} from "@/stores/admin";

/**
 * @param params is an ValueGetterParams Object from AG-Grid that contains a field called "phase"
 */
export const renderPhaseCell = (params: ValueGetterParams):string => {
    return params.data.phase.toLowerCase().replace("phase", "")
}

/**
 * @param params is an ValueGetterParams Object from AG-Grid that contains a field called "netId"
 */
export const nameFromNetIdCellRender = (params: ValueGetterParams) => {
    const user = useAdminStore().usersByNetId[params.data.netId];
    return `${user.firstName} ${user.lastName}`
}

/**
 * @param params is an ValueGetterParams Object from AG-Grid that contains a field called "timestamp"
 */
export const renderTimestampCell = (params: ValueGetterParams):string => {
    const months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"]
    const time = new Date(params.data.timestamp)
    return months[time.getMonth()] + " " + time.getDate() + " " + time.getHours() + ":" + time.getMinutes()
}

/**
 * @param params is an ValueGetterParams Object from AG-Grid that contains a field called "repoUrl"
 */
export const renderRepoLinkCell = (params: ValueGetterParams):string => {
    return '<a id="repo-link" href="' + params.data.repoUrl + '" target="_blank">' + params.data.repoUrl + '</a>'
}

/**
 * @param params is an ValueGetterParams Object from AG-Grid that contains a field called "score"
 */
export const renderScoreCell = (params: ValueGetterParams):string => {
    return roundTwoDecimals(params.data.score * 100) + "%"
}

const roundTwoDecimals = (num: number) => {
    return Math.round((num + Number.EPSILON) * 100) / 100;
}