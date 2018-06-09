

export class Environment {
  public static readonly API = "/fifa2018/api"
//  public static readonly AUTHTOKEN = "AUTH-TOKEN"
  public static readonly XAUTHTOKEN = "X-AUTH-TOKEN"


  public static api(other: string): string {
      return `${this.API}/${other}`
  }

}


export interface ErrorMessage {
  type: string
  error: string
}


export interface OkMessage {
   ok: string
}
