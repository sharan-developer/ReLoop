// Opens Razorpay Checkout for an order created by the backend. The frontend callback is NOT proof of payment:
// the caller must send the result to the backend, which verifies the signature before anything is marked paid.
function loadScript() {
  return new Promise((resolve, reject) => {
    if (window.Razorpay) return resolve();
    const s = document.createElement('script');
    s.src = 'https://checkout.razorpay.com/v1/checkout.js';
    s.onload = resolve;
    s.onerror = () => reject(new Error('Could not load the payment window. Check your connection.'));
    document.body.appendChild(s);
  });
}

export async function openRazorpay(order, user) {
  await loadScript();
  return new Promise((resolve, reject) => {
    const rzp = new window.Razorpay({
      key: order.razorpayKeyId,
      order_id: order.razorpayOrderId,
      name: 'ReLoop',
      description: `Order ${order.orderNumber}`,
      prefill: { name: user.fullName, email: user.email, contact: user.phone || '' },
      theme: { color: '#178a49' },
      handler: (resp) => resolve({
        orderId: order.id,
        razorpayOrderId: resp.razorpay_order_id,
        razorpayPaymentId: resp.razorpay_payment_id,
        razorpaySignature: resp.razorpay_signature,
      }),
      modal: { ondismiss: () => reject(new Error('Payment was cancelled. Your order is saved; you can pay again or cancel it from My Orders.')) },
    });
    rzp.on('payment.failed', (r) => reject(new Error(r?.error?.description || 'Payment failed')));
    rzp.open();
  });
}
