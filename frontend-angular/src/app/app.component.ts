import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { DashboardComponent } from './components/dashboard/dashboard.component';

/**
 * AppComponent - Root component of the application
 * Provides layout and navigation
 */
@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, DashboardComponent],
  template: `
    <div class="app-container">
      <!-- Header -->
      <header class="app-header">
        <div class="header-content">
          <h1><i class="material-icons">factory</i> Simulador Industrial</h1>
          <p class="subtitle">Sistema de gestão da linha de produção</p>
        </div>
        <div class="header-status">
          <span class="status-badge" [class.online]="isOnline()">
            <i class="material-icons">{{ isOnline() ? 'check_circle' : 'cancel' }}</i>
            {{ isOnline() ? 'Conectado' : 'Desconectado' }}
          </span>
        </div>
      </header>

      <!-- Main Content -->
      <main class="app-main">
        <app-dashboard></app-dashboard>
      </main>

      <!-- Footer -->
      <footer class="app-footer">
        <p>&copy; 2026 Simulador Industrial | Projeto de Sistemas Distribuídos</p>
      </footer>
    </div>
  `,
  styles: [
    `
      .app-container {
        display: flex;
        flex-direction: column;
        min-height: 100vh;
        background: #f8faff;
      }

      .app-header {
        background: var(--primary-dark);
        color: white;
        padding: 2rem;
        box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
        display: flex;
        justify-content: space-between;
        align-items: center;
      }

      .header-content h1 {
        margin: 0;
        font-size: 2.5rem;
        font-weight: 700;
      }

      .subtitle {
        margin: 0.5rem 0 0 0;
        font-size: 1rem;
        opacity: 0.9;
      }

      .header-status {
        display: flex;
        align-items: center;
        gap: 1rem;
      }

      .status-badge {
        padding: 0.5rem 1rem;
        border-radius: 20px;
        background: rgba(194, 231, 255, 0.2);
        font-weight: 600;
        display: flex;
        align-items: center;
        gap: 0.5rem;
      }

      .status-badge.online {
        background: rgba(194, 231, 255, 0.35);
      }

      .app-main {
        flex: 1;
        padding: 2rem;
        max-width: 1400px;
        width: 100%;
        margin: 0 auto;
      }

      .app-footer {
        background: var(--primary-dark);
        color: white;
        text-align: center;
        padding: 1rem;
        font-size: 0.9rem;
      }

      @media (max-width: 768px) {
        .app-header {
          flex-direction: column;
          text-align: center;
          gap: 1rem;
        }

        .app-main {
          padding: 1rem;
        }

        .header-content h1 {
          font-size: 1.8rem;
        }
      }
    `,
  ],
})
export class AppComponent implements OnInit {
  isOnline = signal(true);

  ngOnInit(): void {
    // Check backend connectivity periodically
    this.checkConnectivity();
    setInterval(() => this.checkConnectivity(), 5000);
  }

  private checkConnectivity(): void {
    // Simplified check - can be enhanced with actual health checks
    this.isOnline.set(true);
  }
}
