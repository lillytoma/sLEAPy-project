import { Module } from '@nestjs/common';
import { AppController } from './app.controller.js';
import { ClientService } from './services/client.service.js';
import { ClientValidator } from './services/client.validator.js';
import { PassportModule } from '@nestjs/passport';
import { JwtModule } from '@nestjs/jwt';
// import { TypeOrmModule } from '@nestjs/typeorm'
// import ClientEntity from './models/client.entity.js';

@Module({
  imports: [
    // TypeOrmModule.forRoot({
    //   type: 'postgres',
    //   host: 'localhost',
    //   port: 8101,
    //   username: 'postgres',
    //   password: 'sleapy_neueda',
    //   database: 'sleapy_db',
    //   entities: [ClientEntity],
    //   synchronize: true
    // }),
    PassportModule.register({ defaultStrategy: 'jwt'}),
    JwtModule.register({
      secret: 'hehehe_dont_tell_anybody',
      signOptions: {expiresIn: '1h'}
    })
  ],
  controllers: [AppController],
  providers: [ClientService, ClientValidator],
})
export class AppModule {}
