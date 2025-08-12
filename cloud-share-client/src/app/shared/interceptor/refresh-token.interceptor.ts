import { Injectable } from '@angular/core';
import {
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpInterceptor,
  HttpErrorResponse
} from '@angular/common/http';
import { catchError, Observable, switchMap, throwError } from 'rxjs';
import { AuthService } from '../../service/auth.service';

@Injectable()
export class RefreshTokenInterceptor implements HttpInterceptor {

  constructor(private authService: AuthService) {}

  intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    let count = 0;

    return next.handle(request).pipe(
      catchError((err) => {
        if (err instanceof HttpErrorResponse && err.status === 401) {
          if (!request.url.includes('login') && count < 2) {
            count++;
            return this.handle401(request, next)
          } else {
            return throwError(() => {throw err});
          }
        } else {
          return throwError(() => {throw err});
        }
      })
    );
  }

  addAccessToken(request: HttpRequest<any>): HttpRequest<any> {
    const accessToken: string = this.authService.getAccessToken();
    request = request.clone({
      setHeaders: {
        Authorization: `Bearer ${accessToken}`
      }
    });

    return request;
  }

  handle401(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    return this.authService.refreshToken().pipe(
      switchMap((res) => {
        return next.handle(this.addAccessToken(request));
      })
    );
  }
}

