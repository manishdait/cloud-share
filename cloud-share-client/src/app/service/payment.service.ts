import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Plan } from '../types/plan.type';
import { PaymentDto, PaymentVerificationDto, TransactionDto } from '../types/payment.type';
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

  verify(request: PaymentVerificationDto): Observable<PaymentDto> {
    return this.client.post<PaymentDto>(`${URL}/verify`, request);
  }

  getTransaction(): Observable<TransactionDto[]> {
    return this.client.get<TransactionDto[]>(`${URL}/transactions`);
  }
}
