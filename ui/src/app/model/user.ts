import { Environment } from './environment'
import { SpecialBet } from './specialbet'



export interface FilterSettings {
  bet: string
  game: string
  level: string
}

export interface User {
  id: number
  username: string
  email: string
  firstName: string
  lastName: string
  institute: string
  showName: boolean
  isAdmin: boolean
  isRegistrant: boolean
  hadInstructions: boolean
  sendEmail: boolean
  canBet: boolean
  totalPoints: number
  pointsGames: number
  pointsSpecialBet: number
  iconurl: string
  icontype: string
  rank: number
  filterSettings: FilterSettings
  viewable: boolean
}

export interface UserResponse {
  authtoken: string
  user: User
}


export interface UserWSpecialBets {
    user: User
    templateBets: SpecialBet[]
}
