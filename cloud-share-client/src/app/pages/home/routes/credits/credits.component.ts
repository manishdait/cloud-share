import { Component, inject } from '@angular/core';
import { AuthService } from '../../../../service/auth.service';
import { PaymentService } from '../../../../service/payment.service';
import { Plan, Plans } from '../../../../types/plan.type';
import { environment } from '../../../../../environments/environment';
import { PaymentVerificationDto } from '../../../../types/payment.type';
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

  onPaymnet(response: any) {
    const request: PaymentVerificationDto = {
      orederId: response.razorpay_order_id,
      paymentId: response.razorpay_payment_id,
      signature: response.razorpay_signature
    }

    this.paymentService.verify(request).subscribe({
      next: (res) => {
        alert(res.message);
      }
    })
  }

  purchase(plan: Plan) {
    this.paymentService.purchase(plan).subscribe({
      next: (res) => {
        if (!res.success) {
          return;
        }

        const options = {
          key: environment.RAZORPAY_API_KEY,
          amount: Plans[plan].amount,
          currency: 'INR',
          name: 'CloudShare',
          order_id: res.orderId,
          handler: this.onPaymnet.bind(this),
          prefill: {
            name: this.user()!.firstname + ' ' + this.user()!.lastname,
            email: this.user()!.email
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
