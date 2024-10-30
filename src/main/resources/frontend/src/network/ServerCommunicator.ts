import { useAuthStore } from '@/stores/auth'
import { ServerError } from '@/network/ServerError'
import { useAppConfigStore } from '@/stores/appConfig'

export const ServerCommunicator = {
  getRequest: getRequest,
  postRequest: postRequest,
  patchRequest: patchRequest,
  doUnprocessedRequest: doUnprocessedRequest
}

async function getRequest<T>(endpoint: string, expectResponse?: boolean): Promise<T> {
  return await doRequest<T>("GET", endpoint, null,expectResponse ?? true)
}

async function postRequest<T>(endpoint: string, bodyObject?: Object, expectResponse?: boolean): Promise<T> {
  return await doRequest<T>("POST", endpoint, bodyObject,expectResponse ?? true)
}

async function patchRequest<T>(endpoint: string, bodyObject?: Object, expectResponse?: boolean): Promise<T> {
  return await doRequest<T>("PATCH", endpoint, bodyObject,expectResponse ?? true)
}

async function doRequest<T>(method: string,
                            endpoint: string,
                            bodyObject?: Object | null,
                            expectResponse?: boolean): Promise<T> {
  const response = await doUnprocessedRequest(method, endpoint, bodyObject)

  if (!expectResponse) {
    return null as T
  }

  // this makes sure there is actually something in the body before trying to parse
  const text = await response.text()
  if (text) {
    return JSON.parse(text) as T
  }

  // code only reaches here if the caller was expecting a response (T isn't void)
  // and the response from the server is empty
  if (bodyObject) {
    console.error("Body request:", bodyObject)
  }
  console.error("Response: ", response)
  throw new Error(`Expected a response from ${method} call to ${endpoint} but got none`)
}

async function doUnprocessedRequest(method: string,
                                    endpoint: string,
                                    bodyObject?: Object | null): Promise<Response> {
  const authToken: string = useAuthStore().token != null ? useAuthStore().token : ""

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