import { Component, input, output } from "@angular/core";
import { timestampToDate, getSize } from "../../shared/utils";
import { File } from "../../types/file.type";
import { FontAwesomeModule } from "@fortawesome/angular-fontawesome";

@Component({
  selector: 'app-details',
  imports: [FontAwesomeModule],
  templateUrl: './details.component.html',
  styleUrl: './details.component.css'
})
export class DetailsComponent {  
  file = input.required<File>();
  loading = input.required<boolean>();
  onDownload = output<boolean>();

  parseDate(date: Date) {
    return timestampToDate(date);
  }

  parseSize(size: number) {
    return getSize(size);
  }

  onClick() {
    console.log("click");
    
    this.onDownload.emit(true);
  }
}