import { Component } from "@angular/core";
import { SideNavComponent } from "../../components/side-nav/side-nav.component";
import { RouterOutlet } from "@angular/router";

@Component({
  selector: 'app-home',
  imports: [RouterOutlet, SideNavComponent],
  templateUrl: './home.component.html',
  styleUrl: './home.component.css'
})
export class HomeComponent {

}