import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Route, Routes } from 'react-router-dom';
import ProtectedRoute from '../components/ProtectedRoute.jsx';
import { Login, Register } from '../pages/Auth.jsx';
import { json, mockApi, renderApp, user } from './helpers.jsx';

describe('authentication flows', () => {
  it('shows field validation errors without calling the API', async () => {
    const calls = mockApi({});
    renderApp(<Login />);
    await userEvent.click(await screen.findByRole('button', { name: /sign in/i }));
    expect(await screen.findByText(/valid email/i)).toBeInTheDocument();
    expect(screen.getByText(/enter your password/i)).toBeInTheDocument();
    expect(calls.some((c) => c.path === '/api/auth/login')).toBe(false);
  });

  it('logs in and shows server errors for bad credentials', async () => {
    mockApi({ 'POST /api/auth/login': () => json({ message: 'Invalid email or password' }, 401) });
    renderApp(<Login />);
    await userEvent.type(await screen.findByLabelText('Email'), 'a@b.co');
    await userEvent.type(screen.getByLabelText('Password'), 'wrongpass1');
    await userEvent.click(screen.getByRole('button', { name: /sign in/i }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid email or password');
  });

  it('registration validates matching passwords and never sends a role', async () => {
    const calls = mockApi({ 'POST /api/auth/register': { accessToken: 't', user } });
    renderApp(<Register />);
    await userEvent.type(await screen.findByLabelText('Full name'), 'Asha Test');
    await userEvent.type(screen.getByLabelText('Email'), 'a@b.co');
    await userEvent.type(screen.getByLabelText(/^password/i), 'Passw0rd!');
    await userEvent.type(screen.getByLabelText(/^confirm password/i), 'different');
    await userEvent.click(screen.getByRole('button', { name: /create account/i }));
    expect(await screen.findByText(/do not match/i)).toBeInTheDocument();
    await userEvent.clear(screen.getByLabelText(/^confirm password/i));
    await userEvent.type(screen.getByLabelText(/^confirm password/i), 'Passw0rd!');
    await userEvent.click(screen.getByRole('button', { name: /create account/i }));
    await waitFor(() => expect(calls.find((c) => c.path === '/api/auth/register')).toBeTruthy());
    expect(calls.find((c) => c.path === '/api/auth/register').body).not.toHaveProperty('role');
  });

  it('protected routes redirect anonymous visitors to login', async () => {
    mockApi({});
    renderApp(
      <Routes>
        <Route element={<ProtectedRoute />}><Route path="/secret" element={<p>secret page</p>} /></Route>
        <Route path="/login" element={<p>login page</p>} />
      </Routes>, { route: '/secret' });
    expect(await screen.findByText('login page')).toBeInTheDocument();
    expect(screen.queryByText('secret page')).not.toBeInTheDocument();
  });

  it('role-restricted routes block signed-in users without the role', async () => {
    mockApi({ 'POST /api/auth/refresh': { accessToken: 't', user } });
    renderApp(
      <Routes>
        <Route element={<ProtectedRoute roles={['ADMIN']} />}><Route path="/admin" element={<p>admin area</p>} /></Route>
        <Route path="/" element={<p>home</p>} />
      </Routes>, { route: '/admin' });
    expect(await screen.findByText('home')).toBeInTheDocument();
    expect(screen.queryByText('admin area')).not.toBeInTheDocument();
  });
});
