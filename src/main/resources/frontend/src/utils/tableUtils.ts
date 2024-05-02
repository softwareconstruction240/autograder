import type { ValueGetterParams } from 'ag-grid-community'
import type {RubricItem, RubricItemResults, Submission} from "@/types/types";
import {generateClickableLink, scoreToPercentage, simpleTimestamp} from "@/utils/utils";

/**
 * @param params is an ValueGetterParams Object from AG-Grid that contains a field called "phase"
 */
export const renderPhaseCell = (params: ValueGetterParams):string => {
    return params.data.phase.toLowerCase().replace("phase", "")
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
 * Defines how score cells are render in tables throughout the program
 * @param params is an ValueGetterParams Object from AG-Grid that contains a field called "score"
 */
export const renderScoreCell = (params: ValueGetterParams) => {
    const cellElement = document.createElement("div");
    const iconElement = document.createElement("i");

    // TODO: When git blocking gets implemented, change this to (!params.data.submission.approved)
    if (false) {
        cellElement.style.fontWeight = "bold"
        iconElement.classList.add("fa-solid", "fa-circle-exclamation");
        iconElement.style.color = "var(--failure-color)";
    } else if (params.data.submission.passed) {
        iconElement.classList.add("fa-solid", "fa-check");
        iconElement.style.color = "var(--success-color)";
    } else {
        iconElement.classList.add("fa-solid", "fa-ban");
        iconElement.style.color = "var(--plain-500)"
    }

    cellElement.append(iconElement)
    cellElement.append( " " + scoreToPercentage(params.data.score) );
    return cellElement
}

export const standardColSettings = {
    sortable: true,
    filter: true,
    autoHeight:true,
}

export const wrappingColSettings = {
    wrapText: true,
    cellStyle: {"wordBreak": "normal", "lineHeight": "unset"}
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