import http from "@/api/http";

export function listChatConversationsApi() {
    return http.get("/chat/conversations");
}

export function createChatConversationApi(payload) {
    return http.post("/chat/conversations", payload);
}

export function listChatMessagesApi(conversationId) {
    return http.get(`/chat/conversations/${conversationId}/messages`);
}

export function sendChatMessageApi(conversationId, payload) {
    return http.post(`/chat/conversations/${conversationId}/messages`, payload);
}
