import http from "k6/http";
import { check, sleep } from "k6";
import { apiBase, defaultOptions, parseBody, query } from "./common.js";

export const options = defaultOptions();

export default function () {
  const keyword = __ENV.KEYWORD || "耳机";
  const pageSize = Number(__ENV.PAGE_SIZE || 20);

  const productUrl = `${apiBase()}/product/list?${query({
    pageNum: 1,
    pageSize,
    keyword,
  })}`;
  const secondhandUrl = `${apiBase()}/secondhand/list?${query({
    pageNum: 1,
    pageSize,
    keyword,
    status: "ON_SALE",
  })}`;

  const productRes = http.get(productUrl);
  const productBody = parseBody(productRes, "product search");
  check(productRes, {
    "product search http 200": (r) => r.status === 200,
    "product search business ok": () => productBody.code === 0,
    "product search has page data": () => productBody.data && Array.isArray(productBody.data.records),
  });

  const secondhandRes = http.get(secondhandUrl);
  const secondhandBody = parseBody(secondhandRes, "secondhand search");
  check(secondhandRes, {
    "secondhand search http 200": (r) => r.status === 200,
    "secondhand search business ok": () => secondhandBody.code === 0,
    "secondhand search has page data": () => secondhandBody.data && Array.isArray(secondhandBody.data.records),
  });

  sleep(Number(__ENV.SLEEP || 1));
}
