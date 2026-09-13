import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AuthService } from './core/services/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  template: '<router-outlet />'
})
export class AppComponent {
  constructor(auth: AuthService) {
    if (auth.token) {
      auth.me().subscribe({ error: () => auth.logout() });
    }
  }
}
