export class ServerError extends Error {
  readonly endpoint: string;
  readonly status: number;
  readonly statusText: string;
  constructor(endpoint: string, message: string, status: number, statusText: string) {
    super(message);
    this.name = "ServerError";
    this.endpoint = endpoint;
    this.status = status;
    this.statusText = statusText;
  }

  isBadRequest(): boolean {
    return this.status === 400;
  }
  isUnauthorized(): boolean {
    return this.status === 401;
  }
  isForbidden(): boolean {
    return this.status === 403;
  }
  isNotFound(): boolean {
    return this.status === 404;
  }
  isHonorCodeViolation(): boolean {
    return this.status === 418;
  }
  isUnprocessableEntity(): boolean {
    return this.status === 422;
  }
  isInternalServerError(): boolean {
    return this.status === 500;
  }
}
