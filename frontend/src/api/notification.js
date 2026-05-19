import http from "@/api/http";

export function listNotificationsApi(scope) {
  return http.get("/notifications", {
    params: scope ? { scope } : {},
  });
}

export function markNotificationReadApi(notificationId) {
  return http.post(`/notifications/${notificationId}/read`);
}

export function markAllNotificationsReadApi(scope) {
  return http.post("/notifications/read-all", {}, {
    params: scope ? { scope } : {},
  });
}
