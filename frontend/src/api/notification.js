import http from "@/api/http";

export function listNotificationsApi() {
  return http.get("/notifications");
}

export function markNotificationReadApi(notificationId) {
  return http.post(`/notifications/${notificationId}/read`);
}

export function markAllNotificationsReadApi() {
  return http.post("/notifications/read-all");
}
