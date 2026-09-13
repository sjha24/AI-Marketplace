package com.aimarketplace.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "app.marketplace")
public record MarketplaceProperties(BigDecimal platformFeePercent) {
}
