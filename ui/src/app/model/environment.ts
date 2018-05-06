

export class Environment {
  public static readonly API = "/api";
  public static readonly AUTHTOKEN = "AUTH-TOKEN";
  public static readonly XAUTHTOKEN = "X-AUTH-TOKEN";

  public static api(other: string): string {
      return `${this.API}/${other}`;
  }

};
