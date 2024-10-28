import { useAuthStore } from '@/stores/auth'
import { ServerError } from '@/network/ServerErrors'

export const ServerCommunicator = {
  getRequest: getRequest,
  postRequest: postRequest,
  patchRequest: patchRequest,
  deleteRequest: deleteRequest
}

function generateBaseUrl() {
  let baseUrl = window.location.origin
  if (baseUrl.startsWith("http://localhost")) {
    baseUrl = "http://localhost:8788"
  }
  return baseUrl
}

async function doRequest<T>(method: string, endpoint: string, bodyObject?: Object): Promise<T> {
  const authToken: string = useAuthStore().token != null ? useAuthStore().token : ""

  const response = await fetch(generateBaseUrl() + endpoint, {
    method: method,
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': authToken
    },
    body: bodyObject ? JSON.stringify(bodyObject) : null
  });

  if (!response.ok) {
    throw new ServerError(endpoint, await response.text(), response.status, response.statusText)
  }

  return await response.json() as T;
}

async function getRequest<T>(endpoint: string): Promise<T> {
  return await doRequest<T>("GET", endpoint)
}

async function postRequest<T>(endpoint: string, bodyObject?: Object): Promise<T> {
  return await doRequest<T>("POST", endpoint, bodyObject)
}

async function patchRequest<T>(endpoint: string, bodyObject?: Object): Promise<T> {
  return await doRequest<T>("PATCH", endpoint, bodyObject)
}

async function deleteRequest<T>(endpoint: string, bodyObject?: Object): Promise<T> {
  return await doRequest<T>("DELETE", endpoint, bodyObject)
}