import { Component, inject, signal } from '@angular/core';
import { AuthService } from '../../../../service/auth.service';
import { PaymentService } from '../../../../service/payment.service';
import { Plan } from '../../../../types/plan.type';
import { environment } from '../../../../../environments/environment';

@Component({
  selector: 'app-credits',
  imports: [],
  templateUrl: './credits.component.html',
  styleUrl: './credits.component.css'
})
export class CreditsComponent {
  authService = inject(AuthService);
  paymentService = inject(PaymentService);

  user = this.authService.user;

  purchase(plan: Plan) {
    this.paymentService.purchasetest(100, 'INR').subscribe({
      next: (res) => {
        if (!res.success) {
          return;
        }

        const options = {
          key: environment.RAZORPAY_API_KEY,
          amount: 100,
          currency: 'INR',
          name: 'CloudShare',
          order_id: res.orderId,
          handler: function (response: any) {
            console.log('Payment ID:', response.razorpay_payment_id);
            console.log('Order ID:', response.razorpay_order_id);
            console.log('Signature:', response.razorpay_signature);
            alert('Payment successful!');
          },
          prefill: {
            name: this.user().firstname + ' ' + this.user().lastname,
            email: this.user().email,
            contact: '9999999999',
          },
          theme: {
            color: '#9810fa',
          }
        };

        const rzp = new (window as any).Razorpay(options);
        rzp.open();
      }
    })
  }
}
