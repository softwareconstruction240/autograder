import type { Phase } from '@/types/types'


const getPhaseName = (phase: Phase) => {
  switch (phase) {
    case '0':
      return "Phase 0: Move Rules"
    case '1':
      return "Phase 1: Game Logic"
    case '3':
      return "Phase 3: Web API"
    case '4':
      return "Phase 4: SQL DAOs"
    case '5':
      return "Phase 5: Chess Client"
    case '6':
      return "Phase 6: Idk yet Paul"
  }
}

export const uiConfig = {
  getPhaseName: getPhaseName
}

