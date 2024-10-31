import { useAuthStore } from '@/stores/auth'
import { ServerError } from '@/network/ServerError'
import { useAppConfigStore } from '@/stores/appConfig'

export const ServerCommunicator = {
  getRequest: getRequest,
  postRequest: postRequest,
  patchRequest: patchRequest,
  doUnprocessedRequest: doUnprocessedRequest
}

// GET request overloads
function getRequest<T>(endpoint: string, expectResponse: false): Promise<null>;
function getRequest<T>(endpoint: string, expectResponse?: true): Promise<T>;
async function getRequest<T>(
  endpoint: string,
  expectResponse: boolean = true
): Promise<T | null> {
  if (expectResponse) {
    return await doRequest<T>("GET", endpoint, null, true);
  }
  return await doRequest<T>("GET", endpoint, null, false);
}

// POST request overloads
function postRequest<T>(endpoint: string, bodyObject: Object | null, expectResponse: false): Promise<null>;
function postRequest<T>(endpoint: string, bodyObject?: Object | null, expectResponse?: true): Promise<T>;
async function postRequest<T>(
  endpoint: string,
  bodyObject: Object | null = null,
  expectResponse: boolean = true
): Promise<T | null> {
  if (expectResponse) {
    return await doRequest<T>("POST", endpoint, bodyObject, true);
  }
  return await doRequest<T>("POST", endpoint, bodyObject, false);

}

// PATCH request overloads
function patchRequest<T>(endpoint: string, bodyObject: Object | null, expectResponse: false): Promise<null>;
function patchRequest<T>(endpoint: string, bodyObject?: Object | null, expectResponse?: true): Promise<T>;
async function patchRequest<T>(
  endpoint: string,
  bodyObject: Object | null = null,
  expectResponse: boolean = true
): Promise<T | null> {
  if (expectResponse) {
    return doRequest<T>("PATCH", endpoint, bodyObject, true);
  }
  return doRequest<T>("PATCH", endpoint, bodyObject, false);
}

// doRequest overloads
function doRequest<T>(
  method: string,
  endpoint: string,
  bodyObject: Object | null,
  expectResponse: false
): Promise<null>;
function doRequest<T>(
  method: string,
  endpoint: string,
  bodyObject?: Object | null,
  expectResponse?: true
): Promise<T>;
async function doRequest<T>(
  method: string,
  endpoint: string,
  bodyObject: Object | null = null,
  expectResponse: boolean = true
): Promise<T | null> {
  const response = await doUnprocessedRequest(method, endpoint, bodyObject);

  if (!expectResponse) {
    return null;
  }

  const text = await response.text()
  if (text) {
    return JSON.parse(text) as T
  }

  if (bodyObject) {
    console.error("Body request:", bodyObject)
  }
  console.error("Response: ", response)
  throw new Error(`Expected a response from ${method} call to ${endpoint} but got none`)
}

async function doUnprocessedRequest(
  method: string,
  endpoint: string,
  bodyObject: Object | null = null
): Promise<Response> {
  const authToken = useAuthStore().token ?? ""

  const response = await fetch(useAppConfigStore().backendUrl + endpoint, {
    method: method,
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': authToken
    },
    body: bodyObject ? JSON.stringify(bodyObject) : null
  });

  if (!response.ok) {
    console.error(`A ${response.status} error occurred while making a ${method} request to ${endpoint}`)
    console.error(response)
    throw new ServerError(endpoint, await response.text(), response.status, response.statusText)
  }
  return response
}