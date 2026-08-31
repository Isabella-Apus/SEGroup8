import { test, expect } from "../fixtures";
import type { APIRequestContext, Page, Response } from "@playwright/test";
import { assertNoVisibleError } from "../helpers/http";
import {
    bearer,
    captureDomainEEvidence,
    domainEToken,
    expectBusinessFailure,
    expectBusinessSuccess,
    loginAsDomainE,
    uniqueName,
} from "../helpers/domain-e";

const responseTimeout = 30_000;
const sellerUserId = 2;
const buyerUserId = 3;
const productId = 1;

type Conversation = {
    id: number;
    other?: { userId?: number; nickname?: string };
};

type ChatMessage = {
    id: number;
    conversationId: number;
    senderUserId: number;
    receiverUserId: number;
    content: string;
};

type Notification = {
    title: string;
    content: string;
    targetPath?: string;
};

test.describe("@DOMAIN_E @UC24 chat authorization and delivery", () => {
    test.describe.configure({ timeout: 120_000 });

    test("buyer and seller exchange persisted messages while outsider and blocks are isolated", async ({
        page,
        request,
    }, testInfo) => {
        const buyerToken = await domainEToken(request, "BUYER");
        const sellerToken = await domainEToken(request, "OFFICIAL_SELLER");
        const outsiderToken = await domainEToken(request, "THIRD_PARTY");
        await ensureUnblocked(request, buyerToken, sellerUserId);
        await ensureUnblocked(request, sellerToken, buyerUserId);

        const buyerMessage = uniqueName("UC24 buyer message");
        const sellerReply = uniqueName("UC24 seller reply");

        await loginAsDomainE(page, "BUYER");
        const createResponse = page.waitForResponse(
            isApiResponse("/api/chat/conversations", "POST"),
            { timeout: responseTimeout },
        );
        await page.goto(`/messages?participantId=${sellerUserId}&sourceType=PRODUCT&sourceId=${productId}`);
        const conversation = await expectBusinessSuccess<Conversation>(await createResponse);
        const conversationId = Number(conversation.id);
        expect(conversationId).toBeGreaterThan(0);
        await expect(page.getByRole("heading", { name: "站内消息" })).toBeVisible();
        await expect(page.getByRole("heading", { name: conversation.other?.nickname || "" }))
            .toBeVisible();

        const repeated = await expectBusinessSuccess<Conversation>(await request.post(
            "/api/chat/conversations",
            {
                headers: bearer(sellerToken),
                data: {
                    targetUserId: buyerUserId,
                    sourceType: "PRODUCT",
                    sourceId: productId,
                },
            },
        ));
        expect(Number(repeated.id)).toBe(conversationId);

        await sendThroughUi(page, buyerMessage);
        await expect(page.locator(".message-content", { hasText: buyerMessage })).toBeVisible();

        const sellerNotifications = await notifications(request, sellerToken);
        expect(sellerNotifications.some((notification) =>
            notification.title === "新消息"
            && notification.targetPath === `/merchant/messages?conversationId=${conversationId}`,
        )).toBeTruthy();

        await loginAsDomainE(page, "OFFICIAL_SELLER");
        await page.goto(`/merchant/messages?conversationId=${conversationId}`);
        await expect(page.getByRole("heading", { name: repeated.other?.nickname || "" }))
            .toBeVisible();
        await expect(page.locator(".message-content", { hasText: buyerMessage })).toBeVisible();
        await sendThroughUi(page, sellerReply);
        await expect(page.locator(".message-content", { hasText: sellerReply })).toBeVisible();

        await loginAsDomainE(page, "BUYER");
        await page.goto(`/messages?conversationId=${conversationId}`);
        await expect(page.locator(".message-content", { hasText: buyerMessage })).toBeVisible();
        await expect(page.locator(".message-content", { hasText: sellerReply })).toBeVisible();
        await page.reload();
        await expect(page.locator(".message-content", { hasText: buyerMessage })).toBeVisible();
        await expect(page.locator(".message-content", { hasText: sellerReply })).toBeVisible();

        const outsiderConversations = await conversations(request, outsiderToken);
        expect(outsiderConversations.some((item) => Number(item.id) === conversationId)).toBeFalsy();
        const outsiderRead = await request.get(
            `/api/chat/conversations/${conversationId}/messages`,
            { headers: bearer(outsiderToken) },
        );
        const outsiderReadFailure = await expectBusinessFailure(outsiderRead);
        expect(Number(outsiderReadFailure?.code)).toBe(403);
        const outsiderSend = await request.post(
            `/api/chat/conversations/${conversationId}/messages`,
            { headers: bearer(outsiderToken), data: { content: "outsider message" } },
        );
        const outsiderSendFailure = await expectBusinessFailure(outsiderSend);
        expect(Number(outsiderSendFailure?.code)).toBe(403);

        const historyBeforeBlock = await messages(request, buyerToken, conversationId);
        const blockResponse = page.waitForResponse(
            isApiResponse("/api/report-block/block", "POST"),
            { timeout: responseTimeout },
        );
        await page.getByRole("button", { name: "拉黑", exact: true }).click();
        const blockDialog = page.getByRole("dialog", { name: "拉黑用户" });
        await blockDialog.getByRole("button", { name: "确认拉黑", exact: true }).click();
        await expectBusinessSuccess(await blockResponse);
        await expect(page.getByRole("button", { name: "已拉黑", exact: true })).toBeDisabled();

        const blockedMessage = uniqueName("UC24 blocked message");
        const blockedSend = await request.post(
            `/api/chat/conversations/${conversationId}/messages`,
            { headers: bearer(sellerToken), data: { content: blockedMessage } },
        );
        const blockedSendFailure = await expectBusinessFailure(blockedSend);
        expect(Number(blockedSendFailure?.code)).toBe(403);
        const historyAfterBlock = await messages(request, buyerToken, conversationId);
        expect(historyAfterBlock).toHaveLength(historyBeforeBlock.length);
        expect(historyAfterBlock.some((message) => message.content === blockedMessage)).toBeFalsy();

        await assertNoVisibleError(page);
        await captureDomainEEvidence(page, testInfo, "UC24", "uc24-chat-history-and-block");
        await ensureUnblocked(request, buyerToken, sellerUserId);
    });
});

async function sendThroughUi(page: Page, content: string): Promise<void> {
    const response = page.waitForResponse(
        (candidate) => candidate.url().includes("/api/chat/conversations/")
            && candidate.url().endsWith("/messages")
            && candidate.request().method() === "POST",
        { timeout: responseTimeout },
    );
    await page.getByPlaceholder("输入消息").fill(content);
    await page.getByRole("button", { name: "发送", exact: true }).click();
    const message = await expectBusinessSuccess<ChatMessage>(await response);
    expect(message.content).toBe(content);
}

function isApiResponse(path: string, method: string) {
    return (response: Response) => response.url().includes(path)
        && response.request().method() === method;
}

async function conversations(request: APIRequestContext, token: string): Promise<Conversation[]> {
    return expectBusinessSuccess<Conversation[]>(await request.get("/api/chat/conversations", {
        headers: bearer(token),
    }));
}

async function messages(
    request: APIRequestContext,
    token: string,
    conversationId: number,
): Promise<ChatMessage[]> {
    return expectBusinessSuccess<ChatMessage[]>(await request.get(
        `/api/chat/conversations/${conversationId}/messages`,
        { headers: bearer(token) },
    ));
}

async function notifications(request: APIRequestContext, token: string): Promise<Notification[]> {
    return expectBusinessSuccess<Notification[]>(await request.get("/api/notifications", {
        headers: bearer(token),
    }));
}

async function ensureUnblocked(
    request: APIRequestContext,
    token: string,
    targetUserId: number,
): Promise<void> {
    const check = await expectBusinessSuccess<boolean>(await request.get(
        `/api/report-block/block/check/${targetUserId}`,
        { headers: bearer(token) },
    ));
    if (!check) {
        return;
    }
    await expectBusinessSuccess(await request.delete(`/api/report-block/block/${targetUserId}`, {
        headers: bearer(token),
    }));
}
