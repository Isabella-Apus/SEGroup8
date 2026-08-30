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
