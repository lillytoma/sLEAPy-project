-- Client_Status
CREATE TABLE Client_Status (
    ClientStatus_ID SERIAL PRIMARY KEY,
    ClientStatus_Name VARCHAR(100) NOT NULL
);
-- Order_Status
CREATE TABLE Order_Status (
    OrderStatus_ID SERIAL PRIMARY KEY,
    OrderStatus_Name VARCHAR(100) NOT NULL
);
-- Instrument_Type
CREATE TABLE Instrument_Type (
    InstrumentType_ID SERIAL PRIMARY KEY,
    InstrumentType VARCHAR(100) NOT NULL
);
-- Clients
CREATE TABLE Clients (
    Client_ID SERIAL PRIMARY KEY,
    ClientStatus_ID INTEGER NOT NULL REFERENCES Client_Status(ClientStatus_ID),
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    phone_number VARCHAR(50),
    "address" VARCHAR(255) NOT NULL,
    ssn VARCHAR(20),
    balance NUMERIC(15,2)
);
-- Instruments
CREATE TABLE Instruments (
    Instrument_ID SERIAL PRIMARY KEY,
    Symbol VARCHAR(20) NOT NULL,
    Symbol_Name VARCHAR(255) NOT NULL,
    InstrumentType_ID INTEGER NOT NULL REFERENCES Instrument_Type(InstrumentType_ID)
);
-- Holdings
CREATE TABLE Holdings (
    Holdings_ID SERIAL PRIMARY KEY,
    Client_ID INTEGER NOT NULL REFERENCES Clients(Client_ID),
    Instrument_ID INTEGER NOT NULL REFERENCES Instruments(Instrument_ID),
    Quantity_Shares NUMERIC(15,4),
    Purchase_Price NUMERIC(15,2)
);
-- Orders 
CREATE TABLE Orders (
    Order_ID SERIAL PRIMARY KEY,
    Client_ID INTEGER NOT NULL REFERENCES Clients(Client_ID),
    Instrument_ID INTEGER NOT NULL REFERENCES Instruments(Instrument_ID),
    Quantity NUMERIC(15,4),
    Time_of_purchase TIMESTAMP,
    Purchase_Price NUMERIC(15,2),
    Status INTEGER NOT NULL REFERENCES Order_Status(OrderStatus_ID)
);