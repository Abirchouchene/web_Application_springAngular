import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { MaterialModule } from './material.module';

@Component({
    selector: 'app-root',
    standalone: true,
    imports: [RouterOutlet, MaterialModule],
    template: `
        <router-outlet></router-outlet>
    `
})
export class AppComponent {
  title = 'Modernize Angular Admin Template';
}
