import { Component, OnInit } from '@angular/core';
import { UserService } from '../../service/user.service';
import { catchError } from 'rxjs/operators';
import { of } from 'rxjs';
import { ToastComponent } from '../toast/toast.component';
import { MatSnackBar } from '@angular/material';
import { FilterService } from '../../service/filter.service';


@Component({
  selector: 'filter-view',
  templateUrl: './filter-view.component.html',
  styleUrls: ['./filter-view.component.css']
})
export class FilterViewComponent implements OnInit {

  constructor(private filterService: FilterService, private snackBar: MatSnackBar, private userService: UserService) { }


  filter = this.filterService.getFilter()

  gameFilter = ['all', 'open', 'closed']
  levelFilter = ['all', 'group', 'last16', 'quarter', 'semi', 'third', 'final']
  betFilter = ['all','set', 'not set']

  resetFilter(){
    this.filter = this.filterService.getDefaultFilter()
  }

  filterChanged = function(){
      if(this.userService.isLoggedIn){
       this.filterService.saveFilter(this.filter).pipe(
          catchError( (err) => {
             return of({ error: err.message})
          })
        ).subscribe( result => {
          if(result['error']){
            this.snackBar.openFromComponent(ToastComponent, { data: { message: `could not save filter\n ${result['error']}`, level: "error"}})
          }
      })
    }
    this.filterService.setFilter(this.filter)
  }

  ngOnInit() {
  }

}
