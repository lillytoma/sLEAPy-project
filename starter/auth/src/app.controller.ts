import { Body, Controller, Get, HttpStatus, Post, Res, UseGuards } from '@nestjs/common';
import type { Response } from 'express';
import { ClientService,  } from './services/client.service.js';
import SignUpRequestDTO from './models/signuprequest.dto.js'
import {
  ClientValidator,
  InvalidEmailFormatException,
} from './services/client.validator.js';
import { JwtService } from '@nestjs/jwt';
import { UUID } from 'crypto';
import  ClientEntity  from './models/client.entity.js';
import  LoginRequestDTO  from './models/loginrequest.dto.js';
import  SignUpResponseDTO  from './models/signupresponse.dto.js';
import { AuthGuard } from '@nestjs/passport';
import crypto from 'node:crypto';
class JwtPayload{
  sub: string; 
  userId: number;
  iat: number; //in seconds
  // exp: number; //in seconds
  jti: UUID;
}

function generateJwtPayload(user: ClientEntity){
   const iat = new Date().getSeconds();
      const exp: number = new Date(Date.now() + 8640000).getSeconds();
      const payload: JwtPayload = {
        sub: user.email,
        userId: user.id,
        iat: iat,
        // exp: exp,
        jti: crypto.randomUUID()
      }
    return payload;
}

@Controller('auth')
export class AppController {
  // The ClientService is injected into the AppController to handle authentication logic,
  // such as checking user credentials against the database.
  constructor(
    private readonly clientService: ClientService,
    private readonly clientValidator: ClientValidator,
    private readonly jwtService: JwtService,
  ) {}



 

  // The login method handles POST requests to the /login endpoint. It takes a
  // LoginRequestDTO object containing the user's email and password, checks the
  // credentials using the ClientService, and returns a response indicating whether
  // the login was successful or not.

  /**
   curl -iX POST http://localhost:8084/auth/login --header "Content-Type: application/json" -d "{
    \"email\" : \"test@example.com\",
    \"password\" : \"TestPassword123\"}"
  */ 
  @Post('/login')
  async login(@Body() request: LoginRequestDTO, @Res() res: Response): Promise<Response> {
    console.log(request);
    try {
      // Step 1: Validate email format
      this.clientValidator.validateEmail(request.email);

      //2. check if the credentials are valid
      const isValid = await this.clientService.checkCredentials(
        request.email,
        request.password,
      );

      //3. if the email / password are invalid throw a failed repsonse
      if (!isValid) {
        return res.status(HttpStatus.UNAUTHORIZED).json('Invalid email or password');
      }

      //4. if credentials are valid then reach out to the mapper to reach the db
      const user: ClientEntity | null = await this.clientService.findByEmail(request.email);
      if (!user) {
        throw new Error('User not found');
      }

      // Step 4: Generate JWT token
      // make sure the exp is set as a number or this will fail!!11!1!!1!!
      const payload = generateJwtPayload(user);
       const jwtToken = this.jwtService.sign(payload);

      // Step 5: Build response
      const response = new SignUpResponseDTO('Login successful', user.id, jwtToken);

      //6. return the 200 status OK
      return res.status(HttpStatus.OK).json(response);
    } catch (ex) {
      if (ex instanceof InvalidEmailFormatException) {
        return res.status(HttpStatus.BAD_REQUEST).json(ex.message);
      }

      console.error(ex);
      const errorMsg =
        ex instanceof Error && ex.message
          ? ex.message
          : (ex as { constructor?: { name?: string } }).constructor?.name ??
            'UnknownError';
      return res
        .status(HttpStatus.INTERNAL_SERVER_ERROR)
        .json(`Login failed: ${errorMsg}`);
    }
  }

  @Post('/signup')
  async signUp(@Body() request: SignUpRequestDTO, @Res() res: Response): Promise<Response> {
    try {
      // Step 1: Validate email format
      this.clientValidator.validateEmail(request.email);

      // Step 2: Check if email already exists
      if (await this.clientService.isUniqueEmail(request.email)) {
        return res.status(HttpStatus.CONFLICT).json('Email already registered');
      }

      // Step 3: Create new client in database
      const newClient = await this.clientService.signup(request);

       const payload = generateJwtPayload(newClient);

      // Step 4: Generate JWT token
       const jwtToken = this.jwtService.sign(payload);
     

      // Step 5: Build response
      const response = new SignUpResponseDTO(
        'Signup successful. Welcome!',
        newClient.id,
        jwtToken,
      );

      // Step 6: Return 201 CREATED (RESTful standard)
      return res.status(HttpStatus.CREATED).json(response);
    } catch (ex) {
      if (ex instanceof InvalidEmailFormatException) {
        return res.status(HttpStatus.BAD_REQUEST).json(ex.message);
      }

      console.error(ex);
      const errorMsg =
        ex instanceof Error && ex.message
          ? ex.message
          : (ex as { constructor?: { name?: string } }).constructor?.name ??
            'UnknownError';
      return res
        .status(HttpStatus.INTERNAL_SERVER_ERROR)
        .json(`Signup failed: ${errorMsg}`);
    }
  }

  // "gimme a valid refresh token, get a new access token"
  @Post('/refresh')
  async refresh(){}

  // the backend should reach out to this endpoint to make sure the tokens
  // provided are valid for the protected endpoints
  @Get('/validate')
  @UseGuards(AuthGuard('jwt'))
  async validate(){}

}
