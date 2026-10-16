import { Link } from 'react-router-dom';
import ProductCard from '../components/ProductCard.jsx';
import { EmptyState } from '../components/ui.jsx';
import { useWishlist } from '../context/WishlistContext.jsx';

export default function Wishlist() {
  const { items } = useWishlist();
  return (
    <div className="container-x py-8">
      <h1 className="mb-5 text-2xl">My wishlist</h1>
      {items.length === 0
        ? <EmptyState title="Your wishlist is empty" text="Tap the heart on any product to save it here." action={<Link to="/products" className="btn-primary">Browse products</Link>} />
        : <div className="grid grid-cols-2 gap-3 sm:gap-4 md:grid-cols-3 lg:grid-cols-4">{items.map((p) => <ProductCard key={p.id} product={p} />)}</div>}
    </div>
  );
}
