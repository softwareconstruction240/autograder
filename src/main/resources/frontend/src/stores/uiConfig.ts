import { Phase } from "@/types/types";

const getPhaseName = (phase: Phase | null) => {
  switch (phase) {
    case Phase.Phase0:
      return "Phase 0: Move Rules";
    case Phase.Phase1:
      return "Phase 1: Game Logic";
    case Phase.Phase3:
      return "Phase 3: Web API";
    case Phase.Phase4:
      return "Phase 4: SQL DAOs";
    case Phase.Phase5:
      return "Phase 5: Chess Client";
    case Phase.Phase6:
      return "Phase 6: Gameplay";
    case Phase.Quality:
      return "Code Quality Check";
    case Phase.GitHub:
      return "Chess GitHub Repository";
    default:
      return "Chess Project";
  }
};

const getSpecLink = (phase: Phase | null) => {
  switch (phase) {
    case Phase.Phase0:
      return "https://github.com/softwareconstruction240/softwareconstruction/blob/main/chess/0-chess-moves/chess-moves.md";
    case Phase.Phase1:
      return "https://github.com/softwareconstruction240/softwareconstruction/blob/main/chess/1-chess-game/chess-game.md";
    case Phase.Phase3:
      return "https://github.com/softwareconstruction240/softwareconstruction/blob/main/chess/3-web-api/web-api.md";
    case Phase.Phase4:
      return "https://github.com/softwareconstruction240/softwareconstruction/blob/main/chess/4-database/database.md";
    case Phase.Phase5:
      return "https://github.com/softwareconstruction240/softwareconstruction/blob/main/chess/5-pregame/pregame.md";
    case Phase.Phase6:
      return "https://github.com/softwareconstruction240/softwareconstruction/blob/main/chess/6-gameplay/gameplay.md";
    case Phase.Quality:
      return "https://github.com/softwareconstruction240/softwareconstruction/blob/main/chess/code-quality-rubric.md";
    case Phase.GitHub:
      return "https://github.com/softwareconstruction240/softwareconstruction/blob/main/chess/chess-github-repository/chess-github-repository.md";
    default:
      return "https://github.com/softwareconstruction240/softwareconstruction/blob/main/chess/chess.md#readme";
  }
};

export const uiConfig = {
  getPhaseName: getPhaseName,
  getSpecLink: getSpecLink,
  links: {
    helpQueue: "https://students.cs.byu.edu/~cs240ta/helpqueueDelta/",
    canvas: "https://byu.instructure.com",
  },
};
