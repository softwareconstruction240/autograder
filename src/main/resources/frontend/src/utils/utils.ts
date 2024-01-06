export const readableTimestamp = (timestamp: Date) => {
    return `
  ${timestamp.getMonth() + 1}/${timestamp.getDate()}/${timestamp.getFullYear()} ${timestamp.getHours()}:${timestamp.getMinutes()}
  `;
}