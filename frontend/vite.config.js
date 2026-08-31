import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import { fileURLToPath, URL } from 'node:url';

export default defineConfig({
    plugins: [vue()],
    resolve: {
        alias: {
            '@': fileURLToPath(new URL('./src', import.meta.url))
        }
    },
    server: {
        port: 5174,
        host: '0.0.0.0',
        proxy: {
            ...Object.fromEntries([
                '/api/auth', '/api/report-block', '/api/credit',
                '/api/admin/merchant-applications', '/api/admin/users',
                '/api/admin/audit-logs', '/api/admin/reports'
            ].map((path) => [path, {
                target: process.env.VITE_IDENTITY_TARGET || 'http://localhost:8091',
                changeOrigin: true
            }])),
            '/api/order': { target: 'http://localhost:8085', changeOrigin: true },
            '/api/review': { target: 'http://localhost:8085', changeOrigin: true },
            '/api/logistics': { target: 'http://localhost:8085', changeOrigin: true },
            '/api/admin/orders': { target: 'http://localhost:8085', changeOrigin: true },
            ...Object.fromEntries([
                '/api/category', '/api/product', '/api/shop',
                '/api/admin/product-risk-audits', '/api/user/browse-history', '/api/search'
            ].map((path) => [path, {
                target: process.env.VITE_CATALOG_SHOP_TARGET || 'http://localhost:8086',
                changeOrigin: true
            }])),
            // Keep the broader identity prefix after catalog's
            // /api/user/browse-history route so the longest owner wins.
            '/api/user': {
                target: process.env.VITE_IDENTITY_TARGET || 'http://localhost:8091',
                changeOrigin: true
            },
            '/api/secondhand': {
                target: process.env.VITE_SECONDHAND_TARGET || 'http://localhost:18080',
                changeOrigin: true
            },
            '/api/finance': {
                target: process.env.VITE_FINANCE_TARGET || 'http://localhost:18085',
                changeOrigin: true
            },
            '/api/voucher': {
                target: process.env.VITE_FINANCE_TARGET || 'http://localhost:18085',
                changeOrigin: true
            },
            '/api/chat': {
                target: 'http://localhost:8084',
                changeOrigin: true
            },
            '/api/notifications': {
                target: 'http://localhost:8084',
                changeOrigin: true
            },
            '/api': {
                target: 'http://localhost:8080',
                changeOrigin: true
            },
            '/uploads': {
                target: 'http://localhost:8080',
                changeOrigin: true
            },
            '/ws': {
                target: 'ws://localhost:8084',
                changeOrigin: true,
                ws: true
            }
        }
    }
});
