import { HttpClient } from '@angular/common/http';
import { Injectable, signal } from '@angular/core';
import { UserDto } from '../types/user.type';
import { map, Observable } from 'rxjs';

const URL = 'http://localhost:8080/api/v1/users';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  constructor(private client: HttpClient, ) {}

  getUserSummary(): Observable<UserDto> {
    return this.client.get<UserDto>(`${URL}/me`).pipe(
      map((res) => {
        return res;
      })
    );
  }
}
