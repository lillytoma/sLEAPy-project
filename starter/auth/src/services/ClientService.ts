import { Injectable } from '@nestjs/common';
import { User } from '../models/user.js';

export interface SignUpRequestDTO {
	email: string;
	password: string;
	[key: string]: unknown;
}

export interface ClientEntity {
	id: number;
	email: string;
	password?: string;
}

@Injectable()
export class ClientService {
	async checkCredentials(_email: string, _password: string): Promise<boolean> {
		// TODO: Replace stub with DB-backed credential verification.
		return false;
	}

	async isUniqueEmail(_email: string): Promise<boolean> {
		// TODO: Replace stub with DB-backed uniqueness check.
		// Return true when email already exists to match Java controller behavior.
		return false;
	}

	async signup(request: SignUpRequestDTO): Promise<User> {
		// TODO: Replace stub with DB-backed client creation.
		
	}
}
