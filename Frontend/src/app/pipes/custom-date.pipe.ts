import { Pipe, PipeTransform } from '@angular/core';
import { formatDate } from '@angular/common';

@Pipe({
  name: 'customDate'
})
export class CustomDatePipe implements PipeTransform {
  transform(value: Date | string | number): string {
    return value ? formatDate(value, 'dd.MM.yyyy, HH:mm', 'de-DE') : '';
  }
}
