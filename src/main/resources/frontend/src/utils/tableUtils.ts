import type { ValueGetterParams } from 'ag-grid-community'
import {useAdminStore} from "@/stores/admin";
import type {Submission} from "@/types/types";

export const renderPhaseCell = (params: ValueGetterParams):string => {
    return params.data.phase.toLowerCase().replace("phase", "")
}

export const nameFromNetIdCellRender = (params: ValueGetterParams) => {
    const user = useAdminStore().usersByNetId[params.data.netId];
    return `${user.firstName} ${user.lastName}`
}

export const renderTimestampCell = (params: ValueGetterParams):string => {
    const months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"]
    const time = new Date(params.data.timestamp)
    return months[time.getMonth()] + " " + time.getDate() + " " + time.getHours() + ":" + time.getMinutes()
}

export const renderRepoLinkCell = (params: ValueGetterParams):string => {
    return '<a id="repo-link" href="' + params.data.repoUrl + '" target="_blank">' + params.data.repoUrl + '</a>'
}

export const renderScoreCell = (params: ValueGetterParams):string => {
    return roundTwoDecimals(params.data.score * 100) + "%"
}

const roundTwoDecimals = (num: number) => {
    return Math.round((num + Number.EPSILON) * 100) / 100;
}