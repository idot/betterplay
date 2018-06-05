import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'evenodd'
})
export class EvenoddPipe implements PipeTransform {

  transform(value: number): string {
     return value % 2 == 0 ? "even" : "odd"
  }

}
