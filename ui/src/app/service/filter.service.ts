import { Injectable } from '@angular/core';
import { FilterSettings } from '../model/user';
import { HttpClient } from '@angular/common/http';
import { Environment } from '../model/environment';
import { BetService } from './bet.service';
import { GameBet, GameWithTeams } from '../model/bet';



@Injectable({
  providedIn: 'root'
})
export class FilterService {

  private defaultFilter = {
    bet : "all",
    level : "all",
    game: "all"
  }

  private filter = this.defaultFilter

  constructor(private http: HttpClient,private betService: BetService) { }

  fGame(gameBet: GameBet, filterGame){
       switch(filterGame) {
            case "open": return ! this.betService.betClosed(gameBet.game.game.serverStart)
            case "closed":  return this.betService.betClosed(gameBet.game.game.serverStart)
            default: return true
       }
   }

  fGameD(gwt: GameWithTeams, filterGame){
       switch(filterGame) {
            case "open": return ! this.betService.betClosed(gwt.game.serverStart)
            case "closed":  return this.betService.betClosed(gwt.game.serverStart)
            default: return true
        }
  }

  fBet(gameBet: GameBet, filterBet){
        switch(filterBet) {
            case "set": return gameBet.bet.result.isSet
            case "not set": return ! gameBet.bet.result.isSet
            default: return true
        }
  }

  fLevel(gameBet: GameBet, filterLevel){
        if(filterLevel == "all"){
            return true
        }
        return gameBet.game.level.name == filterLevel
  }

  fLevelD(gwt: GameWithTeams, filterLevel){
       if(filterLevel == "all"){
            return true
       }
      return gwt.level.name == filterLevel
  }

  filterGBL(gameBet: GameBet){
      return this.fGame(gameBet, this.filter.game) && this.fBet(gameBet, this.filter.bet) && this.fLevel(gameBet, this.filter.level)
  }

  filterGL(gwt: GameWithTeams){
      return this.fGameD(gwt, this.filter.game) && this.fLevelD(gwt, this.filter.level)
  }


  getFilter(): FilterSettings {
     return this.filter
  }

  getDefaultFilter(): FilterSettings {
     return this.defaultFilter
  }

  saveFilter(filterSettings: FilterSettings){
     this.filter = filterSettings
     return this.http.post<any>(Environment.api("user/filter"), filterSettings)
  }

  setFilter(filterSettings: FilterSettings){
    this.filter = filterSettings
  }

}
