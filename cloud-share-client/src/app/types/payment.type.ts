export interface PaymentDto {
  readonly orderId: string;
  success: boolean;
  message: string;
}