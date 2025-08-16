import { Plan } from "./plan.type";

export interface PaymentDto {
  readonly orderId: string;
  success: boolean;
  message: string;
}

export interface PaymentVerificationDto {
  orederId: string;
  paymentId: string; 
  signature: string;
}

export interface TransactionDto {
  id: string;
  plan: Plan;
  amount: number;
  status: string;
  timestamp: Date;
}