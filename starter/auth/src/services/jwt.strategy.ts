import { Injectable } from "@nestjs/common";
import { PassportStrategy } from "@nestjs/passport";
import { Strategy, ExtractJwt } from "passport-jwt";
@Injectable()
export class JwtStrategy extends PassportStrategy(Strategy){
    validate(...args: any[]): unknown {
        // throw new Error("Method not implemented.");
        return console.log("yomama" + args);
    }
    /**
     *
     */
    constructor() {
        super({
            jwtFromRequest: ExtractJwt.fromAuthHeaderAsBearerToken(),
            ignoreExpiration: false,
            secretOrKey: 'hehehe_dont_tell_anybody',
            passReqToCallback: true
        });
        
    }
}