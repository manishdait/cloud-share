import { Component, input, signal } from '@angular/core';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { getSize, timestampToDate } from '../../shared/utils';
import { File } from '../../types/file.type';

@Component({
  selector: 'app-card-view',
  imports: [FontAwesomeModule],
  templateUrl: './card-view.component.html',
  styleUrl: './card-view.component.css'
})
export class CardViewComponent {
  file = input.required<File>();
  menuToggle = signal(false);

  toggleMenu() {
    this.menuToggle.update(toggle => !toggle);
  }

  parseTimestamp(timestamp: Date): string {
    return timestampToDate(timestamp);
  }

  parseSize(size: number): string {
    return getSize(size);
  }
}
