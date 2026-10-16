import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Route, Routes } from 'react-router-dom';
import Cart from '../pages/Cart.jsx';
import Products from '../pages/Products.jsx';
import { mockApi, renderApp, user } from './helpers.jsx';

const product = (o = {}) => ({ id: 1, name: 'Galaxy S22', brand: 'Samsung', condition: 'REFURBISHED', price: 32999, originalPrice: 59999, discountPercent: 45, imageUrl: '/x.svg', stock: 5, available: true, rating: null, reviewCount: 0, ...o });
const page = (items) => ({ items, page: 0, size: 12, totalElements: items.length, totalPages: 1 });

describe('product search and filtering', () => {
  it('sends the search term and filters to the backend and renders results', async () => {
    const calls = mockApi({
      'GET /api/categories': [{ id: 1, name: 'Smartphones', slug: 'smartphones' }],
      'GET /api/products': page([product()]),
    });
    renderApp(<Products />, { route: '/products?q=galaxy&condition=REFURBISHED&minPrice=1000' });
    expect(await screen.findByText('Galaxy S22')).toBeInTheDocument();
    const call = calls.find((c) => c.path.startsWith('/api/products?'));
    expect(call.path).toContain('q=galaxy');
    expect(call.path).toContain('condition=REFURBISHED');
    expect(call.path).toContain('minPrice=1000');
  });

  it('shows an empty state and an error state with retry', async () => {
    mockApi({ 'GET /api/categories': [], 'GET /api/products': page([]) });
    const { unmount } = renderApp(<Products />, { route: '/products?q=zzz' });
    expect(await screen.findByText(/no products found/i)).toBeInTheDocument();
    unmount();
    mockApi({ 'GET /api/categories': [], 'GET /api/products': () => Promise.resolve(new Response('{"message":"boom"}', { status: 500 })) });
    renderApp(<Products />);
    expect(await screen.findByRole('alert')).toHaveTextContent('boom');
    expect(screen.getByRole('button', { name: /try again/i })).toBeInTheDocument();
  });

  it('applies a price filter to the URL-driven query', async () => {
    const calls = mockApi({ 'GET /api/categories': [], 'GET /api/products': page([product()]) });
    renderApp(<Products />);
    await screen.findByText('Galaxy S22');
    await userEvent.type(screen.getByLabelText('Maximum price'), '50000');
    await userEvent.click(screen.getByRole('button', { name: 'Apply' }));
    await waitFor(() => expect(calls.some((c) => c.path.includes('maxPrice=50000'))).toBe(true));
  });
});

describe('cart interactions', () => {
  const cart = (qty, available = true) => ({ items: [{ id: 9, productId: 1, name: 'Galaxy S22', imageUrl: '/x.svg', unitPrice: 1000, quantity: qty, stock: available ? 5 : 1, available, lineTotal: 1000 * qty }], itemCount: qty, subtotal: available ? 1000 * qty : 0, hasUnavailable: !available });

  it('updates quantity through the API and re-renders server totals', async () => {
    let q = 1;
    const calls = mockApi({
      'POST /api/auth/refresh': { accessToken: 't', user },
      'GET /api/wishlist': [],
      'GET /api/cart': () => cart(q),
      'PUT /api/cart/items/9': (p, init) => { q = JSON.parse(init.body).quantity; return cart(q); },
    });
    renderApp(<Routes><Route path="/cart" element={<Cart />} /></Routes>, { route: '/cart' });
    expect(await screen.findByText('Galaxy S22')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Increase' }));
    await waitFor(() => expect(calls.find((c) => c.method === 'PUT').body).toEqual({ quantity: 2 }));
    expect(await screen.findAllByText(/₹2,000/)).not.toHaveLength(0);
  });

  it('blocks checkout and explains when an item is unavailable', async () => {
    mockApi({ 'POST /api/auth/refresh': { accessToken: 't', user }, 'GET /api/wishlist': [], 'GET /api/cart': cart(3, false) });
    renderApp(<Routes><Route path="/cart" element={<Cart />} /></Routes>, { route: '/cart' });
    expect(await screen.findByText(/only 1 available/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /proceed to checkout/i })).toBeDisabled();
  });

  it('shows the empty cart state', async () => {
    mockApi({ 'POST /api/auth/refresh': { accessToken: 't', user }, 'GET /api/wishlist': [], 'GET /api/cart': { items: [], itemCount: 0, subtotal: 0, hasUnavailable: false } });
    renderApp(<Cart />);
    expect(await screen.findByText(/your cart is empty/i)).toBeInTheDocument();
  });
});
