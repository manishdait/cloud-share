import { Component, inject, OnInit, signal } from '@angular/core';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { FileService } from '../../service/file.service';
import { File } from '../../types/file.type';
import { ActivatedRoute } from '@angular/router';
import { DetailsComponent } from '../../components/details/details.component';

@Component({
  selector: 'app-file',
  imports: [FontAwesomeModule, DetailsComponent],
  templateUrl: './file.component.html',
  styleUrl: './file.component.css'
})
export class FileComponent implements OnInit {
  activeRouter = inject(ActivatedRoute);
  fileService = inject(FileService);
  file = signal<File|null>(null);

  uuid = this.activeRouter.snapshot.params['id'];

  ngOnInit(): void {
    this.fileService.getPublicFile(this.uuid).subscribe({
      next: (res) => {
        this.file.set(res);
      },
      error: (err) => {
        this.file.set(null);
      }
    })
  }

  
}
