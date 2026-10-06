// import {Column, Entity, PrimaryGeneratedColumn } from 'typeorm';
// @Entity()
export default class ClientEntity {
    // @PrimaryGeneratedColumn()
    id: number;

    // @Column()
    username: string;

    // @Column()
    email: string;
    
    // @Column()
    cashBalance: number;
    
    // @Column()
    password: string;
    
    // @Column()
    phoneNumber: string;

    // @Column()
    ssn: string;
  
}