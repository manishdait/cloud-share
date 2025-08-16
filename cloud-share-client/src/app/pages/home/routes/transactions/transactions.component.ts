import { Component, inject, OnInit, signal } from '@angular/core';
import { PaymentService } from '../../../../service/payment.service';
import { TransactionDto } from '../../../../types/payment.type';
import { timestampToDate } from '../../../../shared/utils';

@Component({
  selector: 'app-transactions',
  imports: [],
  templateUrl: './transactions.component.html',
  styleUrl: './transactions.component.css'
})
export class TransactionsComponent implements OnInit {
  paymentService = inject(PaymentService);
  transactions = signal<TransactionDto[]>([]);

  ngOnInit(): void {
    this.paymentService.getTransaction().subscribe({
      next: (res) => {
        this.transactions.set(res);
      }
    })
  }

  parseDate(timestamp: Date) {
    return timestampToDate(timestamp);
  }
}
