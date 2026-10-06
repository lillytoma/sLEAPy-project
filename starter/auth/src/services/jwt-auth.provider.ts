import {
  AuthenticationRegistry,
  JwtBearerProvider,
  type JwtClaims,
} from '@nestjs/authentication';
import { Injectable } from '@nestjs/common';
import { User } from '../models/user.js';


@Injectable()
export class JwtAuth extends JwtBearerProvider<User>{
    constructor(registry: AuthenticationRegistry) {
    super({realm: 'store'});
    registry.registerProvider(this, { order: 1 });
  }

  protected validate(claims: JwtClaims): User | null {
    // TODO: lookup by claims.sub and return your user
    return null;
  }

}