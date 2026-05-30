/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        surface: {
          50: '#f8fafc',
          100: '#f1f5f9',
          200: '#e2e8f0',
          300: '#cbd5e1',
          400: '#94a3b8',
          500: '#64748b',
          600: '#475569',
          700: '#334155',
          800: '#1e293b',
          900: '#0f172a',
          950: '#0a0f1a',
        },
        brand: {
          50: '#eef2ff',
          100: '#e0e7ff',
          200: '#c7d2fe',
          300: '#a5b4fc',
          400: '#818cf8',
          500: '#6366f1',
          600: '#4f46e5',
          700: '#4338ca',
          800: '#3730a3',
          900: '#312e81',
        },
        accent: {
          cyan: '#06b6d4',
          violet: '#8b5cf6',
          pink: '#ec4899',
          amber: '#f59e0b',
          emerald: '#10b981',
        },
      },
      backgroundImage: {
        'gradient-radial': 'radial-gradient(var(--tw-gradient-stops))',
        'gradient-conic': 'conic-gradient(from 180deg at 50% 50%, var(--tw-gradient-stops))',
        'gradient-aurora': 'linear-gradient(135deg, #6366f1 0%, #8b5cf6 25%, #ec4899 50%, #f59e0b 75%, #06b6d4 100%)',
        'gradient-brand': 'linear-gradient(135deg, #6366f1 0%, #06b6d4 100%)',
        'gradient-violet': 'linear-gradient(135deg, #8b5cf6 0%, #6366f1 100%)',
        'gradient-sunset': 'linear-gradient(135deg, #ec4899 0%, #f59e0b 100%)',
        'gradient-ocean': 'linear-gradient(135deg, #06b6d4 0%, #10b981 100%)',
        'gradient-deep': 'linear-gradient(180deg, #0a0f1a 0%, #1e1b4b 30%, #0f172a 60%, #1e293b 100%)',
        'gradient-hero': 'radial-gradient(ellipse at 50% 0%, rgba(99, 102, 241, 0.15) 0%, transparent 50%), radial-gradient(ellipse at 80% 50%, rgba(6, 182, 212, 0.1) 0%, transparent 40%), radial-gradient(ellipse at 20% 80%, rgba(139, 92, 246, 0.1) 0%, transparent 45%)',
      },
      boxShadow: {
        'glass': '0 8px 32px -8px rgba(0, 0, 0, 0.3), 0 0 0 1px rgba(255, 255, 255, 0.05) inset',
        'glass-hover': '0 16px 48px -12px rgba(99, 102, 241, 0.3), 0 0 0 1px rgba(99, 102, 241, 0.2) inset',
        'glow': '0 0 40px rgba(99, 102, 241, 0.3)',
        'glow-lg': '0 0 80px rgba(99, 102, 241, 0.4)',
        'card': '0 4px 24px -4px rgba(0, 0, 0, 0.25)',
        'card-hover': '0 20px 40px -12px rgba(0, 0, 0, 0.35)',
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', '-apple-system', 'sans-serif'],
        mono: ['JetBrains Mono', 'monospace'],
      },
      animation: {
        'float': 'float 6s ease-in-out infinite',
        'pulse-slow': 'pulse-slow 4s ease-in-out infinite',
        'shimmer': 'shimmer 2s infinite',
        'gradient': 'gradient 8s ease infinite',
        'aurora': 'aurora 15s ease infinite',
      },
      keyframes: {
        float: {
          '0%, 100%': { transform: 'translateY(0)' },
          '50%': { transform: 'translateY(-10px)' },
        },
        'pulse-slow': {
          '0%, 100%': { opacity: '0.4' },
          '50%': { opacity: '0.8' },
        },
        shimmer: {
          '0%': { backgroundPosition: '-200% 0' },
          '100%': { backgroundPosition: '200% 0' },
        },
        gradient: {
          '0%, 100%': { backgroundPosition: '0% 50%' },
          '50%': { backgroundPosition: '100% 50%' },
        },
        aurora: {
          '0%, 100%': { backgroundPosition: '0% 50%' },
          '50%': { backgroundPosition: '100% 50%' },
        },
      },
      backdropBlur: {
        xs: '2px',
      },
    },
  },
  plugins: [],
}
