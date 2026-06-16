// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** getHistory GET /api/search/history */
export async function getHistoryUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getHistoryUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListSearchHistory_>("/api/search/history", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** clearHistory POST /api/search/history/clear */
export async function clearHistoryUsingPost(options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean_>("/api/search/history/clear", {
    method: "POST",
    ...(options || {}),
  });
}

/** deleteHistory POST /api/search/history/delete */
export async function deleteHistoryUsingPost(
  body: API.DeleteHistoryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>("/api/search/history/delete", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** getHotKeywords GET /api/search/hot */
export async function getHotKeywordsUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getHotKeywordsUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListString_>("/api/search/hot", {
    method: "GET",
    params: {
      // type has a default value: picture
      type: "picture",
      ...params,
    },
    ...(options || {}),
  });
}
