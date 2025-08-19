import { HttpBackend, HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable, signal } from '@angular/core';
import { AuthRequest, AuthResponse, RegistrationRequest } from '../types/auth.type';
import { catchError, map, Observable, of, switchMap } from 'rxjs';
import { LocalStorageService } from 'ngx-webstorage';
import { UserService } from './user.service';
import { UserDto } from '../types/user.type';

const URL = 'http://localhost:8080/api/v1/auth';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private client: HttpClient;

  user = signal<UserDto>({
    firstname: '',
    lastname: '',
    email: '',
    credit: 0
  });

  constructor(private backend: HttpBackend, private storage: LocalStorageService, private userService: UserService) {
    this.client = new HttpClient(backend);
  }

  registerUser(request: RegistrationRequest): Observable<{[key: string]: boolean}> {
    return this.client.post<{[key: string]: boolean}>(`${URL}/sign-up`, request);
  }

  verifyEmail(email: string, token: string): Observable<AuthResponse> {
    return this.client.post<AuthResponse>(`${URL}/verify-email?email=${email}&token=${token}`, null).pipe(
      switchMap((res) => {
        this.storeCred(res);
        return this.userService.getUserSummary().pipe(
          map((userRes) => {
            this.user.set(userRes);
            return res;
          })
        )
      })
    );
  }

  renewToken(email: string): Observable<{[key: string]: boolean}> {
    return this.client.post<{[key: string]: boolean}>(`${URL}/renew-token?email=${email}`, null);
  }

  renewPasswordToken(email: string): Observable<{[key: string]: boolean}> {
    return this.client.post<{[key: string]: boolean}>(`${URL}/renew-password-token?email=${email}`, null);
  }

  authenticateUser(request: AuthRequest): Observable<AuthResponse> {
    return this.client.post<AuthResponse>(`${URL}/login`, request).pipe(
      switchMap((res) => {
        this.storeCred(res);
        return this.userService.getUserSummary().pipe(
          map((userRes) => {
            this.user.set(userRes);
            return res;
          })
        )
      })
    );
  }

  forgotPassword(email: string): Observable<{[key: string]: boolean}> {
    return this.client.post<{[key: string]: boolean}>(`${URL}/forgot-password/${email}`, null);
  }

  refreshToken(): Observable<AuthResponse> {
    return this.client.post<AuthResponse>(`${URL}/refresh`, null, {headers:{'Authorization': 'Bearer ' + this.getRefreshToken()}}).pipe(
      map((res) => {
        this.storeCred(res);
        return res;
      })
    );
  }

  isAuthenticated(): Observable<boolean> {
    const accessToken = this.getAccessToken();
    if (!accessToken) {
      return of(false);
    }

    return this.userService.getUserSummary().pipe(
      map((res) => {
        this.user.set(res);
        return true;
      }),
      catchError((err) => {
        this.storage.clear();
        return of(false);
      })
    );
  }

  getAccessToken() {
    return this.storage.retrieve('accessToken');
  }

  getRefreshToken() {
    return this.storage.retrieve('refreshToken');
  }

  private storeCred(res: AuthResponse) {
    this.storage.store('accessToken', res.accessToken);
    this.storage.store('refreshToken', res.refreshToken);
  }
}
