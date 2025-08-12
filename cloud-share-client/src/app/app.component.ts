import { Component, inject, OnInit } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { FaIconLibrary } from '@fortawesome/angular-fontawesome';
import { fontawsomeIcons } from './shared/fa-icons';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit {
  faLibrary = inject(FaIconLibrary);
  
  ngOnInit(): void {
    this.faLibrary.addIcons(...fontawsomeIcons);
  }
}
