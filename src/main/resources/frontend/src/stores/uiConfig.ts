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

const getSpecLink = (phase: Phase | null) => {
  switch (phase) {
    case '0':
      return "https://github.com/softwareconstruction240/softwareconstruction/blob/main/chess/0-chess-moves/chess-moves.md"
    case '1':
      return "https://github.com/softwareconstruction240/softwareconstruction/blob/main/chess/1-chess-game/chess-game.md"
    case '3':
      return "https://github.com/softwareconstruction240/softwareconstruction/blob/main/chess/3-web-api/web-api.md"
    case '4':
      return "https://github.com/softwareconstruction240/softwareconstruction/blob/main/chess/4-database/database.md"
    case '5':
      return "https://github.com/softwareconstruction240/softwareconstruction/blob/main/chess/5-pregame/pregame.md"
    case '6':
      return "https://github.com/softwareconstruction240/softwareconstruction/blob/main/chess/6-gameplay/gameplay.md"
    default:
      return "https://github.com/softwareconstruction240/softwareconstruction/blob/main/chess/chess.md#readme"
  }
}

export const uiConfig = {
  getPhaseName: getPhaseName,
  getSpecLink: getSpecLink,
  links: {
    helpQueue: "https://students.cs.byu.edu/~cs240ta/helpqueueDelta/",
    canvas: "https://byu.instructure.com",
  }
}

