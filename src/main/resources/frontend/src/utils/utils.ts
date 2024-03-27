import {useAdminStore} from "@/stores/admin";
import type { Submission } from '@/types/types'
import { useAuthStore } from '@/stores/auth'

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
  repoLink = repoLink.substring(0, repoLink.indexOf(".git"))
  const link = repoLink
    + (repoLink.charAt(repoLink.length-1) == '/' ? "" : "/")
    + "tree/"
    + hash;
  return '<a href="' + link + '" target="_blank">' + hash.substring(0,6) + '</a>'
}