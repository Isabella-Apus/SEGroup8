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
            '/api': {
                target: 'http://localhost:8080',
                changeOrigin: true
            },
            '/uploads': {
                target: 'http://localhost:8080',
                changeOrigin: true
            },
            '/ws': {
                target: 'ws://localhost:8080',
                changeOrigin: true,
                ws: true
            }
        }
    },
    build: {
        chunkSizeWarningLimit: 1300,
        rollupOptions: {
            output: {
                manualChunks: {
                    vue: ['vue', 'vue-router', 'pinia'],
                    element: ['element-plus'],
                    charts: ['echarts'],
                    search: ['fuse.js']
                }
            }
        }
    }
});
