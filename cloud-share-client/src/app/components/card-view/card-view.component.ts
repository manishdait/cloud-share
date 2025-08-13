import { Component, inject, input, signal } from '@angular/core';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { getSize, timestampToDate } from '../../shared/utils';
import { File } from '../../types/file.type';
import { FileService } from '../../service/file.service';
import { Store } from '@ngrx/store';
import { AppState } from '../../store/app.state';
import { deleteFile, updateFile } from '../../store/file/file.action';

@Component({
  selector: 'app-card-view',
  imports: [FontAwesomeModule],
  templateUrl: './card-view.component.html',
  styleUrl: './card-view.component.css'
})
export class CardViewComponent {
  file = input.required<File>();
  
  fileService = inject(FileService);
  menuToggle = signal(false);

  constructor(private store: Store<AppState>) {}

  toggleMenu() {
    this.menuToggle.update(toggle => !toggle);
  }

  parseTimestamp(timestamp: Date): string {
    return timestampToDate(timestamp);
  }

  parseSize(size: number): string {
    return getSize(size);
  }

  toggleVisiblity(toggle: boolean) {
    this.fileService.toggleVisiblity(this.file().uuid, toggle).subscribe({
      next: (res) => {
        this.store.dispatch(updateFile({file: res}));
      },
      error: (err) => {
        
      }
    })
  }

  deleteFile() {
    this.fileService.deleteFile(this.file().uuid).subscribe({
      next: (res) => {
        this.store.dispatch(deleteFile({uuid: res['deleted']}));
        console.log(res);
      },
      error: (err) => {
      }
    })
  }

  download() {
    this.fileService.downloadPublicFile(this.file().uuid).subscribe({
      next: (res) => {
        const fileName = res.headers.get('X-File-Name') as string || 'download';
        const blob = new Blob([res.body!], { type: res.headers.get('Content-Type') as string });
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = fileName;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
      }
    })
  }

  copyLink() {
    navigator.clipboard.writeText(`http://localhost:4200/files/${this.file().uuid}`);
  }
}
