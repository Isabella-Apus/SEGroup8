import http from './http';

export function getSearchHistoryApi() {
  return http.get('/search/history');
}

export function getHotSearchApi() {
  return http.get('/search/hot');
}
