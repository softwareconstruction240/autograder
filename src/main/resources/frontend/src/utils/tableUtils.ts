import type { ValueGetterParams } from 'ag-grid-community'

export const renderPhaseCell = (params: ValueGetterParams):string => {
    return params.data.phase.toLowerCase().replace("phase", "")
}

export const renderTimestampCell = (params: ValueGetterParams):string => {
    const months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"]
    const time = params.data.time
    return months[time.getMonth()] + " " + time.getDate() + " " + time.getHours() + ":" + time.getMinutes()
}

export const renderRepoLinkCell = (params: ValueGetterParams):string => {
    return '<a id="repo-link" href="' + params.data.github + '" target="_blank">' + params.data.github + '</a>'
}

export const renderScoreCell = (params: ValueGetterParams):string => {
    return roundTwoDecimals(params.data.score * 100) + "%"
}

const roundTwoDecimals = (num: number) => {
    return Math.round((num + Number.EPSILON) * 100) / 100;
}