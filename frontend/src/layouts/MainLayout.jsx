import { Outlet, useLocation } from 'react-router-dom';
import { useEffect } from 'react';
import CategoryNav from '../components/CategoryNav.jsx';
import Footer from '../components/Footer.jsx';
import Header from '../components/Header.jsx';

export default function MainLayout() {
  const { pathname } = useLocation();
  useEffect(() => { window.scrollTo(0, 0); }, [pathname]);
  return (
    <div className="flex min-h-screen flex-col">
      <Header />
      <CategoryNav />
      <main className="flex-1"><Outlet /></main>
      <Footer />
    </div>
  );
}
