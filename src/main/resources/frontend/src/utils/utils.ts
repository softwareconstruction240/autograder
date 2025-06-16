import { useAdminStore } from "@/stores/admin";
import {
  type CoverageAnalysis,
  Phase,
  type RubricItem,
  type RubricItemResults,
  type RubricType,
  type Submission,
  type TestNode,
  VerifiedStatus,
} from "@/types/types";
import { useAuthStore } from "@/stores/auth";

const months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];

export const commitVerificationFailed = (submission: Submission) => {
  if (submission.admin) return false; // Admin submissions don't have commit requirements
  if (!submission.verifiedStatus) {
    // old submissions lack this info
    console.error("submission from " + submission.netId + " has no verification info");
    return false;
  }
  return submission.verifiedStatus.toString() === VerifiedStatus[VerifiedStatus.Unapproved];
};

export const simpleDate = (date: Date | string) => {
  const dateObj = typeof date === "string" ? new Date(date + "T00:00:00") : date;
  return months[dateObj.getMonth()] + " " + dateObj.getDate() + " " + dateObj.getFullYear();
};

export const readableTimestamp = (timestampOrString: Date | string) => {
  if (timestampOrString === "never") {
    return "never";
  }

  const timestamp =
    typeof timestampOrString === "string" ? new Date(timestampOrString) : timestampOrString;
  return timestamp.toLocaleString();
};

export const simpleTimestamp = (date: Date | string) => {
  const time = typeof date === "string" ? new Date(date) : date;
  return (
    months[time.getMonth()] +
    " " +
    time.getDate() +
    " " +
    time.getHours() +
    ":" +
    String(time.getMinutes()).padStart(2, "0")
  );
};

/**
 * Creates a time-zone-free timestamp from a date string and time string.
 *
 * If the time string is empty, it sets the time to the end of the day.
 *
 * @param date a date string formatted as YYYY-MM-DD
 * @param time a time string formatted as HH:MM
 */
export const combineDateAndTime = (date: string, time: string) => {
  return `${date}T${time ? time + ":00" : "23:59:59"}`;
};

export const nameFromNetId = (netId: string) => {
  const user = useAdminStore().usersByNetId[netId];
  if (user == null) {
    console.error(
      "Error getting name from netId, either because user with netId " +
        netId +
        " doesn't exist, or you're not logged in as an admin",
    );
    return;
  }
  return `${user.firstName} ${user.lastName}`;
};

/**
 * returns the first and last name of the person who sent in the submission.
 * IMPORTANT NOTE: If this is called from a student user, it will simply return the student's name,
 * without regard to the actual netId on the submission
 * @param submission
 */
export const nameOnSubmission = (submission: Submission) => {
  const user = useAuthStore().user;
  if (!user) {
    console.error("Asking for name on submission while logged out");
    return "?";
  } else if (user.role == "ADMIN") {
    return nameFromNetId(submission.netId);
  } else {
    return user.firstName + " " + user.lastName;
  }
};

export const scoreToPercentage = (score: number) => {
  return roundTwoDecimals(score * 100) + "%";
};

export const roundTwoDecimals = (num: number) => {
  return Math.round((num + Number.EPSILON) * 100) / 100;
};

export const generateClickableLink = (link: string) => {
  return '<a href="' + link + '" target="_blank">' + link.split("://")[1] + "</a>";
};

export const generateClickableCommitLink = (repoLink: string, hash: string) => {
  if (repoLink.endsWith(".git")) {
    repoLink = repoLink.substring(0, repoLink.indexOf(".git"));
  }
  const link = repoLink + (repoLink.charAt(repoLink.length - 1) == "/" ? "" : "/") + "tree/" + hash;
  return '<a href="' + link + '" target="_blank">' + hash.substring(0, 6) + "</a>";
};

export const sanitizeHtml = (str: string) => {
  str = str.replace(/&/g, "&amp;");
  str = str.replace(/</g, "&lt;");
  str = str.replace(/>/g, "&gt;");
  str = str.replace(/"/g, "&quot;");
  return str.replace(/\n/g, "<br />");
};

export const generateResultsHtmlStringFromTestNode = (node: TestNode, indent: string) => {
  let result = indent + node.testName;

  if (node.passed !== undefined) {
    result += node.passed ? ` <span class="success">✓</span>` : ` <span class="failure">✗</span>`;
    if (node.errorMessage !== null && node.errorMessage !== undefined && node.errorMessage !== "") {
      result += `<br/>${indent}   ↳<span class="failure">${sanitizeHtml(node.errorMessage)}</span>`;
    }
  } else {
    result += ` (${node.numTestsPassed} passed, ${node.numTestsFailed} failed)`;
  }
  result += "<br/>";

  for (const key in node.children) {
    if (Object.prototype.hasOwnProperty.call(node.children, key)) {
      result += generateResultsHtmlStringFromTestNode(
        node.children[key],
        indent + "&nbsp;&nbsp;&nbsp;&nbsp;",
      );
    }
  }

  return result;
};

const proportionToColor = (proportion: number) => {
  /*
  Was having a grand old time with math functions for this.
  The point is that at 0% coverage it's max red, at 100% coverage it's max green
      (although 255 green is really bright so 210 for now)
  At 50% coverage it's 50% green but still about 70% red
  */
  const red = 255 * Math.cos((proportion * Math.PI) / 2);
  const green = 210 * proportion;
  return `rgb(${red}, ${green}, 0)`;
};

export const generateCoverageHtmlStringFromCoverage = (coverage: CoverageAnalysis) => {
  coverage.classAnalyses.sort((a, b) => {
    if (a.packageName === b.packageName) {
      return a.className.localeCompare(b.className);
    }
    return a.packageName.localeCompare(b.packageName);
  });

  let out = "<br>Coverage:<br>";
  for (const classAnalysis of coverage.classAnalyses) {
    const total = classAnalysis.covered + classAnalysis.missed;
    if (total > 0) {
      const coveredProportion = classAnalysis.covered / total;
      out += `<span style="color: ${proportionToColor(coveredProportion)}">${classAnalysis.packageName}.${classAnalysis.className}: ${classAnalysis.covered} / ${total}</span><br>`;
    }
  }
  return out;
};

export const phaseString = (phase: Phase | "Quality" | "GitHub") => {
  if (phase == "Quality") {
    return "Code Quality Check";
  }
  if (phase == "GitHub") {
    return "Chess GitHub Repository";
  } else {
    return "Phase " + phase.toString().charAt(5);
  }
};

export const sortedItems = (items: Record<RubricType, RubricItem>): RubricItem[] => {
  return Object.keys(items)
    .sort((a, b) => {
      const aPoints = items[a as RubricType].results.possiblePoints;
      const bPoints = items[b as RubricType].results.possiblePoints;
      return bPoints - aPoints;
    })
    .map((item) => items[item as RubricType]);
};

export const isPhaseGraded = (phase: Phase | "Quality" | "GitHub"): boolean => {
  return phase !== "Quality";
};

export const getRubricTypes = (phase: Phase | "Quality" | "GitHub"): RubricType[] => {
  if (phase === "GitHub" || phase === Phase.GitHub) {
    return ["GITHUB_REPO"];
  } else if (phase === Phase.Phase0 || phase === Phase.Phase1) {
    return ["PASSOFF_TESTS", "GIT_COMMITS"];
  } else if (phase === Phase.Phase3 || phase === Phase.Phase4) {
    return ["PASSOFF_TESTS", "GIT_COMMITS", "QUALITY", "UNIT_TESTS"];
  } else if (phase === Phase.Phase5) {
    return ["UNIT_TESTS", "QUALITY", "GIT_COMMITS"];
  } else if (phase === Phase.Phase6) {
    return ["PASSOFF_TESTS", "QUALITY", "GIT_COMMITS"];
  }
  return [];
};

export const convertPhaseStringToEnum = (phaseAsString: string): Phase => {
  return Phase[phaseAsString as keyof typeof Phase];
};

export const convertRubricTypeToHumanReadable = (rubricType: RubricType): string => {
  const words = rubricType
    .split("_")
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase());
  return words.join(" ");
};

export const isPlausibleRepoUrl = (url: string): boolean => {
  // NOTE: This restriction on accepting "github.com" urls is arbitrary.
  // The back-end service can function with any link usable by `git clone <URL>`
  return !!url && url.toLowerCase().includes("github.com");
};

export const assureHttpPrefix = (url: string): string => {
  if (/^https?:\/\//.test(url)) return url;
  return `https://${url}`;
};

export const submissionScoreDisplayText = (submission: Submission, inLine: boolean): string => {
  const failedCommitVerification = commitVerificationFailed(submission);
  const penalized = submission.rawScore && submission.rawScore != submission.score;
  let commitVerificationText = "";
  let penaltyText = "after penalties";

  if (failedCommitVerification) {
    commitVerificationText = `Score withheld for commits<br>Score Before Penalt${penalized ? "ies" : "y"}: `;
    penaltyText = "after other penalties";
  }

  let scoreText = scoreToPercentage(submission.score);
  if (!inLine && !failedCommitVerification) {
    scoreText = "<b>" + scoreText + "</b>";
  }

  if (penalized) {
    const separator = failedCommitVerification && !inLine ? "<br>" : " ";
    let rawScoreText = scoreToPercentage(submission.rawScore);
    if (!inLine && !failedCommitVerification) {
      rawScoreText = "<b>" + rawScoreText + "</b>";
    }
    scoreText = `${rawScoreText}${separator}(${penaltyText}: ${scoreText})`;
  }

  return commitVerificationText + scoreText;
};

export const resultsScoreDisplayText = (results: RubricItemResults): string => {
  const score = roundTwoDecimals(results.score) + "/" + results.possiblePoints;
  if (results.rawScore && results.score != results.rawScore) {
    return `${roundTwoDecimals(results.rawScore)}/${results.possiblePoints} (after penalties: ${score})`;
  } else {
    return score;
  }
};

/**
 * Says if the last date in the list chronologically is within a set number of days.
 * Returns true if there are no dates in the list
 * @param dates
 * @param days
 */
export const isLastDateWithinXDays = (dates: Date[] | string[], days: number) => {
  if (!dates.length) return true;

  let datesToUse: Date[];
  if (typeof dates[0] === "string") {
    datesToUse = dates.map((d) => new Date(d as string));
  } else {
    datesToUse = dates as Date[];
  }

  const lastHoliday = datesToUse.reduce((latest, current) => (current > latest ? current : latest));
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  lastHoliday.setHours(0, 0, 0, 0);

  const daysInTheFuture = (lastHoliday.getTime() - today.getTime()) / (1000 * 60 * 60 * 24);

  return daysInTheFuture < days;
};

export const isAdmin = (): boolean => {
  return useAuthStore().user?.role === "ADMIN";
};
