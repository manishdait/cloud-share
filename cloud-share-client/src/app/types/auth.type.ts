export interface RegistrationRequest {
  firstname: string;
  lastname: string;
  email: string;
  password: string;
};

export interface AuthRequest {
  email: string;
  password: string;
};

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
};

export interface ResetPasswordRequest {
  password: string;
  token: string;
}