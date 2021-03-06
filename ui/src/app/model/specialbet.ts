
export interface Template {
    id: number
    name: string
    description: string
    points: number
    closeDate: Date
    betGroup: string
    itemType: string
    result: string
    nr: number
}

export interface SpBet {
    id: number
    userId: number
    specialbetId: number
    prediction: string
    points: number
}


export interface SpecialBet {
    template: Template
    bet: SpBet
}

export interface SpecialBetPredictions {
    template: Template
    predictions: string[]
}
