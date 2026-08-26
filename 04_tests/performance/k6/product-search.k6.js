import { check, sleep } from "k6";
import http from "k6/http";
import { apiBase, defaultOptions, observeResponse, parseBody, query } from "./common.js";

export const options = defaultOptions();

export default function () {
  const keyword = __ENV.KEYWORD || "Demo";
  const pageSize = Number(__ENV.PAGE_SIZE || 20);
  const requests = [
    {
      label: "new product search",
      url: `${apiBase()}/product/list?${query({ pageNum: 1, pageSize, keyword })}`,
    },
    {
      label: "secondhand search",
      url: `${apiBase()}/secondhand/list?${query({ pageNum: 1, pageSize, keyword })}`,
    },
  ];

  for (const request of requests) {
    const response = http.get(request.url, { tags: { endpoint: request.label } });
    const body = parseBody(response, request.label);
    const accepted = observeResponse(response, body, [0]);
    check(response, {
      [`${request.label}: HTTP 200`]: (value) => value.status === 200,
      [`${request.label}: business success`]: () => accepted,
      [`${request.label}: records returned`]: () =>
        body.data && Array.isArray(body.data.records),
    });
  }

  sleep(Number(__ENV.SLEEP || 0.2));
}
