import { Module } from '@nestjs/common';
import { AppController } from './app.controller.js';
import { ClientService } from './services/ClientService.js';
import { ClientValidator } from './services/ClientValidator.js';

@Module({
  imports: [],
  controllers: [AppController],
  providers: [ClientService, ClientValidator],
})
export class AppModule {}
