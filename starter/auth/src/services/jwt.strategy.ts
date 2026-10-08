import { Injectable } from "@nestjs/common";
import { PassportStrategy } from "@nestjs/passport";
import { Strategy, ExtractJwt } from "passport-jwt";
import JwtPayload from "../models/jwtpayload.dto.js";

@Injectable()
export class JwtStrategy extends PassportStrategy(Strategy){
    validate(payload: JwtPayload): JwtPayload {
        console.log(payload);
        return payload;
    }
    /**
     *
     */
    constructor() {
        super({
            jwtFromRequest: ExtractJwt.fromAuthHeaderAsBearerToken(),
            ignoreExpiration: false,
            secretOrKey: 'hehehe_dont_tell_anybody'
        });
        
    }
}