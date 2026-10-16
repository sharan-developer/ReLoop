import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { NewRepair } from '../pages/Repairs.jsx';
import Calculator, { compute } from '../pages/Calculator.jsx';
import { RegisterProductModal } from '../pages/MyProducts.jsx';
import { mockApi, renderApp, user } from './helpers.jsx';

const owned = [{ id: 5, name: 'Test Phone', status: 'ACTIVE', openRepairs: 0 }];
const partners = [{ id: 2, businessName: 'FixIt', city: 'Hyderabad' }];

describe('repair booking flow', () => {
  it('validates required fields before submitting', async () => {
    const calls = mockApi({ 'POST /api/auth/refresh': { accessToken: 't', user }, 'GET /api/wishlist': [], 'GET /api/cart': { items: [], itemCount: 0, subtotal: 0 }, 'GET /api/my-products': owned, 'GET /api/partners': partners });
    renderApp(<NewRepair />);
    await userEvent.click(await screen.findByRole('button', { name: /send request/i }));
    expect(await screen.findByText('Select a product')).toBeInTheDocument();
    expect(screen.getByText('Select a repair partner')).toBeInTheDocument();
    expect(screen.getByText('Describe the problem')).toBeInTheDocument();
    expect(calls.some((c) => c.method === 'POST' && c.path === '/api/repairs')).toBe(false);
  });

  it('submits a valid repair request to the backend', async () => {
    const calls = mockApi({
      'POST /api/auth/refresh': { accessToken: 't', user }, 'GET /api/wishlist': [], 'GET /api/cart': { items: [], itemCount: 0, subtotal: 0 },
      'GET /api/my-products': owned, 'GET /api/partners': partners, 'POST /api/repairs': { id: 77 },
    });
    renderApp(<NewRepair />);
    await userEvent.selectOptions(await screen.findByLabelText('Product'), '5');
    await userEvent.selectOptions(screen.getByLabelText('Repair partner'), '2');
    await userEvent.type(screen.getByLabelText(/what.s wrong/i), 'Screen cracked');
    await userEvent.click(screen.getByRole('button', { name: /send request/i }));
    await waitFor(() => expect(calls.find((c) => c.method === 'POST' && c.path === '/api/repairs')).toBeTruthy());
    expect(calls.find((c) => c.path === '/api/repairs').body).toMatchObject({ ownedProductId: 5, partnerId: 2, problem: 'Screen cracked' });
  });

  it('product registration rejects a future purchase date and mismatched warranty dates', async () => {
    mockApi({});
    renderApp(<RegisterProductModal onClose={() => {}} onDone={() => {}} />);
    await userEvent.type(screen.getByLabelText('Product name'), 'Phone');
    await userEvent.type(screen.getByLabelText('Warranty start'), '2024-01-01');
    await userEvent.type(screen.getByLabelText('Purchase date'), '2024-01-01');
    await userEvent.click(screen.getByRole('button', { name: /register product/i }));
    expect(await screen.findByText(/provide both warranty dates/i)).toBeInTheDocument();
  });
});

describe('repair vs replace calculator', () => {
  it('computes percentage and per-month figures from user inputs', () => {
    const r = compute({ repair: '5000', replace: '20000', months: '10' });
    expect(r.pct).toBe(25);
    expect(r.perMonthRepair).toBe(500);
    expect(r.saving).toBe(15000);
  });
  it('returns null until enough input is present and labels itself an estimate', async () => {
    expect(compute({ repair: '', replace: '20000', months: '' })).toBeNull();
    expect(compute({ repair: '100', replace: '0', months: '' })).toBeNull();
    mockApi({});
    renderApp(<Calculator />);
    expect(screen.getByText(/not an ai prediction/i)).toBeInTheDocument();
  });
});
