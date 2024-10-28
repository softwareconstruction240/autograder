export class ServerError extends Error {
  endpoint: string
  status: number
  constructor(
    endpoint: string,
    message: string,
    status: number,
    statusText: string,
  ) {
    super(message);
    this.name = statusText + " Error"

    this.endpoint = endpoint
    this.status = status

    // this ensures instanceof checks work
    Object.setPrototypeOf(this, ServerError.prototype);

    // new.target tells us if this constructor was called directly
    // or through a child class constructor
    if (new.target === ServerError) {
      switch (status) {
        case 400:
          return new ServerBadRequestError(endpoint, message)
        case 401:
          return new ServerUnauthorizedError(endpoint, message)
        case 403:
          return new ServerForbiddenError(endpoint, message)
        case 404:
          return new ServerNotFoundError(endpoint, message)
        case 418:
          return new ServerHonorCodeViolationError(endpoint, message)
        case 422:
          return new ServerUnprocessableEntityError(endpoint, message)
        case 500:
          return new InternalServerError(endpoint, message)
      }
    }
  }
}


class ServerBadRequestError extends ServerError {
  constructor(endpoint: string, message: string) {
    super(endpoint, message, 400, "Bad Request")
  }
}

class ServerUnauthorizedError extends ServerError {
  constructor(endpoint: string, message: string) {
    super(endpoint, message, 401, "Unauthorized")
    Object.setPrototypeOf(this, ServerBadRequestError.prototype);
  }
}

class ServerForbiddenError extends ServerError {
  constructor(endpoint: string, message: string) {
    super(endpoint, message, 403, "Forbidden")
    Object.setPrototypeOf(this, ServerBadRequestError.prototype);
  }
}

class ServerNotFoundError extends ServerError {
  constructor(endpoint: string, message: string) {
    super(endpoint, message, 404, "Not Found")
    Object.setPrototypeOf(this, ServerBadRequestError.prototype);
  }
}

class ServerHonorCodeViolationError extends ServerError {
  constructor(endpoint: string, message: string) {
    super(endpoint, message, 418, "Honor Code Violation")
    Object.setPrototypeOf(this, ServerBadRequestError.prototype);
  }
}

class ServerUnprocessableEntityError extends ServerError {
  constructor(endpoint: string, message: string) {
    super(endpoint, message, 422, "Unprocessable Entity")
    Object.setPrototypeOf(this, ServerBadRequestError.prototype);
  }
}

class InternalServerError extends ServerError {
  constructor(endpoint: string, message: string) {
    super(endpoint, message, 500, "Internal Server Error")
    Object.setPrototypeOf(this, ServerBadRequestError.prototype);
  }
}