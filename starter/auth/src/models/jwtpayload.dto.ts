import { UUID } from "node:crypto";
import ClientEntity from "./client.entity.js";
import crypto from 'node:crypto';


export default class JwtPayload{
  sub: string; 
  userId: number;
  iat: number; //in seconds
  jti: UUID;
}

export function generateJwtPayload(user: ClientEntity){
   const iat =  Math.floor(Date.now() / 1000); //seconds since Jan 01, 1970
      const payload: JwtPayload = {
        sub: user.email,
        userId: user.id,
        iat: iat,
        jti: crypto.randomUUID()
      }
    return payload;
}