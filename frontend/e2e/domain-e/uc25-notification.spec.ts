import { test, expect } from "../fixtures";
import type { APIRequestContext, WebSocket as PlaywrightWebSocket } from "@playwright/test";
import {
    bearer,
    captureDomainEEvidence,
    domainEToken,
    expectBusinessSuccess,
    loginAsDomainE,
    uniqueName,
} from "../helpers/domain-e";

type NotificationRecord = {
    id: number;
    title: string;
    content: string;
    scope: "buyer" | "seller";
    isRead: number;
};

async function listNotifications(
    request: APIRequestContext,
    token: string,
): Promise<NotificationRecord[]> {
    const response = await request.get("/api/notifications", { headers: bearer(token) });
    return expectBusinessSuccess<NotificationRecord[]>(response);
}

async function sendBuyerMessage(
    request: APIRequestContext,
    sellerToken: string,
    content: string,
): Promise<void> {
    const conversationResponse = await request.post("/api/chat/conversations", {
        headers: bearer(sellerToken),
        data: { targetUserId: 3, sourceType: "DIRECT" },
    });
    const conversation = await expectBusinessSuccess<{ id: number }>(conversationResponse);
    const messageResponse = await request.post(
        `/api/chat/conversations/${conversation.id}/messages`,
        { headers: bearer(sellerToken), data: { content } },
    );
    await expectBusinessSuccess(messageResponse);
}

async function clearMutualBlocks(
    request: APIRequestContext,
    buyerToken: string,
    sellerToken: string,
): Promise<void> {
    await request.delete("/api/report-block/block/2", { headers: bearer(buyerToken) });
    await request.delete("/api/report-block/block/3", { headers: bearer(sellerToken) });
}

async function newlyCreatedNotification(
    request: APIRequestContext,
    buyerToken: string,
    previousIds: Set<number>,
): Promise<NotificationRecord> {
    let created: NotificationRecord | undefined;
    await expect.poll(async () => {
        const current = await listNotifications(request, buyerToken);
        created = current.find((item) => !previousIds.has(Number(item.id)));
        return created?.id || 0;
    }, { timeout: 15_000 }).toBeGreaterThan(0);
    return created!;
}

test.describe("@DOMAIN_E @UC25 notification websocket and reconnect compensation", () => {
    test.describe.configure({ timeout: 120_000 });

    test("buyer receives, reads and recovers missed notifications in real Edge", async ({
        page,
        request,
    }, testInfo) => {
        await page.addInitScript(() => {
            const NativeWebSocket = window.WebSocket;
            class TrackedWebSocket extends NativeWebSocket {
                constructor(url: string | URL, protocols?: string | string[]) {
                    if (protocols === undefined) {
                        super(url);
                    } else {
                        super(url, protocols);
                    }
                    (window as any).__uc25ActiveWebSocket = this;
                }
            }
            window.WebSocket = TrackedWebSocket as typeof WebSocket;
        });
        const realtimeSockets: PlaywrightWebSocket[] = [];
        page.on("websocket", (socket) => {
            if (socket.url().includes("/ws/realtime")) {
                realtimeSockets.push(socket);
            }
        });

        await loginAsDomainE(page, "BUYER");
        await expect.poll(() => realtimeSockets.length, { timeout: 15_000 }).toBeGreaterThan(0);

        const buyerToken = await domainEToken(request, "BUYER");
        const sellerToken = await domainEToken(request, "OFFICIAL_SELLER");
        await clearMutualBlocks(request, buyerToken, sellerToken);

        const notificationResponse = page.waitForResponse(
            (response) => response.url().includes("/api/notifications")
                && response.request().method() === "GET",
        );
        await page.goto("/notifications");
        await expectBusinessSuccess(await notificationResponse);
        await expect(page.getByRole("heading", { name: "通知" })).toBeVisible();

        const initial = await listNotifications(request, buyerToken);
        const initialIds = new Set(initial.map((item) => Number(item.id)));
        await sendBuyerMessage(request, sellerToken, uniqueName("UC25-realtime"));
        const pushed = await newlyCreatedNotification(request, buyerToken, initialIds);

        const pushedCard = page.locator(`[data-notification-id="${pushed.id}"]`);
        await expect(pushedCard, "notification must arrive through the live WebSocket without reload")
            .toBeVisible({ timeout: 15_000 });
        await expect(pushedCard.getByText("未读", { exact: true })).toBeVisible();

        const sellerNotifications = await listNotifications(request, sellerToken);
        expect(sellerNotifications.some((item) => Number(item.id) === Number(pushed.id))).toBeFalsy();

        await pushedCard.getByRole("button", { name: "标为已读", exact: true }).click();
        await expect(pushedCard.getByText("未读", { exact: true })).toHaveCount(0);
        await page.reload();
        await expect(page.locator(`[data-notification-id="${pushed.id}"]`).getByText("未读", { exact: true }))
            .toHaveCount(0);

        const beforeDisconnect = await listNotifications(request, buyerToken);
        const beforeDisconnectIds = new Set(beforeDisconnect.map((item) => Number(item.id)));
        const socketCountBeforeDisconnect = realtimeSockets.length;
        await page.context().setOffline(true);
        await page.evaluate(() => {
            const activeSocket = (window as any).__uc25ActiveWebSocket as WebSocket | undefined;
            if (activeSocket && activeSocket.readyState < WebSocket.CLOSING) {
                activeSocket.close(4000, "UC25 E2E disconnect");
            }
        });
        await expect.poll(() => realtimeSockets.at(-1)?.isClosed(), { timeout: 10_000 })
            .toBeTruthy();
        await sendBuyerMessage(request, sellerToken, uniqueName("UC25-missed"));
        const missed = await newlyCreatedNotification(request, buyerToken, beforeDisconnectIds);

        await page.context().setOffline(false);
        await expect.poll(
            () => realtimeSockets.slice(socketCountBeforeDisconnect).some((socket) => !socket.isClosed()),
            { timeout: 20_000 },
        ).toBeTruthy();
        const missedCard = page.locator(`[data-notification-id="${missed.id}"]`);
        await expect(
            missedCard,
            "reconnect must refresh the list and compensate for notifications missed while offline",
        ).toBeVisible({ timeout: 20_000 });

        await missedCard.getByRole("button", { name: "标为已读", exact: true }).click();
        await page.reload();
        const persistedCard = page.locator(`[data-notification-id="${missed.id}"]`);
        await expect(persistedCard).toBeVisible();
        await expect(persistedCard.getByText("未读", { exact: true })).toHaveCount(0);
        await captureDomainEEvidence(
            page,
            testInfo,
            "UC25",
            "uc25-realtime-reconnect-and-read",
        );
    });
});
