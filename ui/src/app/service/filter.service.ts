import { Injectable } from '@angular/core';
import { FilterSettings } from '../model/user';
import { HttpClient } from '@angular/common/http';
import { Environment } from '../model/environment';
import { GameBet, GameWithTeams } from '../model/bet';
import { BetService } from './bet.service';

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

  constructor(private http: HttpClient) { }

  fGame(gameBet: GameBet, filterGame, betService: BetService): boolean {
       switch(filterGame) {
            case "open": return ! betService.betClosed(gameBet.game.game.serverStart)
            case "closed":  return betService.betClosed(gameBet.game.game.serverStart)
            default: return true
       }
   }

  fGameD(gwt: GameWithTeams, filterGame, betService: BetService): boolean {
       switch(filterGame) {
            case "open": return ! betService.betClosed(gwt.game.serverStart)
            case "closed":  return betService.betClosed(gwt.game.serverStart)
            default: return true
        }
  }

  fBet(gameBet: GameBet, filterBet): boolean {
        const result = gameBet.bet.result
        if(result){
          switch(filterBet) {
              case "set": return result.isSet
              case "not set": return ! result.isSet
              default: return true
          }
        } else {
          return true
        }
  }

  fLevel(gameBet: GameBet, filterLevel): boolean {
        if(filterLevel == "all"){
            return true
        }
        return gameBet.game.level.name == filterLevel
  }

  fLevelD(gwt: GameWithTeams, filterLevel): boolean {
       if(filterLevel == "all"){
            return true
       }
      return gwt.level.name == filterLevel
  }

  filterGBL(gameBet: GameBet, betService: BetService): boolean {
      return this.fGame(gameBet, this.filter.game, betService) && this.fBet(gameBet, this.filter.bet) && this.fLevel(gameBet, this.filter.level)
  }

  filterGL(gwt: GameWithTeams, betService: BetService): boolean {
      return this.fGameD(gwt, this.filter.game, betService) && this.fLevelD(gwt, this.filter.level)
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
