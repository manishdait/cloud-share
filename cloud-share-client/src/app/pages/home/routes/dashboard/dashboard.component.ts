import { Component, inject, OnInit, signal } from "@angular/core";
import { FontAwesomeModule } from "@fortawesome/angular-fontawesome";
import { TabularViewComponent } from "../../../../components/tabular-view/tabular-view.component";
import { CardViewComponent } from "../../../../components/card-view/card-view.component";
import { AuthService } from "../../../../service/auth.service";
import { FileService } from "../../../../service/file.service";
import { File } from "../../../../types/file.type";
import { CommonModule } from "@angular/common";
import { Observable } from "rxjs";
import { Store } from "@ngrx/store";
import { AppState } from "../../../../store/app.state";
import { fileSelector } from "../../../../store/file/file.selector";
import { setFiles } from "../../../../store/file/file.action";

@Component({
  selector: 'app-dashboard',
  imports: [CommonModule, FontAwesomeModule, TabularViewComponent, CardViewComponent],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit {
  authService = inject(AuthService);
  fileService = inject(FileService);

  user = this.authService.user;
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