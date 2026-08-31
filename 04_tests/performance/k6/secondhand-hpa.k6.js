import { check } from "k6";
import http from "k6/http";
import { apiBase, observeResponse, parseBody, query } from "./common.js";

export const options = {
  vus: Number(__ENV.VUS || 10),
  duration: __ENV.DURATION || "150s",
  thresholds: {
    http_req_failed: ["rate<0.05"],
    server_error: ["rate==0"],
  },
};

export default function () {
  const pageSize = Number(__ENV.PAGE_SIZE || 50);
  const url = `${apiBase()}/secondhand/list?${query({ pageNum: 1, pageSize })}`;
  const batchSize = Math.max(1, Number(__ENV.BATCH_SIZE || 1));
  const requests = Array.from({ length: batchSize }, () => [
    "GET",
    url,
    null,
    { tags: { endpoint: "secondhand HPA list" } },
  ]);
  const responses = http.batch(requests);

  for (const response of responses) {
    const body = parseBody(response, "secondhand HPA list");
    const accepted = observeResponse(response, body, [0]);
    check(response, {
      "secondhand HPA list: HTTP 200": (value) => value.status === 200,
      "secondhand HPA list: business success": () => accepted,
      "secondhand HPA list: records returned": () =>
        body.data && Array.isArray(body.data.records),
    });
  }
}
