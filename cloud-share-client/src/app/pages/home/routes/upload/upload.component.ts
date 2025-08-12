import { Component, inject, signal } from '@angular/core';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { FileService } from '../../../../service/file.service';
import { Store } from '@ngrx/store';
import { AppState } from '../../../../store/app.state';
import { addFiles } from '../../../../store/file/file.action';

@Component({
  selector: 'app-upload',
  imports: [FontAwesomeModule],
  templateUrl: './upload.component.html',
  styleUrl: './upload.component.css'
})
export class UploadComponent {
  filesService = inject(FileService);

  dragging = signal(false);
  files = signal<File[]>([]);

  constructor(private store: Store<AppState>) {
  }

  onInput(event: Event) {
    const target = event.target as HTMLInputElement;
    console.log(target);

    const fileList = target.files;

    const files:File[] = [];
    for (let file of fileList!) {
      files.push(file);
    }

    this.files.update(arr => [...files, ...arr])
  }

  removeFile(index: number) {
    this.files.update(arr => {
      arr.splice(index, 1);
      return arr;
    });
  }

  onDragOver(event: Event) {
    event.preventDefault();
    event.stopPropagation();
    this.dragging.set(true);
    
  }

  onDragLeave(event: Event) {
    event.preventDefault();
    event.stopPropagation();
    this.dragging.set(false);
    
  }

  onDrop(event: DragEvent) {
    event.preventDefault();
    event.stopPropagation();

    const target = event.dataTransfer;
    const file: File = target!.files![0];

    this.files.update(arr => [file, ...arr]);
  }

  onUpload() {
    if (this.files().length == 0) {return;}

    const formData = new FormData();

    for (let file of this.files()) {
      formData.append('files', file, file.name);
    }

    this.filesService.uploadFiles(formData).subscribe({
      next: (res) => {
        this.store.dispatch(addFiles({files: res}));
        this.files.set([]);
      }
    })
  }
}
