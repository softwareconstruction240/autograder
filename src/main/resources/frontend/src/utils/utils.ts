import {useAdminStore} from "@/stores/admin";

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
  return `${user.firstName} ${user.lastName}`
}

export const scoreToPercentage = (score:number) => {
  return roundTwoDecimals(score * 100) + "%"
}

const roundTwoDecimals = (num: number) => {
  return Math.round((num + Number.EPSILON) * 100) / 100;
}

export const generateClickableLink = (link: string) => {
  return '<a id="repo-link" style="color: darkblue; font-style: italic;" href="' + link + '" target="_blank">' + link + '</a>'
}