import {useAdminStore} from "@/stores/admin";
import type {Phase, RubricItem, RubricType, Submission, TestNode} from '@/types/types'
import { VerifiedStatus } from '@/types/types'
import { useAuthStore } from '@/stores/auth'

export const commitVerificationFailed = (submission: Submission) => {
  if (submission.admin) return false; // Admin submissions don't have commit requirements
  if (!submission.verifiedStatus) { // old submissions lack this info
    console.error("submission from " + submission.netId + " has no verification info")
    return false;
  }
  return submission.verifiedStatus.toString() === VerifiedStatus[VerifiedStatus.Unapproved];
}

export const readableTimestamp = (timestampOrString: Date | string) => {
  const timestamp = typeof timestampOrString === "string" ? new Date(timestampOrString) : timestampOrString;
  return timestamp.toLocaleString();
}

export const simpleTimestamp = (date: Date | string) => {
  const months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"]
  const time = typeof date === "string" ? new Date(date) : date;
  return months[time.getMonth()] + " " + time.getDate() + " " + time.getHours() + ":" + String(time.getMinutes()).padStart(2, '0')
}

export const nameFromNetId = (netId: string) => {
  const user = useAdminStore().usersByNetId[netId];
  if (user == null) {
    console.error("Error getting name from netId, either because user with netId " + netId + " doesn't exist, or you're not logged in as an admin")
    return
  }
  return `${user.firstName} ${user.lastName}`
}

/**
 * returns the first and last name of the person who sent in the submission.
 * IMPORTANT NOTE: If this is called from a student user, it will simply return the student's name,
 * without regard to the actual netId on the submission
 * @param submission
 */
export const nameOnSubmission = (submission: Submission) => {
  const user = useAuthStore().user
  if (!user) {
    console.error("Asking for name on submission while logged out")
    return "?"
  }
  else if (user.role == 'ADMIN') {
    return nameFromNetId(submission.netId)
  } else {
    return user.firstName + " " + user.lastName
  }
}

export const scoreToPercentage = (score:number) => {
  return roundTwoDecimals(score * 100) + "%"
}

export const roundTwoDecimals = (num: number) => {
  return Math.round((num + Number.EPSILON) * 100) / 100;
}

export const generateClickableLink = (link: string) => {
  return '<a href="' + link + '" target="_blank">' + link.split("://")[1] + '</a>'
}

export const generateClickableCommitLink = (repoLink: string, hash: string) => {
  if (repoLink.endsWith(".git")) {
    repoLink = repoLink.substring(0, repoLink.indexOf(".git"))
  }
  const link = repoLink
    + (repoLink.charAt(repoLink.length-1) == '/' ? "" : "/")
    + "tree/"
    + hash;
  return '<a href="' + link + '" target="_blank">' + hash.substring(0,6) + '</a>'
}

export const sanitizeHtml = (str: string) => {
  str = str.replace(/&/g, "&amp;");
  str = str.replace(/</g, "&lt;");
  str = str.replace(/>/g, "&gt;");
  str = str.replace(/"/g, "&quot;");
  return str.replace(/\n/g, '<br />')
}

export const generateResultsHtmlStringFromTestNode = (node: TestNode, indent: string) => {
  let result = indent + node.testName;

  if (node.passed !== undefined) {
    result += node.passed ? ` <span class="success">✓</span>` : ` <span class="failure">✗</span>`;
    if (node.errorMessage !== null && node.errorMessage !== undefined && node.errorMessage !== "") {
      result += `<br/>${indent}   ↳<span class="failure">${sanitizeHtml(node.errorMessage)}</span>`;
    }
  } else {
    result += ` (${node.numTestsPassed} passed, ${node.numTestsFailed} failed)`
  }
  result += "<br/>";

  for (const key in node.children) {
    if (Object.prototype.hasOwnProperty.call(node.children, key)) {
      result += generateResultsHtmlStringFromTestNode(node.children[key], indent + "&nbsp;&nbsp;&nbsp;&nbsp;");
    }
  }

  return result;
}

export const phaseString = (phase: Phase | "Quality") => {
  if (phase == 'Quality') { return "Code Quality Check"; }
  else { return "Phase " + phase.toString().charAt(5)}
}

export const sortedItems = (items: Record<RubricType, RubricItem>): RubricItem[] => {
  return Object.keys(items).sort((a, b) => {
    const aPoints = items[a as RubricType].results.possiblePoints;
    const bPoints = items[b as RubricType].results.possiblePoints;
    return bPoints - aPoints;
  }).map((item) => items[item as RubricType]);
}
