import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { HTTP_INTERCEPTORS, provideHttpClient, withFetch, withInterceptors, withInterceptorsFromDi } from '@angular/common/http';
import { provideNgxWebstorage, withLocalStorage } from 'ngx-webstorage';
import { RefreshTokenInterceptor } from './shared/interceptor/refresh-token.interceptor';
import { AccessTokenInterceptor } from './shared/interceptor/access-token.interceptor';
import { provideState, provideStore } from '@ngrx/store';
import { fileReducer } from './store/file/file.reducer';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideNgxWebstorage(withLocalStorage()),
    provideStore(),
    provideState({name: 'fileState', reducer: fileReducer}),
    provideHttpClient(withInterceptorsFromDi()),
    [
        { provide: HTTP_INTERCEPTORS, useClass: RefreshTokenInterceptor, multi: true },
        { provide: HTTP_INTERCEPTORS, useClass: AccessTokenInterceptor, multi: true }
    ],
    provideStore()
]
};
