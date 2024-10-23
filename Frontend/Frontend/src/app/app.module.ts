import {LOCALE_ID, NgModule} from '@angular/core';
import {BrowserModule} from '@angular/platform-browser';

import {AppComponent} from './app.component';
import {AppRoutingModule} from './app-routing-module';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {CarouselModule} from 'primeng/carousel';
import {ConfirmationService, MessageService} from 'primeng/api';
import {CustomDatePipe} from './pipes/custom-date.pipe';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {HomepageComponent} from './components/homepage/homepage.component';
import {PaginatorModule} from 'primeng/paginator';
import {provideHttpClient, withInterceptorsFromDi} from '@angular/common/http';
import {ToastModule} from 'primeng/toast';
import {TooltipModule} from 'primeng/tooltip';

@NgModule({
  declarations: [
    AppComponent,
    CustomDatePipe,
    HomepageComponent,
  ],
  imports: [
    AppRoutingModule,
    BrowserAnimationsModule,
    BrowserModule,
    CarouselModule,
    FormsModule,
    PaginatorModule,
    ReactiveFormsModule,
    ToastModule,
    TooltipModule
  ],
  providers: [
    provideHttpClient(withInterceptorsFromDi()),
    {provide: LOCALE_ID, useValue: 'de-DE'},
    MessageService,
    ConfirmationService
  ],
  bootstrap: [AppComponent]
})
export class AppModule {
}
