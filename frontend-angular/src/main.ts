(window as any).global = window;
import 'zone.js';
import { LOCALE_ID } from '@angular/core';
import { bootstrapApplication } from '@angular/platform-browser';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { registerLocaleData } from '@angular/common';
import localePt from '@angular/common/locales/pt-PT';
import { AppComponent } from './app/app.component';
import { routes } from './app/app.routes';

// Português (Portugal/Angola): datas, números e moeda formatados em pt.
registerLocaleData(localePt, 'pt-PT');

bootstrapApplication(AppComponent, {
  providers: [
    provideHttpClient(),
    provideRouter(routes),
    { provide: LOCALE_ID, useValue: 'pt-PT' },
  ],
}).catch((err) => console.error(err));
