import { Component, inject, input } from '@angular/core';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { File } from '../../types/file.type';
import { getSize, timestampToDate } from '../../shared/utils';
import { FileService } from '../../service/file.service';
import { RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { CommonModule } from '@angular/common';
import { Store } from '@ngrx/store';
import { AppState } from '../../store/app.state';
import { deleteFile, updateFile } from '../../store/file/file.action';

@Component({
  selector: 'app-tabular-view',
  imports: [CommonModule, FontAwesomeModule],
  templateUrl: './tabular-view.component.html',
  styleUrl: './tabular-view.component.css'
})
export class TabularViewComponent {
  fileService = inject(FileService);
  files = input.required<Observable<File[]>>();

  constructor(private store: Store<AppState>) {
  }

  parseTimestamp(timestamp: Date): string {
    return timestampToDate(timestamp);
  }

  parseSize(size: number): string {
    return getSize(size);
  }


  toggleVisiblity(uuid: string, toggle: boolean) {
    this.fileService.toggleVisiblity(uuid, toggle).subscribe({
      next: (res) => {
        this.store.dispatch(updateFile({file: res}));
      },
      error: (err) => {
        
      }
    })
  }

  deleteFile(uuid: string) {
    this.fileService.deleteFile(uuid).subscribe({
      next: (res) => {
        this.store.dispatch(deleteFile({uuid: res['deleted']}));
        console.log(res);
      },
      error: (err) => {
      }
    })
  }

  downloadFile(uuid: string) {

  }

  copyLink(uuid: string) {
    navigator.clipboard.writeText(`http://localhost:4200/files/${uuid}`);
  }
}
