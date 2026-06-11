import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const gatewayTarget = env.VITE_API_PROXY_TARGET || 'http://localhost:8180';

  return {
    plugins: [react()],
    server: {
      port: 5173,
      proxy: {
        '/api': {
          target: gatewayTarget,
          changeOrigin: true,
        },
      },
    },
  };
});
