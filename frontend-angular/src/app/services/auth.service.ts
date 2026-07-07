import { Injectable, signal } from '@angular/core';

const SESSION_KEY = 'industry-simulator-auth';

// Credenciais do portal de configurações. Numa implementação de produção
// seriam validadas por um serviço de autenticação dedicado; aqui cumprem o
// requisito de "acesso autenticado ao portal" do enunciado.
const VALID_USERNAME = 'admin';
const VALID_PASSWORD = 'admin123';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private authenticated = signal<boolean>(sessionStorage.getItem(SESSION_KEY) === 'true');

  isAuthenticated() {
    return this.authenticated();
  }

  login(username: string, password: string): boolean {
    const ok = username === VALID_USERNAME && password === VALID_PASSWORD;
    if (ok) {
      sessionStorage.setItem(SESSION_KEY, 'true');
      this.authenticated.set(true);
    }
    return ok;
  }

  logout(): void {
    sessionStorage.removeItem(SESSION_KEY);
    this.authenticated.set(false);
  }
}
