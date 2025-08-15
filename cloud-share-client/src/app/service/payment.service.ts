import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Plan } from '../types/plan.type';
import { PaymentDto } from '../types/payment.type';
import { Observable } from 'rxjs';

const URL = 'http://localhost:8080/api/v1/payments';

@Injectable({
  providedIn: 'root'
})
export class PaymentService {
  constructor(private client: HttpClient) {}

  purchase(plan: Plan): Observable<PaymentDto> {
    return this.client.post<PaymentDto>(`${URL}/create-order?plan=${plan}`, null);
  }
  purchasetest(amount: number, currency: string): Observable<PaymentDto> {
    return this.client.post<PaymentDto>(`${URL}/create-order?amount=${amount}&currency=${currency}`, null);
  }
}
