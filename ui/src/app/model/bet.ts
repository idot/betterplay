import { User } from "./user"
import { SpecialBet } from "./specialbet"


    export interface Result {
        goalsTeam1: number
        goalsTeam2: number
        isSet: boolean
    }

    export interface Player {
        id: number
        name: string,
        role: string,
        club: string,
        teamid: string,
        dbimage: string
    }

    export interface Game {
        id: number
        result: Result
        team1id: number
        team2id: number
        levelId: number
        localStart: Date
        localtz: string
        serverStart: Date
        servertz: string
        venue: string
        group: string
        nr: number
        viewMinutesToGame: number
        closingMinutesToGame: number
        gameClosed: boolean
        nextGame: boolean
    }

    export interface Team {
        id: number
        name: string
        short3: string
        short2: string
    }


    export interface Level {
        id: number
        name: string
        pointsExact: number
        pointsTendency: number
        level: number
    }

    export interface GameWithTeams {
        game: Game
        team1: Team
        team2: Team
        level: Level
    }

    export interface Bet {
        id: number
        points: number
        result: Result
        gameId: number
        userId: number
        viewable: boolean
    }


    export interface GameBet {
        game: GameWithTeams
        bet: Bet
    }

    export interface UserWithBets {
        user: User
        specialBets: SpecialBet[]
        gameBets: GameBet[]
    }

    export interface PlayerWithTeam {
        player: Player
        team: Team
    }
