import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="login-container">
      <form class="login-card" (ngSubmit)="onSubmit()">
        <h1><i class="material-icons">factory</i> Simulador Industrial</h1>
        <p class="subtitle">Portal de Configurações — acesso restrito</p>

        <label>
          Utilizador
          <input type="text" name="username" [(ngModel)]="username" autocomplete="username" required />
        </label>

        <label>
          Password
          <input type="password" name="password" [(ngModel)]="password" autocomplete="current-password" required />
        </label>

        <p class="error" *ngIf="error()">{{ error() }}</p>

        <button type="submit">Entrar</button>
      </form>
    </div>
  `,
  styles: [
    `
      .login-container {
        min-height: 100vh;
        display: flex;
        align-items: center;
        justify-content: center;
        background: #f8faff;
      }
      .login-card {
        background: white;
        padding: 2.5rem;
        border-radius: 12px;
        box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
        width: 100%;
        max-width: 360px;
        display: flex;
        flex-direction: column;
        gap: 1rem;
      }
      h1 {
        margin: 0;
        font-size: 1.5rem;
        display: flex;
        align-items: center;
        gap: 0.5rem;
      }
      .subtitle {
        margin: 0;
        color: #666;
        font-size: 0.9rem;
      }
      label {
        display: flex;
        flex-direction: column;
        gap: 0.35rem;
        font-size: 0.9rem;
        font-weight: 600;
      }
      input {
        padding: 0.6rem 0.75rem;
        border: 1px solid #ccc;
        border-radius: 6px;
        font-size: 1rem;
      }
      button {
        margin-top: 0.5rem;
        padding: 0.7rem;
        border: none;
        border-radius: 6px;
        background: var(--primary-dark, #1a3a6b);
        color: white;
        font-weight: 600;
        cursor: pointer;
      }
      .error {
        color: #c0392b;
        font-size: 0.85rem;
        margin: 0;
      }
    `,
  ],
})
export class LoginComponent {
  username = '';
  password = '';
  error = signal<string | null>(null);

  constructor(private auth: AuthService, private router: Router) {}

  onSubmit(): void {
    if (this.auth.login(this.username, this.password)) {
      this.error.set(null);
      this.router.navigate(['/']);
    } else {
      this.error.set('Utilizador ou password inválidos');
    }
  }
}
