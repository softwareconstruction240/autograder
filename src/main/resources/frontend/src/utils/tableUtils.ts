import type { ValueGetterParams } from 'ag-grid-community'
import {
  commitVerificationFailed,
  generateClickableLink,
  scoreToPercentage,
  simpleTimestamp
} from '@/utils/utils'

/**
 * @param params is an ValueGetterParams Object from AG-Grid that contains a field called "phase"
 */
export const renderPhaseCell = (params: ValueGetterParams): string => {
  return params.data.phase.toLowerCase().replace('phase', '')
}

/**
 * @param params is an ValueGetterParams Object from AG-Grid that contains a field called "timestamp"
 */
export const renderTimestampCell = (params: ValueGetterParams): string => {
  return simpleTimestamp(params.data.timestamp)
}

/**
 * @param params is an ValueGetterParams Object from AG-Grid that contains a field called "repoUrl"
 */
export const renderRepoLinkCell = (params: ValueGetterParams): string => {
  return params.data.repoUrl ? generateClickableLink(params.data.repoUrl) : ''
}

/**
 * Defines how score cells are render in tables throughout the program
 * @param params is an ValueGetterParams Object from AG-Grid that contains a field called "score"
 */
export const renderScoreCell = (params: ValueGetterParams) => {
  const cellElement = document.createElement('div')
  const iconElement = document.createElement('i')
  let scoreText = ''

  if (!params.data.passed) {
    iconElement.classList.add('fa-solid', 'fa-ban')
    iconElement.style.color = 'grey'
    scoreText = ' N/A'
  } else if (commitVerificationFailed(params.data)) {
    cellElement.style.fontWeight = 'bold'
    iconElement.classList.add('fa-solid', 'fa-circle-exclamation')
    iconElement.style.color = 'red'
    scoreText = ' TA'
  } else {
    iconElement.classList.add('fa-solid', 'fa-check')
    iconElement.style.color = 'green'
    scoreText = ' ' + scoreToPercentage(params.data.score)
  }

  cellElement.append(iconElement)
  cellElement.append(scoreText)
  return cellElement
}

export const standardColSettings = {
  sortable: true,
  filter: true,
  autoHeight: true
}

export const wrappingColSettings = {
  wrapText: true,
  cellStyle: { wordBreak: 'normal', lineHeight: 'unset' }
}
