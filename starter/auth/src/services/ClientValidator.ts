import { Injectable } from '@nestjs/common';

export class InvalidEmailFormatException extends Error {
    constructor(message: string) {
        super(message);
        this.name = 'InvalidEmailFormatException';
    }
}

@Injectable()
export class ClientValidator {
    private readonly emailRegex =
        /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;

    validateEmail(email: string): void {
        if (!this.emailRegex.test(email)) {
            throw new InvalidEmailFormatException('Invalid email format');
        }
    }
}