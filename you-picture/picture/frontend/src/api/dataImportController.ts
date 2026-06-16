// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** importPictureData POST /api/picture/data/import */
export async function importPictureDataUsingPost(options?: {
  [key: string]: any;
}) {
  return request<API.BaseResponseInt_>("/api/picture/data/import", {
    method: "POST",
    ...(options || {}),
  });
}
