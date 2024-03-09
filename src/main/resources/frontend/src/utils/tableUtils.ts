import type { ValueGetterParams } from 'ag-grid-community'
import {useAdminStore} from "@/stores/admin";
import type {RubricItem, RubricItemResults, Submission} from "@/types/types";
import {onRenderTracked} from "vue";
import {generateClickableLink, nameFromNetId, scoreToPercentage, simpleTimestamp} from "@/utils/utils";

/**
 * @param params is an ValueGetterParams Object from AG-Grid that contains a field called "phase"
 */
export const renderPhaseCell = (params: ValueGetterParams):string => {
    return params.data.phase.toLowerCase().replace("phase", "")
}

/**
 * @param params is an ValueGetterParams Object from AG-Grid that contains a field called "netId"
 */
export const nameCellRender = (params: ValueGetterParams) => {
    return nameFromNetId(params.data.netId)
}

/**
 * @param params is an ValueGetterParams Object from AG-Grid that contains a field called "timestamp"
 */
export const renderTimestampCell = (params: ValueGetterParams):string => {
    return simpleTimestamp(params.data.timestamp)
}

/**
 * @param params is an ValueGetterParams Object from AG-Grid that contains a field called "repoUrl"
 */
export const renderRepoLinkCell = (params: ValueGetterParams):string => {
    return generateClickableLink(params.data.repoUrl)
}

/**
 * @param params is an ValueGetterParams Object from AG-Grid that contains a field called "score"
 */
export const renderScoreCell = (params: ValueGetterParams):string => {
    return scoreToPercentage(params.data.score)
}

type RubricRow = {
    category: string,
    criteria: string,
    notes: string,
    points: string,
    results: RubricItemResults
}

export const loadRubricRows = (submission: Submission) => {
    const possibleRubricItems: RubricItem[] = [
        submission.rubric.passoffTests,
        submission.rubric.unitTests,
        submission.rubric.quality,
    ];
    const rubricRows: any = []
    for (let i = 0; i < possibleRubricItems.length; i++) {
        const item = possibleRubricItems[i]
        if(item) {
            const row: RubricRow = {
                category: item.category,
                criteria: item.criteria,
                notes: item.results.notes,
                points: Math.round(item.results.score) + "/" + item.results.possiblePoints,
                results: item.results
            }
            rubricRows.push(row)
        }
    }
    return rubricRows
}