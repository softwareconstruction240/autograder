export const readableTimestamp = (timestampOrString: Date | string) => {
  const timestamp = typeof timestampOrString === "string" ? new Date(timestampOrString) : timestampOrString;
  return timestamp.toLocaleString();
}
