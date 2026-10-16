/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        brand: { 50: '#effaf3', 100: '#d8f3e1', 200: '#b3e6c6', 500: '#22a35a', 600: '#178a49', 700: '#136f3b' },
        ink: { 900: '#14181b', 800: '#1d2327', 700: '#2a3237' },
      },
      fontFamily: { sans: ['Inter', 'ui-sans-serif', 'system-ui', 'Segoe UI', 'Roboto', 'sans-serif'] },
    },
  },
  plugins: [],
};
