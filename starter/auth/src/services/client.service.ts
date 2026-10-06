import { Injectable } from '@nestjs/common';
import  ClientEntity  from '../models/client.entity.js';
import  SignUpRequestDTO  from '../models/signuprequest.dto.js';




@Injectable()
export class ClientService {
	async checkCredentials(_email: string, _password: string): Promise<boolean> {
		// TODO: Replace stub with DB-backed credential verification.
		return true;
	}

	async isUniqueEmail(_email: string): Promise<boolean> {
		// TODO: Replace stub with DB-backed uniqueness check.
		// Return true when email already exists to match Java controller behavior.
		return true;
	}

	public async findByEmail(_email: string): Promise<ClientEntity | null> {
	// TODO: Replace stub with database mapper lookup.
	return {
			id: 0,
			username: 'joemmama',
			email: 'joe@gmail.com',
			cashBalance: 67.420,
			password: 'password',
			phoneNumber: '6789998212',
			ssn: '123458765'
		};
	}

	async signup(request: SignUpRequestDTO): Promise<ClientEntity> {
		return {
			id: 0,
			username: 'joemmama',
			email: 'joe@gmail.com',
			cashBalance: 67.420,
			password: 'password',
			phoneNumber: '6789998212',
			ssn: '123458765'
		}
		
	}
}
