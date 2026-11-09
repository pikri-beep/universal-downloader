/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        funi: {
          bg: '#F6F8FA',
          coral: '#FFE4EC',
          coralBorder: '#FF8DA1',
          coralText: '#9D174D',
          coralHover: '#FFD4E2',
          mint: '#DCFCE7',
          mintBorder: '#86EFAC',
          mintText: '#166534',
          sky: '#E0F2FE',
          skyBorder: '#7DD3FC',
          skyText: '#0369A1',
        }
      },
      boxShadow: {
        'tactile': '0 6px 0 0 rgba(226, 232, 240, 1)',
        'tactile-hover': '0 4px 0 0 rgba(203, 213, 225, 1)',
        'tactile-coral': '0 5px 0 0 #F472B6',
        'tactile-mint': '0 5px 0 0 #4ADE80',
        'tactile-sky': '0 5px 0 0 #38BDF8',
      },
      borderRadius: {
        '3xl': '24px',
        '4xl': '32px',
      }
    },
  },
  plugins: [],
};
