export default class SignUpResponseDTO {
  constructor(
    public readonly message: string,
    public readonly clientId: number,
    public readonly jwtToken: string,
  ) {}
}