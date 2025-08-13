import { HttpClient, HttpHeaders, HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { File } from '../types/file.type';

const URL = 'http://localhost:8080/api/v1/uploads';

@Injectable({
  providedIn: 'root'
})
export class FileService {
  constructor(private client: HttpClient) {}

  getUserFiles(): Observable<File[]> {
    return this.client.get<File[]>(`${URL}/my`);
  }

  uploadFiles(files: FormData): Observable<File[]> {
    return this.client.post<File[]>(`${URL}`, files);
  }

  getFile(uuid: string): Observable<File> {
    return this.client.get<File>(`${URL}/${uuid}`);
  }

  getPublicFile(uuid: string): Observable<File> {
    return this.client.get<File>(`${URL}/public/${uuid}`);
  }

  toggleVisiblity(uuid: string, toggle: boolean): Observable<File> {
    return this.client.patch<File>(`${URL}/${uuid}?toggle=${toggle}`, null);
  }

  deleteFile(uuid: string): Observable<{[key: string]: string}> {
    return this.client.delete<{[key: string]: string}>(`${URL}/${uuid}`)
  }

  downloadPublicFile(uuid: string): Observable<HttpResponse<Blob>> {
    return this.client.get<Blob>(`${URL}/public/download/${uuid}`, {
      observe: 'response',
      responseType: 'blob' as 'json'
    });
  }

  downloadFile(uuid: string): Observable<HttpResponse<Blob>> {
    return this.client.get<Blob>(`${URL}/download/${uuid}`, {
      observe: 'response',
      responseType: 'blob' as 'json'
    });
  }
}
