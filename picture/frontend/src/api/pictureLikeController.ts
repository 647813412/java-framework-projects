// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** getLikeCount GET /api/like/count */
export async function getLikeCountUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getLikeCountUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseInt_>("/api/like/count", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** likePicture POST /api/like/do */
export async function likePictureUsingPost(
  body: API.LikeRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLikeVO_>("/api/like/do", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** getLikedPictureList POST /api/like/list */
export async function getLikedPictureListUsingPost(
  body: API.PictureQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePagePictureVO_>("/api/like/list", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** getLikeStatus GET /api/like/status */
export async function getLikeStatusUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getLikeStatusUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>("/api/like/status", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}
