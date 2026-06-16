// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** addComment POST /api/comment/add */
export async function addCommentUsingPost(
  body: API.AddCommentRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseCommentVO_>("/api/comment/add", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** deleteComment POST /api/comment/delete */
export async function deleteCommentUsingPost(
  body: API.DeleteCommentRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>("/api/comment/delete", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** listComments GET /api/comment/list */
export async function listCommentsUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.listCommentsUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageCommentVO_>("/api/comment/list", {
    method: "GET",
    params: {
      // current has a default value: 1
      current: "1",
      // pageSize has a default value: 10
      pageSize: "10",
      ...params,
    },
    ...(options || {}),
  });
}

/** listReplies GET /api/comment/reply/list */
export async function listRepliesUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.listRepliesUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageCommentVO_>("/api/comment/reply/list", {
    method: "GET",
    params: {
      // current has a default value: 1
      current: "1",
      // pageSize has a default value: 20
      pageSize: "20",
      ...params,
    },
    ...(options || {}),
  });
}
