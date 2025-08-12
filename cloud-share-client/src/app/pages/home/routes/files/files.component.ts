import { Component, inject, OnInit, signal } from '@angular/core';
import { TabularViewComponent } from '../../../../components/tabular-view/tabular-view.component';
import { CardViewComponent } from '../../../../components/card-view/card-view.component';
import { FileService } from '../../../../service/file.service';
import { File } from '../../../../types/file.type';
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { Observable } from 'rxjs';
import { Store } from '@ngrx/store';
import { AppState } from '../../../../store/app.state';
import { fileSelector } from '../../../../store/file/file.selector';
import { setFiles } from '../../../../store/file/file.action';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';

@Component({
  selector: 'app-files',
  imports: [CommonModule, FontAwesomeModule,  TabularViewComponent, CardViewComponent],
  templateUrl: './files.component.html',
  styleUrl: './files.component.css'
})
export class FilesComponent implements OnInit {
  fileService = inject(FileService);

  files$: Observable<File[]>;

  constructor(private store: Store<AppState>) {
    this.files$ = store.select(fileSelector);
  }

  ngOnInit(): void {
    this.fileService.getUserFiles().subscribe({
      next: (res) => {
        this.store.dispatch(setFiles({files: res}));
      }
    });
  }
}
