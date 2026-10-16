import { Link } from 'react-router-dom';
import { EmptyState } from '../components/ui.jsx';

export default function NotFound() {
  return <div className="container-x py-20"><EmptyState title="Page not found" text="The page you’re looking for doesn’t exist." action={<Link to="/" className="btn-primary">Back to home</Link>} /></div>;
}
