import { Route, Routes } from 'react-router-dom';
import ProtectedRoute from './components/ProtectedRoute.jsx';
import AdminLayout from './layouts/AdminLayout.jsx';
import MainLayout from './layouts/MainLayout.jsx';
import Account from './pages/Account.jsx';
import { AdminAudit, AdminCategories, AdminDashboard, AdminLifecycle, AdminListings, AdminOrders, AdminPartners, AdminProducts, AdminReports, AdminReviews, AdminUsers } from './pages/admin/AdminPages.jsx';
import { Login, Register } from './pages/Auth.jsx';
import Calculator from './pages/Calculator.jsx';
import Cart from './pages/Cart.jsx';
import Checkout from './pages/Checkout.jsx';
import Home from './pages/Home.jsx';
import { ListingDetail, Marketplace, MyListings, Sell } from './pages/Marketplace.jsx';
import MyProducts from './pages/MyProducts.jsx';
import NotFound from './pages/NotFound.jsx';
import { OrderDetail, MyOrders } from './pages/Orders.jsx';
import Passport from './pages/Passport.jsx';
import { PartnerDashboard, PartnerRepair } from './pages/partner/PartnerPages.jsx';
import ProductDetail from './pages/ProductDetail.jsx';
import Products from './pages/Products.jsx';
import PublicPassport from './pages/PublicPassport.jsx';
import { MyRepairs, NewRepair, RepairDetail, RepairServices } from './pages/Repairs.jsx';
import Wishlist from './pages/Wishlist.jsx';

export default function App() {
  return (
    <Routes>
      <Route element={<MainLayout />}>
        <Route index element={<Home />} />
        <Route path="products" element={<Products />} />
        <Route path="products/:id" element={<ProductDetail />} />
        <Route path="login" element={<Login />} />
        <Route path="register" element={<Register />} />
        <Route path="marketplace" element={<Marketplace />} />
        <Route path="marketplace/:id" element={<ListingDetail />} />
        <Route path="repair-services" element={<RepairServices />} />
        <Route path="sell" element={<Sell />} />
        <Route path="calculator" element={<Calculator />} />
        <Route path="passport/:token" element={<PublicPassport />} />
        <Route element={<ProtectedRoute />}>
          <Route path="cart" element={<Cart />} />
          <Route path="checkout" element={<Checkout />} />
          <Route path="wishlist" element={<Wishlist />} />
          <Route path="account" element={<Account />} />
          <Route path="orders" element={<MyOrders />} />
          <Route path="orders/:id" element={<OrderDetail />} />
          <Route path="my-products" element={<MyProducts />} />
          <Route path="my-products/:id" element={<Passport />} />
          <Route path="my-listings" element={<MyListings />} />
          <Route path="repairs" element={<MyRepairs />} />
          <Route path="repairs/new" element={<NewRepair />} />
          <Route path="repairs/:id" element={<RepairDetail />} />
        </Route>
        <Route element={<ProtectedRoute roles={['REPAIR_PARTNER']} />}>
          <Route path="partner" element={<PartnerDashboard />} />
          <Route path="partner/repairs/:id" element={<PartnerRepair />} />
        </Route>
        <Route path="*" element={<NotFound />} />
      </Route>
      <Route path="admin" element={<ProtectedRoute roles={['ADMIN']} />}>
        <Route element={<AdminLayout />}>
          <Route index element={<AdminDashboard />} />
          <Route path="users" element={<AdminUsers />} />
          <Route path="products" element={<AdminProducts />} />
          <Route path="categories" element={<AdminCategories />} />
          <Route path="orders" element={<AdminOrders />} />
          <Route path="partners" element={<AdminPartners />} />
          <Route path="listings" element={<AdminListings />} />
          <Route path="reports" element={<AdminReports />} />
          <Route path="reviews" element={<AdminReviews />} />
          <Route path="lifecycle" element={<AdminLifecycle />} />
          <Route path="audit" element={<AdminAudit />} />
        </Route>
      </Route>
    </Routes>
  );
}
