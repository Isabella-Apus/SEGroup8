package com.segroup8.secondhand.api;

public record SellerPublicView(long userId, String nickname, String avatar, String region,
        SellerRatingView rating) {
    public record SellerRatingView(int score, String level, long completedTrades) {
    }
}
